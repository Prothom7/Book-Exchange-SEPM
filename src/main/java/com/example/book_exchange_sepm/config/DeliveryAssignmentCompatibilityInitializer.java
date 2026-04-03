package com.example.book_exchange_sepm.config;

import com.example.book_exchange_sepm.entity.Delivery;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.repository.DeliveryRepository;
import com.example.book_exchange_sepm.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Component
@Order(210)
public class DeliveryAssignmentCompatibilityInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DeliveryAssignmentCompatibilityInitializer.class);

    private static final Set<Delivery.Status> ACTIVE_DELIVERY_STATUSES = Set.of(
        Delivery.Status.ASSIGNED,
        Delivery.Status.IN_TRANSIT
    );

    private final DeliveryRepository deliveryRepository;
    private final UserRepository userRepository;

    public DeliveryAssignmentCompatibilityInitializer(DeliveryRepository deliveryRepository,
                                                      UserRepository userRepository) {
        this.deliveryRepository = deliveryRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<User> approvedDeliveryMen = userRepository.findByRoles_Name("ROLE_DELIVERY_MAN").stream()
            .filter(user -> user.getDeliveryRequestStatus() == User.DeliveryRequestStatus.APPROVED)
            .toList();

        if (approvedDeliveryMen.isEmpty()) {
            log.info("No approved delivery users found for compatibility reassignment");
            return;
        }

        List<Delivery> deliveriesNeedingReassignment = deliveryRepository.findAll().stream()
            .filter(this::needsApprovedDriver)
            .toList();

        int reassigned = 0;
        for (Delivery delivery : deliveriesNeedingReassignment) {
            User selectedDriver = approvedDeliveryMen.stream()
                .min(Comparator.comparingLong(this::getActiveDeliveryCount))
                .orElse(null);

            if (selectedDriver == null) {
                continue;
            }

            delivery.setDeliveryMan(selectedDriver);
            if (delivery.getStatus() == Delivery.Status.PENDING_ASSIGNMENT) {
                delivery.setStatus(Delivery.Status.ASSIGNED);
            }
            if (delivery.getAssignedAt() == null) {
                delivery.setAssignedAt(LocalDateTime.now());
            }
            deliveryRepository.save(delivery);
            reassigned++;
        }

        log.info("Reassigned {} deliveries to approved delivery users", reassigned);
    }

    private boolean needsApprovedDriver(Delivery delivery) {
        User driver = delivery.getDeliveryMan();
        if (driver == null) {
            return false;
        }

        return driver.getDeliveryRequestStatus() != User.DeliveryRequestStatus.APPROVED;
    }

    private long getActiveDeliveryCount(User deliveryMan) {
        return deliveryRepository.countByDeliveryMan_IdAndStatusIn(deliveryMan.getId(), ACTIVE_DELIVERY_STATUSES);
    }
}
