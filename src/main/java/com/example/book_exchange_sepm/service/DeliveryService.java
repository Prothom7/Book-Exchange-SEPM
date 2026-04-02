package com.example.book_exchange_sepm.service;

import com.example.book_exchange_sepm.entity.Delivery;
import com.example.book_exchange_sepm.entity.ExchangeRequest;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.exception.ResourceNotFoundException;
import com.example.book_exchange_sepm.repository.DeliveryRepository;
import com.example.book_exchange_sepm.repository.ExchangeRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class DeliveryService {

    private static final Set<Delivery.Status> ACTIVE_DELIVERY_STATUSES = Set.of(
        Delivery.Status.ASSIGNED,
        Delivery.Status.IN_TRANSIT
    );

    private final DeliveryRepository deliveryRepository;
    private final ExchangeRequestRepository exchangeRequestRepository;
    private final UserService userService;

    public DeliveryService(DeliveryRepository deliveryRepository,
                           ExchangeRequestRepository exchangeRequestRepository,
                           UserService userService) {
        this.deliveryRepository = deliveryRepository;
        this.exchangeRequestRepository = exchangeRequestRepository;
        this.userService = userService;
    }

    @Transactional
    public Delivery createPendingDelivery(Long exchangeRequestId) {
        ExchangeRequest exchangeRequest = exchangeRequestRepository.findById(exchangeRequestId)
            .orElseThrow(() -> new ResourceNotFoundException("Exchange request not found with ID: " + exchangeRequestId));

        return deliveryRepository.findByExchangeRequest_Id(exchangeRequestId)
            .orElseGet(() -> deliveryRepository.save(buildPendingDelivery(exchangeRequest)));
    }

    @Transactional
    public Delivery autoAssignDeliveryMan(Long exchangeRequestId) {
        Delivery delivery = createPendingDelivery(exchangeRequestId);

        if (delivery.getDeliveryMan() != null) {
            return delivery;
        }

        List<User> deliveryMen = userService.getDeliveryMen();
        if (deliveryMen.isEmpty()) {
            return delivery;
        }

        User selectedDeliveryMan = deliveryMen.stream()
            .min(Comparator.comparingLong(this::getActiveDeliveryCount))
            .orElse(deliveryMen.get(0));

        delivery.setDeliveryMan(selectedDeliveryMan);
        delivery.setStatus(Delivery.Status.ASSIGNED);
        delivery.setAssignedAt(LocalDateTime.now());
        return deliveryRepository.save(delivery);
    }

    @Transactional(readOnly = true)
    public Delivery getDeliveryByExchangeRequestId(Long exchangeRequestId) {
        return deliveryRepository.findByExchangeRequest_Id(exchangeRequestId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Delivery not found for exchange request ID: " + exchangeRequestId
            ));
    }

    @Transactional(readOnly = true)
    public List<Delivery> getAssignedDeliveriesForCurrentDeliveryMan() {
        User currentUser = userService.getCurrentUserEntity();
        return deliveryRepository.findByDeliveryMan_IdOrderByUpdatedAtDesc(currentUser.getId());
    }

    private long getActiveDeliveryCount(User deliveryMan) {
        return deliveryRepository.countByDeliveryMan_IdAndStatusIn(deliveryMan.getId(), ACTIVE_DELIVERY_STATUSES);
    }

    private Delivery buildPendingDelivery(ExchangeRequest exchangeRequest) {
        Delivery delivery = new Delivery();
        delivery.setExchangeRequest(exchangeRequest);
        delivery.setStatus(Delivery.Status.PENDING_ASSIGNMENT);
        return delivery;
    }
}
