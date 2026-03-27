package com.example.book_exchange_sepm.service;

import com.example.book_exchange_sepm.dto.ExchangeRequestRequest;
import com.example.book_exchange_sepm.dto.ExchangeRequestResponse;
import com.example.book_exchange_sepm.dto.ExchangeStatusUpdateRequest;
import com.example.book_exchange_sepm.entity.Book;
import com.example.book_exchange_sepm.entity.ExchangeRequest;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.exception.ResourceNotFoundException;
import com.example.book_exchange_sepm.exception.UnauthorizedActionException;
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

    /**
     * Create exchange request (USER+ roles)
     * User requests to exchange a book from another user
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

        ExchangeRequest exchangeRequest = new ExchangeRequest();
        exchangeRequest.setRequester(requester);
        exchangeRequest.setBook(requestedBook);
        exchangeRequest.setOfferedBook(offeredBook);
        exchangeRequest.setMessage(request.getMessage());
        exchangeRequest.setStatus(ExchangeRequest.Status.PENDING);

        ExchangeRequest savedRequest = exchangeRequestRepository.save(exchangeRequest);
        return convertToResponse(savedRequest);
    }

    /**
     * Get all exchange requests for current user's books
     * Only book owner can see requests for their own books
     */
    @Transactional(readOnly = true)
    public List<ExchangeRequestResponse> getMyBookExchangeRequests() {
        Long currentUserId = userService.getCurrentUserId();
        return exchangeRequestRepository.findByBookOwner_Id(currentUserId).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get all exchange requests made by current user
     */
    @Transactional(readOnly = true)
    public List<ExchangeRequestResponse> getMyExchangeRequests() {
        Long currentUserId = userService.getCurrentUserId();
        return exchangeRequestRepository.findByRequester_Id(currentUserId).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get pending requests for moderator review.
     */
    @Transactional(readOnly = true)
    public List<ExchangeRequestResponse> getPendingRequestsForModeration() {
        validateModeratorOrAdmin();

        return exchangeRequestRepository.findByStatusOrderByCreatedAtDesc(ExchangeRequest.Status.PENDING).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Approve exchange request
     * OWNERSHIP ENFORCED: Only book owner can approve requests for their book
     */
    @Transactional
    public ExchangeRequestResponse approveExchangeRequest(Long exchangeRequestId) {
        ExchangeRequest exchangeRequest = findExchangeRequestById(exchangeRequestId);
        validateModeratorOrAdmin();

        if (exchangeRequest.getStatus() != ExchangeRequest.Status.PENDING) {
            throw new UnauthorizedActionException("Only pending requests can be approved");
        }

        User reviewer = userService.getCurrentUserEntity();

        exchangeRequest.setStatus(ExchangeRequest.Status.APPROVED);
        exchangeRequest.setReviewedBy(reviewer);
        exchangeRequest.setReviewedAt(java.time.LocalDateTime.now());
        exchangeRequest.setModeratorComment("Approved by moderator");

        exchangeRequest.getBook().setAvailable(false);
        exchangeRequest.getOfferedBook().setAvailable(false);

        ExchangeRequest updatedRequest = exchangeRequestRepository.save(exchangeRequest);
        return convertToResponse(updatedRequest);
    }

    /**
     * Reject exchange request
     * OWNERSHIP ENFORCED: Only book owner can reject requests for their book
     */
    @Transactional
    public ExchangeRequestResponse rejectExchangeRequest(Long exchangeRequestId) {
        ExchangeRequest exchangeRequest = findExchangeRequestById(exchangeRequestId);
        validateModeratorOrAdmin();

        if (exchangeRequest.getStatus() != ExchangeRequest.Status.PENDING) {
            throw new UnauthorizedActionException("Only pending requests can be rejected");
        }

        User reviewer = userService.getCurrentUserEntity();

        exchangeRequest.setStatus(ExchangeRequest.Status.REJECTED);
        exchangeRequest.setReviewedBy(reviewer);
        exchangeRequest.setReviewedAt(java.time.LocalDateTime.now());
        exchangeRequest.setModeratorComment("Rejected by moderator");

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
        ExchangeRequest exchangeRequest = findExchangeRequestById(exchangeRequestId);
        validateModeratorOrAdmin();

        String status = request.getStatus().toUpperCase();

        if ("APPROVED".equals(status)) {
            if (exchangeRequest.getStatus() != ExchangeRequest.Status.PENDING) {
                throw new UnauthorizedActionException("Only pending requests can be approved");
            }
            User reviewer = userService.getCurrentUserEntity();
            exchangeRequest.setStatus(ExchangeRequest.Status.APPROVED);
            exchangeRequest.setReviewedBy(reviewer);
            exchangeRequest.setReviewedAt(java.time.LocalDateTime.now());
            exchangeRequest.setModeratorComment(request.getModeratorComment() != null ? request.getModeratorComment() : "Approved by moderator");
            exchangeRequest.getBook().setAvailable(false);
            exchangeRequest.getOfferedBook().setAvailable(false);
        } else if ("REJECTED".equals(status)) {
            if (exchangeRequest.getStatus() != ExchangeRequest.Status.PENDING) {
                throw new UnauthorizedActionException("Only pending requests can be rejected");
            }
            User reviewer = userService.getCurrentUserEntity();
            exchangeRequest.setStatus(ExchangeRequest.Status.REJECTED);
            exchangeRequest.setReviewedBy(reviewer);
            exchangeRequest.setReviewedAt(java.time.LocalDateTime.now());
            exchangeRequest.setModeratorComment(request.getModeratorComment() != null ? request.getModeratorComment() : "Rejected by moderator");
        } else {
            throw new UnauthorizedActionException("Invalid status. Must be APPROVED or REJECTED");
        }

        ExchangeRequest updatedRequest = exchangeRequestRepository.save(exchangeRequest);
        return convertToResponse(updatedRequest);
    }

    /**
     * Get exchange request by ID
     */
    @Transactional(readOnly = true)
    public ExchangeRequestResponse getExchangeRequestById(Long exchangeRequestId) {
        ExchangeRequest exchangeRequest = findExchangeRequestById(exchangeRequestId);
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
        return new ExchangeRequestResponse(
            exchangeRequest.getId(),
            exchangeRequest.getRequester().getId(),
            exchangeRequest.getRequester().getUsername(),
            exchangeRequest.getBook().getId(),
            exchangeRequest.getBook().getTitle(),
            exchangeRequest.getBook().getOwner().getId(),
            exchangeRequest.getBook().getOwner().getUsername(),
            exchangeRequest.getOfferedBook() != null ? exchangeRequest.getOfferedBook().getId() : null,
            exchangeRequest.getOfferedBook() != null ? exchangeRequest.getOfferedBook().getTitle() : null,
            exchangeRequest.getStatus().toString(),
            exchangeRequest.getMessage(),
            exchangeRequest.getModeratorComment(),
            exchangeRequest.getReviewedBy() != null ? exchangeRequest.getReviewedBy().getId() : null,
            exchangeRequest.getReviewedBy() != null ? exchangeRequest.getReviewedBy().getUsername() : null,
            exchangeRequest.getReviewedAt(),
            exchangeRequest.getCreatedAt(),
            exchangeRequest.getUpdatedAt()
        );
    }

    private void validateModeratorOrAdmin() {
        if (!userService.isModerator() && !userService.isAdmin()) {
            throw new UnauthorizedActionException("Only moderators or admins can review exchange requests");
        }
    }
}
