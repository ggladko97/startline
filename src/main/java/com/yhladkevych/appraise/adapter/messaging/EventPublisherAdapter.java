package com.yhladkevych.appraise.adapter.messaging;

import com.yhladkevych.appraise.domain.model.Order;
import com.yhladkevych.appraise.domain.port.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventPublisherAdapter implements EventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publishOrderCreated(Order order) {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(order.getId())
                .clientId(order.getClientId())
                .carAdUrl(order.getCarAdUrl())
                .dateCreated(order.getDateCreated())
                .carLocation(order.getCarLocation())
                .carPrice(order.getCarPrice())
                .build();
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publishOrderStatusChanged(Order order) {
        OrderStatusChangedEvent event = OrderStatusChangedEvent.builder()
                .orderId(order.getId())
                .status(order.getStatus())
                .appraiserId(order.getAppraiserId())
                .build();
        applicationEventPublisher.publishEvent(event);
    }
}

