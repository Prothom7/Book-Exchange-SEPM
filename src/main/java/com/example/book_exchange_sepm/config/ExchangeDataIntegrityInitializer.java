package com.example.book_exchange_sepm.config;

import com.example.book_exchange_sepm.entity.ExchangeRequest;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.repository.ExchangeRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * CRITICAL: Validates and repairs exchange data on application startup
 * Ensures database integrity and enforces relationship rules
 * 
 * Validation Rules:
 * 1. No NULL values: requester, book, offeredBook cannot be null
 * 2. No self-requests: requester != book.owner
 * 3. Offered book ownership: requester must own offeredBook
 * 4. No duplicate books: book != offeredBook
 * 5. Books must have valid owners
 * 
 * Repair Actions:
 * - Invalid exchanges are auto-cancelled with a comment
 * - Books are marked available if needed
 * - Invalid relationships are documented
 */
@Slf4j
@Component
@Order(150)  // Run before EndpointConfig (200)
public class ExchangeDataIntegrityInitializer {

    @Autowired
    private ExchangeRequestRepository exchangeRequestRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void validateAndRepairExchangeData() {
        log.info("==============================================");
        log.info("🔍 EXCHANGE DATA INTEGRITY CHECK STARTING...");
        log.info("==============================================");

        List<ExchangeRequest> allExchanges = exchangeRequestRepository.findAll();
        int totalExchanges = allExchanges.size();
        int invalidExchanges = 0;
        int validExchanges = 0;

        for (ExchangeRequest exchange : allExchanges) {
            backfillOwnerIfMissing(exchange);
            if (isExchangeValid(exchange)) {
                validExchanges++;
            } else {
                invalidExchanges++;
                repairExchange(exchange);
            }
        }

        log.info("==============================================");
        log.info("✅ EXCHANGE DATA INTEGRITY CHECK COMPLETE");
        log.info("📊 Total Exchanges: {}", totalExchanges);
        log.info("✓ Valid Exchanges: {}", validExchanges);
        log.info("⚠️ Invalid Exchanges (Auto-repaired): {}", invalidExchanges);
        log.info("==============================================");
    }

    /**
     * Comprehensive validation of exchange record
     */
    private boolean isExchangeValid(ExchangeRequest exchange) {
        // Rule 1: No NULL values
        if (exchange.getRequester() == null) {
            log.warn("Exchange ID {} has NULL requester", exchange.getId());
            return false;
        }
        if (exchange.getBook() == null) {
            log.warn("Exchange ID {} has NULL book", exchange.getId());
            return false;
        }
        if (exchange.getOfferedBook() == null) {
            log.warn("Exchange ID {} has NULL offeredBook", exchange.getId());
            return false;
        }

        if (exchange.getOwner() == null) {
            log.warn("Exchange ID {} has NULL owner", exchange.getId());
            return false;
        }

        // Rule 2: Books must have valid owners
        if (exchange.getBook().getOwner() == null) {
            log.warn("Exchange ID {}: book {} has no owner", exchange.getId(), exchange.getBook().getId());
            return false;
        }
        if (exchange.getOfferedBook().getOwner() == null) {
            log.warn("Exchange ID {}: offeredBook {} has no owner", exchange.getId(), exchange.getOfferedBook().getId());
            return false;
        }

        // Rule 3: No self-requests (requester cannot be the owner)
        if (Objects.equals(exchange.getRequester().getId(), exchange.getOwner().getId())) {
            log.warn("Exchange ID {}: SELF-REQUEST detected - requester {} equals owner", 
                exchange.getId(), exchange.getRequester().getId());
            return false;
        }

        // Rule 4: Ownership mapping by status
        if (exchange.getStatus() == ExchangeRequest.Status.COMPLETED) {
            if (!Objects.equals(exchange.getBook().getOwner().getId(), exchange.getRequester().getId())) {
                log.warn("Exchange ID {}: COMPLETED state invalid, requested book not owned by requester", exchange.getId());
                return false;
            }
            if (!Objects.equals(exchange.getOfferedBook().getOwner().getId(), exchange.getOwner().getId())) {
                log.warn("Exchange ID {}: COMPLETED state invalid, offered book not owned by owner", exchange.getId());
                return false;
            }
        } else {
            if (!Objects.equals(exchange.getBook().getOwner().getId(), exchange.getOwner().getId())) {
                log.warn("Exchange ID {}: requested book owner does not match exchange owner", exchange.getId());
                return false;
            }
            if (!Objects.equals(exchange.getOfferedBook().getOwner().getId(), exchange.getRequester().getId())) {
                log.warn("Exchange ID {}: requester {} does not own offeredBook {}", 
                    exchange.getId(), exchange.getRequester().getId(), exchange.getOfferedBook().getId());
                return false;
            }
        }

        // Rule 5: No duplicate books in exchange
        if (Objects.equals(exchange.getBook().getId(), exchange.getOfferedBook().getId())) {
            log.warn("Exchange ID {}: DUPLICATE BOOKS detected", exchange.getId());
            return false;
        }

        return true;
    }

