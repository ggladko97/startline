package com.yhladkevych.appraise.adapter.messaging;

import com.yhladkevych.appraise.adapter.telegram.TelegramNotificationServiceAdapter;
import com.yhladkevych.appraise.domain.model.OrderStatus;
import com.yhladkevych.appraise.domain.port.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {
    private final TelegramNotificationServiceAdapter telegramNotificationService;
    private final OrderRepository orderRepository;

    @Async
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        String traceId = MDC.get("traceId");
        try {
            MDC.put("traceId", traceId != null ? traceId : "event-" + System.currentTimeMillis());
            log.info("Processing order created event for order ID: {}", event.getOrderId());

            String message = String.format(
                    "New order available!\nCar: %s\nLocation: %s\nPrice: %s",
                    event.getCarAdUrl(),
                    event.getCarLocation(),
                    event.getCarPrice()
            );

            telegramNotificationService.sendNotificationToAppraisers(message, event.getOrderId());

            orderRepository.findById(event.getOrderId()).ifPresent(order -> {
                order.setStatus(OrderStatus.APPRAISOR_SEARCH);
                orderRepository.save(order);
            });

            log.info("Order created event processed successfully for order ID: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Error processing order created event for order ID: {}", event.getOrderId(), e);
        } finally {
            MDC.clear();
        }
    }

    @Async
    @EventListener
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        String traceId = MDC.get("traceId");
        try {
            MDC.put("traceId", traceId != null ? traceId : "event-" + System.currentTimeMillis());
            log.info("Processing order status changed event for order ID: {}, new status: {}", 
                    event.getOrderId(), event.getStatus());
        } catch (Exception e) {
            log.error("Error processing order status changed event for order ID: {}", event.getOrderId(), e);
        } finally {
            MDC.clear();
        }
    }
}


