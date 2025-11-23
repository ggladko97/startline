package com.yhladkevych.appraise.domain.port;

import com.yhladkevych.appraise.domain.model.Order;

public interface EventPublisher {
    void publishOrderCreated(Order order);
    void publishOrderStatusChanged(Order order);
}


