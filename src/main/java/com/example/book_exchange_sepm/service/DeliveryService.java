package com.example.book_exchange_sepm.service;

import com.example.book_exchange_sepm.dto.DeliveryResponse;
import com.example.book_exchange_sepm.entity.Delivery;
import com.example.book_exchange_sepm.entity.ExchangeRequest;
import com.example.book_exchange_sepm.entity.Book;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.exception.ResourceNotFoundException;
import com.example.book_exchange_sepm.exception.UnauthorizedActionException;
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
    private final BookService bookService;

    public DeliveryService(DeliveryRepository deliveryRepository,
                           ExchangeRequestRepository exchangeRequestRepository,
                           UserService userService,
                           BookService bookService) {
        this.deliveryRepository = deliveryRepository;
        this.exchangeRequestRepository = exchangeRequestRepository;
        this.userService = userService;
        this.bookService = bookService;
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

        List<User> deliveryMen = userService.getApprovedDeliveryMenForAssignment();
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
    public List<DeliveryResponse> getAssignedDeliveriesForCurrentDeliveryMan() {
        User currentUser = userService.getCurrentUserEntity();
        return deliveryRepository.findByDeliveryMan_IdOrderByUpdatedAtDesc(currentUser.getId()).stream()
            .map(this::convertToResponse)
            .toList();
    }

    @Transactional
    public DeliveryResponse updateDeliveryStatus(Long deliveryId, Delivery.Status requestedStatus) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow(() -> new ResourceNotFoundException("Delivery not found with ID: " + deliveryId));

        User currentUser = userService.getCurrentUserEntity();
        if (delivery.getDeliveryMan() == null || !delivery.getDeliveryMan().getId().equals(currentUser.getId())) {
            throw new UnauthorizedActionException("You can only update deliveries assigned to you");
        }

        Delivery.Status currentStatus = delivery.getStatus();
        if (currentStatus == Delivery.Status.DELIVERED) {
            throw new UnauthorizedActionException("Delivered requests cannot be updated");
        }

        if (requestedStatus == Delivery.Status.PENDING_ASSIGNMENT || requestedStatus == Delivery.Status.ASSIGNED) {
            throw new UnauthorizedActionException("Only in-transit or delivered updates are allowed");
        }

        if (requestedStatus == Delivery.Status.IN_TRANSIT && currentStatus != Delivery.Status.ASSIGNED) {
            throw new UnauthorizedActionException("Only assigned deliveries can move to in transit");
        }

        if (requestedStatus == Delivery.Status.DELIVERED
            && currentStatus != Delivery.Status.ASSIGNED
            && currentStatus != Delivery.Status.IN_TRANSIT) {
            throw new UnauthorizedActionException("Only assigned or in-transit deliveries can be delivered");
        }

        delivery.setStatus(requestedStatus);

        if (requestedStatus == Delivery.Status.DELIVERED) {
            delivery.setDeliveredAt(LocalDateTime.now());
            finalizeExchangeCompletion(delivery);
        }

        return convertToResponse(deliveryRepository.save(delivery));
    }

    @Transactional(readOnly = true)
    public Delivery getDeliveryByExchangeRequestId(Long exchangeRequestId) {
        return deliveryRepository.findByExchangeRequest_Id(exchangeRequestId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Delivery not found for exchange request ID: " + exchangeRequestId
            ));
    }

    @Transactional(readOnly = true)
    public Delivery getDeliveryByExchangeRequestIdIfPresent(Long exchangeRequestId) {
        return deliveryRepository.findByExchangeRequest_Id(exchangeRequestId).orElse(null);
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

    private void finalizeExchangeCompletion(Delivery delivery) {
        ExchangeRequest exchangeRequest = delivery.getExchangeRequest();
        if (exchangeRequest.getStatus() == ExchangeRequest.Status.COMPLETED) {
            return;
        }

        Book requestedBook = exchangeRequest.getBook();
        Book offeredBook = exchangeRequest.getOfferedBook();
        User requester = exchangeRequest.getRequester();
        User originalOwner = resolveOwner(exchangeRequest);

        if (!requestedBook.getOwner().getId().equals(originalOwner.getId())) {
            throw new UnauthorizedActionException("Requested book owner changed. Refresh and retry flow.");
        }
        if (!offeredBook.getOwner().getId().equals(requester.getId())) {
            throw new UnauthorizedActionException("Offered book owner changed. Refresh and retry flow.");
        }

        requestedBook.setOwner(requester);
        offeredBook.setOwner(originalOwner);
        requestedBook.setAvailable(true);
        offeredBook.setAvailable(true);

        bookService.updateBook(requestedBook);
        bookService.updateBook(offeredBook);

        exchangeRequest.setStatus(ExchangeRequest.Status.COMPLETED);
        exchangeRequest.setCompletedAt(LocalDateTime.now());
        exchangeRequest.setModeratorComment("Delivery completed and ownership transferred");
        exchangeRequestRepository.save(exchangeRequest);
    }

    private User resolveOwner(ExchangeRequest exchangeRequest) {
        if (exchangeRequest.getOwner() != null) {
            return exchangeRequest.getOwner();
        }

        if (exchangeRequest.getBook() != null && exchangeRequest.getBook().getOwner() != null) {
            return exchangeRequest.getBook().getOwner();
        }

        throw new UnauthorizedActionException("Exchange request owner is missing. Please recreate request.");
    }

    private DeliveryResponse convertToResponse(Delivery delivery) {
        ExchangeRequest exchangeRequest = delivery.getExchangeRequest();
        return new DeliveryResponse(
            delivery.getId(),
            exchangeRequest.getId(),
            exchangeRequest.getBook().getTitle(),
            exchangeRequest.getOfferedBook() != null ? exchangeRequest.getOfferedBook().getTitle() : null,
            exchangeRequest.getRequester().getUsername(),
            resolveOwner(exchangeRequest).getUsername(),
            delivery.getDeliveryMan() != null ? delivery.getDeliveryMan().getUsername() : null,
            delivery.getStatus().name(),
            delivery.getNotes(),
            delivery.getAssignedAt(),
            delivery.getDeliveredAt(),
            delivery.getUpdatedAt()
        );
    }
}
