package com.yhladkevych.appraise.application.service;

import com.yhladkevych.appraise.domain.exception.InvalidOrderStateException;
import com.yhladkevych.appraise.domain.exception.OrderNotFoundException;
import com.yhladkevych.appraise.domain.exception.UnauthorizedException;
import com.yhladkevych.appraise.domain.model.Order;
import com.yhladkevych.appraise.domain.model.OrderStatus;
import com.yhladkevych.appraise.domain.model.UserRole;
import com.yhladkevych.appraise.domain.port.EventPublisher;
import com.yhladkevych.appraise.domain.port.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EventPublisher eventPublisher;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, eventPublisher);
    }

    @Test
    void createOrder_ShouldCreateAndPublishEvent() {
        Long clientId = 1L;
        String carAdUrl = "https://example.com/car";
        String carLocation = "Kyiv";
        BigDecimal carPrice = new BigDecimal("50000");

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        Order result = orderService.createOrder(clientId, carAdUrl, carLocation, carPrice);

        assertEquals(OrderStatus.CREATED, result.getStatus());
        assertEquals(clientId, result.getClientId());
        assertEquals(carAdUrl, result.getCarAdUrl());
        verify(eventPublisher).publishOrderCreated(any(Order.class));
    }

    @Test
    void payOrder_WhenStatusIsCreated_ShouldChangeToPaid() {
        Long orderId = 1L;
        Long clientId = 1L;
        Order order = createOrder(orderId, clientId, OrderStatus.CREATED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.payOrder(orderId, clientId);

        assertEquals(OrderStatus.PAID, result.getStatus());
        verify(eventPublisher).publishOrderStatusChanged(any(Order.class));
    }

    @Test
    void payOrder_WhenStatusIsNotCreated_ShouldThrowException() {
        Long orderId = 1L;
        Long clientId = 1L;
        Order order = createOrder(orderId, clientId, OrderStatus.PAID);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStateException.class, () -> orderService.payOrder(orderId, clientId));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void payOrder_WhenClientNotOwner_ShouldThrowException() {
        Long orderId = 1L;
        Long clientId = 1L;
        Long otherClientId = 2L;
        Order order = createOrder(orderId, otherClientId, OrderStatus.CREATED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(UnauthorizedException.class, () -> orderService.payOrder(orderId, clientId));
    }

    @Test
    void changeOrderStatus_WhenTransitionIsValid_ShouldUpdateStatus() {
        Long orderId = 1L;
        Long userId = 1L;
        Order order = createOrder(orderId, userId, OrderStatus.PAID);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.changeOrderStatus(orderId, OrderStatus.APPRAISOR_SEARCH, userId, UserRole.CLIENT);

        assertEquals(OrderStatus.APPRAISOR_SEARCH, result.getStatus());
        verify(eventPublisher).publishOrderStatusChanged(any(Order.class));
    }

    @Test
    void changeOrderStatus_WhenTransitionIsInvalid_ShouldThrowException() {
        Long orderId = 1L;
        Long userId = 1L;
        Order order = createOrder(orderId, userId, OrderStatus.CREATED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStateException.class,
                () -> orderService.changeOrderStatus(orderId, OrderStatus.APPRAISOR_SEARCH, userId, UserRole.CLIENT));
    }

    @Test
    void changeOrderStatus_ToDone_WhenReportNotAttached_ShouldThrowException() {
        Long orderId = 1L;
        Long userId = 1L;
        Order order = createOrder(orderId, userId, OrderStatus.IN_PROGRESS);
        order.setReportId(null);
        order.setAppraiserId(userId);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStateException.class,
                () -> orderService.changeOrderStatus(orderId, OrderStatus.DONE, userId, UserRole.APPRAISER));
    }

    @Test
    void changeOrderStatus_ToDone_WhenReportAttached_ShouldSucceed() {
        Long orderId = 1L;
        Long userId = 1L;
        Order order = createOrder(orderId, userId, OrderStatus.IN_PROGRESS);
        order.setReportId(1L);
        order.setAppraiserId(userId);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.changeOrderStatus(orderId, OrderStatus.DONE, userId, UserRole.APPRAISER);

        assertEquals(OrderStatus.DONE, result.getStatus());
    }

    @Test
    void getOrderById_WhenExists_ShouldReturnOrder() {
        Long orderId = 1L;
        Order order = createOrder(orderId, 1L, OrderStatus.CREATED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Order result = orderService.getOrderById(orderId);

        assertEquals(order, result);
    }

    @Test
    void getOrderById_WhenNotExists_ShouldThrowException() {
        Long orderId = 1L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(orderId));
    }

    private Order createOrder(Long id, Long clientId, OrderStatus status) {
        return Order.builder()
                .id(id)
                .clientId(clientId)
                .carAdUrl("https://example.com/car")
                .dateCreated(LocalDateTime.now())
                .carLocation("Kyiv")
                .carPrice(new BigDecimal("50000"))
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}

