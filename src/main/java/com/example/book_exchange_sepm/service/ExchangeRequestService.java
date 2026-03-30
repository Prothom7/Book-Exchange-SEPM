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

    @Autowired
    private ChatRoomService chatRoomService;

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

        ExchangeRequest savedRequest = exchangeRequestRepository.save(exchangeRequest);
        
        // Auto-create chat room for this exchange
        chatRoomService.createChatRoomForExchange(savedRequest);
        
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
        validateCanReviewRequest(exchangeRequest);
        validatePendingOwnershipMapping(exchangeRequest);

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

        cancelConflictingPendingRequests(exchangeRequest);
        chatRoomService.getChatRoomForExchange(exchangeRequestId);

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
        validateCanReviewRequest(exchangeRequest);

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
            chatRoomService.getChatRoomForExchange(exchangeRequestId);
        } else if ("REJECTED".equals(status)) {
            if (exchangeRequest.getStatus() != ExchangeRequest.Status.PENDING) {
                throw new UnauthorizedActionException("Only pending requests can be rejected");
            }
            User reviewer = userService.getCurrentUserEntity();
            exchangeRequest.setStatus(ExchangeRequest.Status.REJECTED);
            exchangeRequest.setReviewedBy(reviewer);
            exchangeRequest.setReviewedAt(java.time.LocalDateTime.now());
            exchangeRequest.setModeratorComment(request.getModeratorComment() != null ? request.getModeratorComment() : "Rejected by moderator");
            chatRoomService.getChatRoomForExchange(exchangeRequestId);
        } else {
            throw new UnauthorizedActionException("Invalid status. Must be APPROVED or REJECTED");
        }

        ExchangeRequest updatedRequest = exchangeRequestRepository.save(exchangeRequest);
        return convertToResponse(updatedRequest);
    }

    /**
     * CRITICAL: Complete an exchange and transfer ownership
     * Only the requester/book owner can confirm completion
     * Transfers ownership of both books and marks them available
     * Flow: PENDING -> APPROVED -> COMPLETED (books transfer ownership)
     */
    @Transactional
    public ExchangeRequestResponse completeExchangeRequest(Long exchangeRequestId) {
        ExchangeRequest exchangeRequest = findExchangeRequestById(exchangeRequestId);

        if (exchangeRequest.getStatus() != ExchangeRequest.Status.APPROVED) {
            throw new UnauthorizedActionException("Only approved requests can be completed. Current status: " + exchangeRequest.getStatus());
        }

        User currentUser = userService.getCurrentUserEntity();
        Long currentUserId = currentUser.getId();

        // Either requester or book owner can complete
        boolean isRequester = exchangeRequest.getRequester().getId().equals(currentUserId);
        boolean isBookOwner = exchangeRequest.getOwner().getId().equals(currentUserId);

        if (!isRequester && !isBookOwner) {
            throw new UnauthorizedActionException("Only the requester or book owner can complete this exchange");
        }

        // OWNERSHIP TRANSFER LOGIC
        Book requestedBook = exchangeRequest.getBook();        // Book to transfer TO requester
        Book offeredBook = exchangeRequest.getOfferedBook();   // Book to transfer TO original owner
        User requester = exchangeRequest.getRequester();
        User originalOwner = exchangeRequest.getOwner();

        // Ensure books are still in expected pre-transfer ownership state.
        if (!requestedBook.getOwner().getId().equals(originalOwner.getId())) {
            throw new UnauthorizedActionException("Requested book ownership changed. Refresh and retry exchange flow.");
        }
        if (!offeredBook.getOwner().getId().equals(requester.getId())) {
            throw new UnauthorizedActionException("Offered book ownership changed. Refresh and retry exchange flow.");
        }

        // Transfer ownership: requester gets the requested book
        requestedBook.setOwner(requester);
        requestedBook.setAvailable(true);

        // Transfer ownership: original owner gets the offered book
        offeredBook.setOwner(originalOwner);
        offeredBook.setAvailable(true);

        // Mark exchange as COMPLETED
        exchangeRequest.setStatus(ExchangeRequest.Status.COMPLETED);
        exchangeRequest.setCompletedAt(java.time.LocalDateTime.now());
        exchangeRequest.setModeratorComment("Exchange completed successfully. Books transferred to new owners.");

        // Save everything
        bookService.updateBook(requestedBook);
        bookService.updateBook(offeredBook);
        ExchangeRequest completedRequest = exchangeRequestRepository.save(exchangeRequest);

        return convertToResponse(completedRequest);
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
        return new ExchangeRequestResponse(
            exchangeRequest.getId(),
            exchangeRequest.getRequester().getId(),
            exchangeRequest.getRequester().getUsername(),
            exchangeRequest.getBook().getId(),
            exchangeRequest.getBook().getTitle(),
            resolveBookImageUrl(exchangeRequest.getBook().getImageUrl(), exchangeRequest.getBook().getIsbn(), exchangeRequest.getBook().getTitle()),
            exchangeRequest.getOwner().getId(),
            exchangeRequest.getOwner().getUsername(),
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
            exchangeRequest.getCompletedAt()
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

    private void validateCanReviewRequest(ExchangeRequest exchangeRequest) {
        User currentUser = userService.getCurrentUserEntity();
        Long currentUserId = currentUser.getId();
        Long ownerId = exchangeRequest.getOwner().getId();

        boolean moderatorOrAdmin = userService.isModerator() || userService.isAdmin();
        if (moderatorOrAdmin || ownerId.equals(currentUserId)) {
            return;
        }

        throw new UnauthorizedActionException("Only the book owner, moderator, or admin can review this request");
    }

    private void validatePendingOwnershipMapping(ExchangeRequest exchangeRequest) {
        if (exchangeRequest.getOwner() == null) {
            throw new UnauthorizedActionException("Exchange request owner is missing. Please recreate request.");
        }

        if (!exchangeRequest.getBook().getOwner().getId().equals(exchangeRequest.getOwner().getId())) {
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
}
