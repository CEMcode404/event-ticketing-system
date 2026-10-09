package com.eventticketing.repository;

import com.eventticketing.entity.Order;
import com.eventticketing.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByStripeSessionId(String stripeSessionId);

    Optional<Order> findFirstByHoldTokenAndStatus(String holdToken, OrderStatus status);
}