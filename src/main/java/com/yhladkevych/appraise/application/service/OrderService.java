package com.yhladkevych.appraise.application.service;

import com.yhladkevych.appraise.domain.exception.InvalidOrderStateException;
import com.yhladkevych.appraise.domain.exception.OrderNotFoundException;
import com.yhladkevych.appraise.domain.exception.UnauthorizedException;
import com.yhladkevych.appraise.domain.model.Order;
import com.yhladkevych.appraise.domain.model.OrderStatus;
import com.yhladkevych.appraise.domain.model.UserRole;
import com.yhladkevych.appraise.domain.port.EventPublisher;
import com.yhladkevych.appraise.domain.port.OrderRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderService {
    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, EventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    public Order createOrder(Long clientId, String carAdUrl, String carLocation, BigDecimal carPrice) {
        Order order = Order.builder()
                .clientId(clientId)
                .carAdUrl(carAdUrl)
                .dateCreated(LocalDateTime.now())
                .carLocation(carLocation)
                .carPrice(carPrice)
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);
        eventPublisher.publishOrderCreated(savedOrder);
        return savedOrder;
    }

    public Order payOrder(Long orderId, Long clientId) {
        Order order = getOrderById(orderId);
        validateClientOwnership(order, clientId);

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new InvalidOrderStateException("Order can only be paid when status is CREATED");
        }

        order.setStatus(OrderStatus.PAID);
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        eventPublisher.publishOrderStatusChanged(savedOrder);
        return savedOrder;
    }

    public Order changeOrderStatus(Long orderId, OrderStatus newStatus, Long userId, UserRole userRole) {
        Order order = getOrderById(orderId);

        if (userRole == UserRole.CLIENT) {
            validateClientOwnership(order, userId);
        } else if (userRole == UserRole.APPRAISER) {
            if (newStatus == OrderStatus.ASSIGNED && order.getAppraiserId() == null) {
                order.setAppraiserId(userId);
            } else if (order.getAppraiserId() == null || !order.getAppraiserId().equals(userId)) {
                throw new UnauthorizedException("Appraiser is not assigned to this order");
            }
        }

        validateStatusTransition(order, newStatus, userRole);
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        eventPublisher.publishOrderStatusChanged(savedOrder);
        return savedOrder;
    }

    public Order assignOrderToAppraiser(Long orderId, Long appraiserId) {
        Order order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.ASSIGNED) {
            throw new InvalidOrderStateException("Order can only be assigned when status is ASSIGNED");
        }

        order.setAppraiserId(appraiserId);
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        eventPublisher.publishOrderStatusChanged(savedOrder);
        return savedOrder;
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
    }

    public List<Order> getOrdersByClientId(Long clientId) {
        return orderRepository.findByClientId(clientId);
    }

    public List<Order> getOrdersByAppraiserId(Long appraiserId) {
        return orderRepository.findByAppraiserId(appraiserId);
    }

    private void validateClientOwnership(Order order, Long clientId) {
        if (!order.getClientId().equals(clientId)) {
            throw new UnauthorizedException("User is not authorized to access this order");
        }
    }

    private void validateStatusTransition(Order order, OrderStatus newStatus, UserRole userRole) {
        OrderStatus currentStatus = order.getStatus();
        
        if (newStatus == OrderStatus.DONE) {
            if (order.getReportId() == null) {
                throw new InvalidOrderStateException("Order status cannot be changed to DONE. Report must be attached first.");
            }
        }

        switch (currentStatus) {
            case CREATED:
                if (newStatus != OrderStatus.PAID) {
                    throw new InvalidOrderStateException("Order in CREATED status can only transition to PAID");
                }
                break;
            case PAID:
                if (newStatus != OrderStatus.APPRAISOR_SEARCH) {
                    throw new InvalidOrderStateException("Order in PAID status can only transition to APPRAISOR_SEARCH");
                }
                break;
            case APPRAISOR_SEARCH:
                if (newStatus != OrderStatus.ASSIGNED) {
                    throw new InvalidOrderStateException("Order in APPRAISOR_SEARCH status can only transition to ASSIGNED");
                }
                break;
            case ASSIGNED:
                if (newStatus != OrderStatus.IN_PROGRESS && userRole != UserRole.APPRAISER) {
                    throw new InvalidOrderStateException("Only appraiser can transition from ASSIGNED to IN_PROGRESS");
                }
                break;
            case IN_PROGRESS:
                if (newStatus != OrderStatus.DONE && newStatus != OrderStatus.COMPLETION_FAILURE) {
                    throw new InvalidOrderStateException("Order in IN_PROGRESS status can only transition to DONE or COMPLETION_FAILURE");
                }
                if (userRole != UserRole.APPRAISER) {
                    throw new InvalidOrderStateException("Only appraiser can transition from IN_PROGRESS");
                }
                break;
            case DONE:
            case COMPLETION_FAILURE:
                throw new InvalidOrderStateException("Order in final state cannot be changed");
        }
    }
}

