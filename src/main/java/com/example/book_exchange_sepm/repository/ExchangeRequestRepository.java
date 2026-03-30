package com.example.book_exchange_sepm.repository;

import com.example.book_exchange_sepm.entity.ExchangeRequest;
import com.example.book_exchange_sepm.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRequestRepository extends JpaRepository<ExchangeRequest, Long> {
    List<ExchangeRequest> findAllByOrderByCreatedAtDesc();
    List<ExchangeRequest> findByOwner_IdOrderByCreatedAtDesc(Long ownerId);
    List<ExchangeRequest> findByRequester_IdOrderByCreatedAtDesc(Long requesterId);
    List<ExchangeRequest> findByBook(Book book);
    List<ExchangeRequest> findByStatusOrderByCreatedAtDesc(ExchangeRequest.Status status);
    boolean existsByRequester_IdAndBook_IdAndStatus(Long requesterId, Long bookId, ExchangeRequest.Status status);
    boolean existsByRequester_IdAndBook_IdAndOfferedBook_IdAndStatus(Long requesterId,
                                                                      Long bookId,
                                                                      Long offeredBookId,
                                                                      ExchangeRequest.Status status);

    // Data visibility queries
    List<ExchangeRequest> findByRequester_IdAndStatusOrderByCreatedAtDesc(Long requesterId, ExchangeRequest.Status status);
        List<ExchangeRequest> findByOwner_IdAndStatusOrderByCreatedAtDesc(Long ownerId, ExchangeRequest.Status status);

        @Query("""
                select e from ExchangeRequest e
                where e.id = :exchangeId
                    and (e.requester.id = :userId or e.owner.id = :userId)
                """)
        Optional<ExchangeRequest> findVisibleByIdForUser(@Param("exchangeId") Long exchangeId,
                                                                                                         @Param("userId") Long userId);

        @Query("""
                select e from ExchangeRequest e
                where e.status = com.example.book_exchange_sepm.entity.ExchangeRequest$Status.PENDING
                    and e.id <> :exchangeId
                    and (
                        e.book.id = :requestedBookId
                        or e.book.id = :offeredBookId
                        or e.offeredBook.id = :requestedBookId
                        or e.offeredBook.id = :offeredBookId
                    )
                """)
        List<ExchangeRequest> findConflictingPendingRequests(@Param("exchangeId") Long exchangeId,
                                                                                                                 @Param("requestedBookId") Long requestedBookId,
                                                                                                                 @Param("offeredBookId") Long offeredBookId);
}
