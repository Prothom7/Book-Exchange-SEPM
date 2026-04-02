package com.example.book_exchange_sepm.repository;

import com.example.book_exchange_sepm.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    Optional<Delivery> findByExchangeRequest_Id(Long exchangeRequestId);

    boolean existsByExchangeRequest_Id(Long exchangeRequestId);

    List<Delivery> findByDeliveryMan_IdOrderByUpdatedAtDesc(Long deliveryManId);

    long countByDeliveryMan_IdAndStatusIn(Long deliveryManId, Collection<Delivery.Status> statuses);
}