    /**
     * Repair invalid exchange by marking as CANCELLED
     */
    private void repairExchange(ExchangeRequest exchange) {
        String reason = getRepairReason(exchange);

        log.warn("🔧 AUTO-REPAIRING Exchange ID {}: {}", exchange.getId(), reason);

        // Mark as cancelled and mark books as available
        exchange.setStatus(ExchangeRequest.Status.CANCELLED);
        exchange.setModeratorComment("AUTO-REPAIRED: " + reason);

        if (exchange.getBook() != null) {
            exchange.getBook().setAvailable(true);
        }
        if (exchange.getOfferedBook() != null) {
            exchange.getOfferedBook().setAvailable(true);
        }

        exchangeRequestRepository.save(exchange);
    }

    /**
     * Generate human-readable reason for repair
     */
    private String getRepairReason(ExchangeRequest exchange) {
        StringBuilder reason = new StringBuilder();

        if (exchange.getRequester() == null) {
            reason.append("NULL requester. ");
        }
        if (exchange.getOwner() == null) {
            reason.append("NULL owner. ");
        }
        if (exchange.getBook() == null) {
            reason.append("NULL book. ");
        } else if (exchange.getBook().getOwner() == null) {
            reason.append("Book has no owner. ");
        }
        if (exchange.getOfferedBook() == null) {
            reason.append("NULL offeredBook. ");
        } else if (exchange.getOfferedBook().getOwner() == null) {
            reason.append("OfferedBook has no owner. ");
        }

        if (exchange.getRequester() != null && exchange.getOwner() != null) {
            if (Objects.equals(exchange.getRequester().getId(), exchange.getOwner().getId())) {
                reason.append("Self-request detected (requester == owner). ");
            }
        }

        if (exchange.getRequester() != null && exchange.getOfferedBook() != null && exchange.getOfferedBook().getOwner() != null) {
            if (!Objects.equals(exchange.getRequester().getId(), exchange.getOfferedBook().getOwner().getId())) {
                reason.append("Requester does not own offeredBook. ");
            }
        }

        if (exchange.getBook() != null && exchange.getOfferedBook() != null) {
            if (Objects.equals(exchange.getBook().getId(), exchange.getOfferedBook().getId())) {
                reason.append("Duplicate books in exchange. ");
            }
        }

        return reason.length() > 0 ? reason.toString() : "Unknown data integrity issue";
    }

    private void backfillOwnerIfMissing(ExchangeRequest exchange) {
        if (exchange.getOwner() != null) {
            return;
        }

        User inferredOwner = null;
        if (exchange.getStatus() == ExchangeRequest.Status.COMPLETED && exchange.getOfferedBook() != null) {
            inferredOwner = exchange.getOfferedBook().getOwner();
        } else if (exchange.getBook() != null) {
            inferredOwner = exchange.getBook().getOwner();
        }

        if (inferredOwner != null) {
            exchange.setOwner(inferredOwner);
            exchangeRequestRepository.save(exchange);
            log.info("Backfilled owner for exchange ID {}", exchange.getId());
        }
    }
}
