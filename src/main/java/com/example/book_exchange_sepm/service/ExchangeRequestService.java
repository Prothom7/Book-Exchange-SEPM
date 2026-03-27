package com.example.book_exchange_sepm.service;

import com.example.book_exchange_sepm.dto.ExchangeRequestRequest;
import com.example.book_exchange_sepm.dto.ExchangeRequestResponse;
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
        Book book = bookService.findBookById(request.getBookId());

        // Cannot request your own book
        if (book.getOwner().getId().equals(requester.getId())) {
            throw new UnauthorizedActionException("You cannot request your own book");
        }

        ExchangeRequest exchangeRequest = new ExchangeRequest();
        exchangeRequest.setRequester(requester);
        exchangeRequest.setBook(book);
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
     * Approve exchange request
     * OWNERSHIP ENFORCED: Only book owner can approve requests for their book
     */
    @Transactional
    public ExchangeRequestResponse approveExchangeRequest(Long exchangeRequestId) {
        ExchangeRequest exchangeRequest = findExchangeRequestById(exchangeRequestId);

        // Only book owner can approve
        userService.validateOwnershipOrAdmin(exchangeRequest.getBook().getOwner().getId());

        exchangeRequest.setStatus(ExchangeRequest.Status.APPROVED);
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

        // Only book owner can reject
        userService.validateOwnershipOrAdmin(exchangeRequest.getBook().getOwner().getId());

        exchangeRequest.setStatus(ExchangeRequest.Status.REJECTED);
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

        exchangeRequest.setStatus(ExchangeRequest.Status.CANCELLED);
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
            exchangeRequest.getStatus().toString(),
            exchangeRequest.getMessage(),
            exchangeRequest.getCreatedAt(),
            exchangeRequest.getUpdatedAt()
        );
    }
}
