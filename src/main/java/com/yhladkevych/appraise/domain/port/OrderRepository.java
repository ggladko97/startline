package com.yhladkevych.appraise.domain.port;

import com.yhladkevych.appraise.domain.model.Order;
import com.yhladkevych.appraise.domain.model.OrderStatus;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(Long id);
    List<Order> findByClientId(Long clientId);
    List<Order> findByAppraiserId(Long appraiserId);
    List<Order> findByStatus(OrderStatus status);
}


