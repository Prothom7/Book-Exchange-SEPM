package com.example.book_exchange_sepm.service;

import com.example.book_exchange_sepm.dto.ExchangeRequestRequest;
import com.example.book_exchange_sepm.dto.ExchangeRequestResponse;
import com.example.book_exchange_sepm.dto.ExchangeStatusUpdateRequest;
import com.example.book_exchange_sepm.entity.Book;
import com.example.book_exchange_sepm.entity.Delivery;
import com.example.book_exchange_sepm.entity.ExchangeRequest;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.exception.ResourceNotFoundException;
import com.example.book_exchange_sepm.exception.UnauthorizedActionException;
import com.example.book_exchange_sepm.repository.DeliveryRepository;
import com.example.book_exchange_sepm.repository.ExchangeRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExchangeRequestService {

    @Autowired
    private ExchangeRequestRepository exchangeRequestRepository;

    @Autowired
    private BookService bookService;

    @Autowired
    private UserService userService;

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private DeliveryRepository deliveryRepository;

    /**
     * Create exchange request (USER+ roles)
     * User requests to exchange a book from another user
     * Automatically creates a chat room for this exchange
     */
    @Transactional
    public ExchangeRequestResponse createExchangeRequest(ExchangeRequestRequest request) {
        User requester = userService.getCurrentUserEntity();
        Book requestedBook = bookService.findBookById(request.getBookId());
        Book offeredBook = bookService.findBookById(request.getOfferedBookId());

        // Cannot request your own book
        if (requestedBook.getOwner().getId().equals(requester.getId())) {
            throw new UnauthorizedActionException("You cannot request your own book");
        }

        if (!Boolean.TRUE.equals(requestedBook.getAvailable())) {
            throw new UnauthorizedActionException("Requested book is not currently available for exchange");
        }

        if (!offeredBook.getOwner().getId().equals(requester.getId())) {
            throw new UnauthorizedActionException("You can only offer a book from your own library");
        }

        if (!Boolean.TRUE.equals(offeredBook.getAvailable())) {
            throw new UnauthorizedActionException("Offered book must be marked available");
        }

        if (requestedBook.getId().equals(offeredBook.getId())) {
            throw new UnauthorizedActionException("Requested and offered book cannot be the same");
        }

        if (exchangeRequestRepository.existsByRequester_IdAndBook_IdAndOfferedBook_IdAndStatus(
                requester.getId(),
                requestedBook.getId(),
                offeredBook.getId(),
                ExchangeRequest.Status.PENDING)) {
            throw new UnauthorizedActionException("You already have a pending request with this offered book");
        }

        if (exchangeRequestRepository.existsByRequester_IdAndBook_IdAndStatus(
                requester.getId(),
                requestedBook.getId(),
                ExchangeRequest.Status.PENDING)) {
            throw new UnauthorizedActionException("You already have a pending request for this book");
        }

        ExchangeRequest exchangeRequest = new ExchangeRequest();
        exchangeRequest.setRequester(requester);
        exchangeRequest.setOwner(requestedBook.getOwner());
        exchangeRequest.setBook(requestedBook);
        exchangeRequest.setOfferedBook(offeredBook);
        exchangeRequest.setMessage(request.getMessage());
        exchangeRequest.setStatus(ExchangeRequest.Status.PENDING);
        exchangeRequest.setRequesterAcceptedAt(java.time.LocalDateTime.now());

        ExchangeRequest savedRequest = exchangeRequestRepository.save(exchangeRequest);
        
        // Auto-create chat room for this exchange
        if (chatRoomService != null) {
            chatRoomService.createChatRoomForExchange(savedRequest);
        }
        
        return convertToResponse(savedRequest);
    }

    /**
     * Get all exchange requests for current user's books
     * Only book owner can see requests for their own books
     */
    @Transactional(readOnly = true)
    public List<ExchangeRequestResponse> getMyBookExchangeRequests() {
        Long currentUserId = userService.getCurrentUserId();
        return exchangeRequestRepository.findByOwner_IdOrderByCreatedAtDesc(currentUserId).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get all exchange requests made by current user
     */
    @Transactional(readOnly = true)
    public List<ExchangeRequestResponse> getMyExchangeRequests() {
        Long currentUserId = userService.getCurrentUserId();
        return exchangeRequestRepository.findByRequester_IdOrderByCreatedAtDesc(currentUserId).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get pending requests for moderator review.
     */
    @Transactional(readOnly = true)
    public List<ExchangeRequestResponse> getPendingRequestsForModeration() {
        validateModeratorOrAdmin();

        return exchangeRequestRepository
            .findByStatusAndRequesterAcceptedAtNotNullAndOwnerAcceptedAtNotNullOrderByCreatedAtDesc(ExchangeRequest.Status.PENDING)
            .stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Participant acceptance stage.
     * Only requester or owner can accept a pending exchange.
     */
    @Transactional
    public ExchangeRequestResponse acceptExchangeRequest(Long exchangeRequestId) {
        ExchangeRequest exchangeRequest = findExchangeRequestById(exchangeRequestId);

        if (exchangeRequest.getStatus() != ExchangeRequest.Status.PENDING) {
            throw new UnauthorizedActionException("Only pending requests can be accepted by participants");
        }

        User currentUser = userService.getCurrentUserEntity();
        Long currentUserId = currentUser.getId();

        if (exchangeRequest.getRequester().getId().equals(currentUserId)) {
            if (exchangeRequest.getRequesterAcceptedAt() == null) {
                exchangeRequest.setRequesterAcceptedAt(java.time.LocalDateTime.now());
            }
        } else if (resolveOwner(exchangeRequest).getId().equals(currentUserId)) {
            if (exchangeRequest.getOwnerAcceptedAt() == null) {
                exchangeRequest.setOwnerAcceptedAt(java.time.LocalDateTime.now());
            }
        } else {
            throw new UnauthorizedActionException("Only exchange participants can accept this exchange");
        }

        ExchangeRequest updated = exchangeRequestRepository.save(exchangeRequest);
        return convertToResponse(updated);
    }

    /**
     * Approve exchange request and hand it over to delivery.
     * Flow: PENDING -> APPROVED, then a delivery record is created and auto-assigned.
     */
    @Transactional
    public ExchangeRequestResponse approveExchangeRequest(Long exchangeRequestId) {
        ExchangeRequest exchangeRequest = findExchangeRequestById(exchangeRequestId);
        validateModeratorOnly();
        validatePendingOwnershipMapping(exchangeRequest);

        if (exchangeRequest.getStatus() != ExchangeRequest.Status.PENDING) {
            throw new UnauthorizedActionException("Only pending requests can be approved");
        }

        if (exchangeRequest.getRequesterAcceptedAt() == null || exchangeRequest.getOwnerAcceptedAt() == null) {
            throw new UnauthorizedActionException("Both participants must accept before moderator approval");
        }

        User reviewer = userService.getCurrentUserEntity();

        // Moderator approval moves the books into delivery.
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

        requestedBook.setAvailable(false);
        offeredBook.setAvailable(false);

        bookService.updateBook(requestedBook);
        bookService.updateBook(offeredBook);

        exchangeRequest.setStatus(ExchangeRequest.Status.APPROVED);
        exchangeRequest.setReviewedBy(reviewer);
        exchangeRequest.setReviewedAt(java.time.LocalDateTime.now());
        exchangeRequest.setCompletedAt(null);
        exchangeRequest.setModeratorComment("Approved by moderator and sent for delivery");

        cancelConflictingPendingRequests(exchangeRequest);
        chatRoomService.getChatRoomForExchange(exchangeRequestId);

        ExchangeRequest updatedRequest = exchangeRequestRepository.save(exchangeRequest);
        deliveryService.autoAssignDeliveryMan(exchangeRequestId);
        return convertToResponse(updatedRequest);
    }

    /**
     * Reject exchange request
     * OWNERSHIP ENFORCED: Only book owner can reject requests for their book
     */
    @Transactional
    public ExchangeRequestResponse rejectExchangeRequest(Long exchangeRequestId) {
        ExchangeRequest exchangeRequest = findExchangeRequestById(exchangeRequestId);
        validateModeratorOnly();

        if (exchangeRequest.getStatus() != ExchangeRequest.Status.PENDING) {
            throw new UnauthorizedActionException("Only pending requests can be rejected");
        }

        User reviewer = userService.getCurrentUserEntity();

        exchangeRequest.setStatus(ExchangeRequest.Status.REJECTED);
        exchangeRequest.setReviewedBy(reviewer);
        exchangeRequest.setReviewedAt(java.time.LocalDateTime.now());
        exchangeRequest.setModeratorComment("Rejected by moderator");

        chatRoomService.getChatRoomForExchange(exchangeRequestId);

        ExchangeRequest updatedRequest = exchangeRequestRepository.save(exchangeRequest);
        return convertToResponse(updatedRequest);
    }

    /**
     * Cancel own exchange request
     */
    @Transactional
    public ExchangeRequestResponse cancelExchangeRequest(Long exchangeRequestId) {
        ExchangeRequest exchangeRequest = findExchangeRequestById(exchangeRequestId);
        Long currentUserId = userService.getCurrentUserId();

        // Only requester can cancel
        if (!exchangeRequest.getRequester().getId().equals(currentUserId)) {
            throw new UnauthorizedActionException("You can only cancel your own requests");
        }

        if (exchangeRequest.getStatus() != ExchangeRequest.Status.PENDING) {
            throw new UnauthorizedActionException("Only pending requests can be cancelled");
        }

        exchangeRequest.setStatus(ExchangeRequest.Status.CANCELLED);
        ExchangeRequest updatedRequest = exchangeRequestRepository.save(exchangeRequest);
        return convertToResponse(updatedRequest);
    }

    /**
     * Update exchange request status (moderator endpoint)
     * Routes to approve/reject based on status in request
     */
    @Transactional
    public ExchangeRequestResponse updateRequestStatus(Long exchangeRequestId, ExchangeStatusUpdateRequest request) {
        String status = request.getStatus().toUpperCase();

        if ("APPROVED".equals(status)) {
            return approveExchangeRequest(exchangeRequestId);
        }

        if ("REJECTED".equals(status)) {
            return rejectExchangeRequest(exchangeRequestId);
        }

        throw new UnauthorizedActionException("Invalid status. Must be APPROVED or REJECTED");
    }

    /**
     * CRITICAL: Complete an exchange and transfer ownership
     * Manual completion is disabled because the transfer now happens through delivery.
     */
    @Transactional
    public ExchangeRequestResponse completeExchangeRequest(Long exchangeRequestId) {
        throw new UnauthorizedActionException("Manual completion is disabled. Ownership transfer happens only after moderator approval.");
    }

    /**
     * Get exchange request by ID
     */
    @Transactional(readOnly = true)
    public ExchangeRequestResponse getExchangeRequestById(Long exchangeRequestId) {
        ExchangeRequest exchangeRequest;
        if (userService.isAdmin() || userService.isModerator()) {
            exchangeRequest = findExchangeRequestById(exchangeRequestId);
        } else {
            Long currentUserId = userService.getCurrentUserId();
            exchangeRequest = exchangeRequestRepository.findVisibleByIdForUser(exchangeRequestId, currentUserId)
                .orElseThrow(() -> new UnauthorizedActionException("You are not allowed to view this exchange request"));
        }
        return convertToResponse(exchangeRequest);
    }

    /**
     * Find exchange request by ID or throw exception
     */
    @Transactional(readOnly = true)
    protected ExchangeRequest findExchangeRequestById(Long exchangeRequestId) {
        return exchangeRequestRepository.findById(exchangeRequestId)
            .orElseThrow(() -> new ResourceNotFoundException("Exchange request not found with id: " + exchangeRequestId));
    }

    /**
     * Convert ExchangeRequest entity to ExchangeRequestResponse DTO
     */
    private ExchangeRequestResponse convertToResponse(ExchangeRequest exchangeRequest) {
        Delivery delivery = deliveryRepository.findByExchangeRequest_Id(exchangeRequest.getId()).orElse(null);
        return new ExchangeRequestResponse(
            exchangeRequest.getId(),
            exchangeRequest.getRequester().getId(),
            exchangeRequest.getRequester().getUsername(),
            exchangeRequest.getBook().getId(),
            exchangeRequest.getBook().getTitle(),
            resolveBookImageUrl(exchangeRequest.getBook().getImageUrl(), exchangeRequest.getBook().getIsbn(), exchangeRequest.getBook().getTitle()),
            resolveOwner(exchangeRequest).getId(),
            resolveOwner(exchangeRequest).getUsername(),
            exchangeRequest.getOfferedBook() != null ? exchangeRequest.getOfferedBook().getId() : null,
            exchangeRequest.getOfferedBook() != null ? exchangeRequest.getOfferedBook().getTitle() : null,
            exchangeRequest.getOfferedBook() != null
                ? resolveBookImageUrl(exchangeRequest.getOfferedBook().getImageUrl(), exchangeRequest.getOfferedBook().getIsbn(), exchangeRequest.getOfferedBook().getTitle())
                : null,
            exchangeRequest.getStatus().toString(),
            exchangeRequest.getMessage(),
            exchangeRequest.getModeratorComment(),
            exchangeRequest.getReviewedBy() != null ? exchangeRequest.getReviewedBy().getId() : null,
            exchangeRequest.getReviewedBy() != null ? exchangeRequest.getReviewedBy().getUsername() : null,
            exchangeRequest.getReviewedAt(),
            exchangeRequest.getCreatedAt(),
            exchangeRequest.getUpdatedAt(),
            exchangeRequest.getCompletedAt(),
            exchangeRequest.getRequesterAcceptedAt(),
            exchangeRequest.getOwnerAcceptedAt(),
            delivery != null ? delivery.getId() : null,
            delivery != null ? delivery.getStatus().name() : null,
            delivery != null && delivery.getDeliveryMan() != null ? delivery.getDeliveryMan().getUsername() : null
        );
    }

    private String resolveBookImageUrl(String imageUrl, String isbn, String title) {
        if (imageUrl != null && !imageUrl.isBlank()) {
            return imageUrl;
        }

        if (isbn != null && !isbn.isBlank()) {
            return "https://covers.openlibrary.org/b/isbn/" + isbn.trim() + "-M.jpg?default=false";
        }

        String safeTitle = (title == null || title.isBlank()) ? "Book" : title.trim().replace(" ", "+");
        return "https://placehold.co/260x380/eef2ff/334155?text=" + safeTitle;
    }

    private void validateModeratorOrAdmin() {
        if (!userService.isModerator() && !userService.isAdmin()) {
            throw new UnauthorizedActionException("Only moderators or admins can review exchange requests");
        }
    }

    private void validateModeratorOnly() {
        if (!userService.isModerator()) {
            throw new UnauthorizedActionException("Only moderators can approve or reject final exchanges");
        }
    }

    private void validateCanReviewRequest(ExchangeRequest exchangeRequest) {
        User currentUser = userService.getCurrentUserEntity();
        Long currentUserId = currentUser.getId();
        Long ownerId = resolveOwner(exchangeRequest).getId();

        boolean moderatorOrAdmin = userService.isModerator() || userService.isAdmin();
        if (moderatorOrAdmin || ownerId.equals(currentUserId)) {
            return;
        }

        throw new UnauthorizedActionException("Only the book owner, moderator, or admin can review this request");
    }

    private void validatePendingOwnershipMapping(ExchangeRequest exchangeRequest) {
        User owner = resolveOwner(exchangeRequest);

        if (!exchangeRequest.getBook().getOwner().getId().equals(owner.getId())) {
            throw new UnauthorizedActionException("Requested book owner no longer matches exchange owner.");
        }

        if (!exchangeRequest.getOfferedBook().getOwner().getId().equals(exchangeRequest.getRequester().getId())) {
            throw new UnauthorizedActionException("Offered book is no longer owned by requester.");
        }
    }

    private void cancelConflictingPendingRequests(ExchangeRequest approvedExchange) {
        List<ExchangeRequest> conflicts = exchangeRequestRepository.findConflictingPendingRequests(
            approvedExchange.getId(),
            approvedExchange.getBook().getId(),
            approvedExchange.getOfferedBook().getId()
        );

        for (ExchangeRequest conflict : conflicts) {
            conflict.setStatus(ExchangeRequest.Status.CANCELLED);
            conflict.setModeratorComment("Auto-cancelled because one of the books is already in an approved exchange.");
            exchangeRequestRepository.save(conflict);
        }
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
}
