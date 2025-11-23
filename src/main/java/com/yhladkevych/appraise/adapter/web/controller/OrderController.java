package com.yhladkevych.appraise.adapter.web.controller;

import com.yhladkevych.appraise.adapter.web.dto.ChangeOrderStatusRequest;
import com.yhladkevych.appraise.adapter.web.dto.CreateOrderRequest;
import com.yhladkevych.appraise.adapter.web.dto.OrderResponse;
import com.yhladkevych.appraise.application.service.OrderService;
import com.yhladkevych.appraise.application.service.UserService;
import com.yhladkevych.appraise.domain.model.Order;
import com.yhladkevych.appraise.domain.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestParam Long telegramId) {
        String traceId = MDC.get("traceId");
        try {
            User user = userService.getUserByTelegramId(telegramId);
            Order order = orderService.createOrder(
                    user.getId(),
                    request.getCarAdUrl(),
                    request.getCarLocation(),
                    request.getCarPrice()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(order));
        } finally {
            if (traceId != null) {
                MDC.put("traceId", traceId);
            }
        }
    }

    @PostMapping("/{orderId}/pay")
    public ResponseEntity<OrderResponse> payOrder(
            @PathVariable Long orderId,
            @RequestParam Long telegramId) {
        String traceId = MDC.get("traceId");
        try {
            User user = userService.getUserByTelegramId(telegramId);
            Order order = orderService.payOrder(orderId, user.getId());
            return ResponseEntity.ok(toResponse(order));
        } finally {
            if (traceId != null) {
                MDC.put("traceId", traceId);
            }
        }
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> changeOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody ChangeOrderStatusRequest request,
            @RequestParam Long telegramId) {
        String traceId = MDC.get("traceId");
        try {
            User user = userService.getUserByTelegramId(telegramId);
            Order order = orderService.changeOrderStatus(orderId, request.getStatus(), user.getId(), user.getRole());
            return ResponseEntity.ok(toResponse(order));
        } finally {
            if (traceId != null) {
                MDC.put("traceId", traceId);
            }
        }
    }

    @PostMapping("/{orderId}/assign")
    public ResponseEntity<OrderResponse> assignOrder(
            @PathVariable Long orderId,
            @RequestParam Long telegramId) {
        String traceId = MDC.get("traceId");
        try {
            User user = userService.getUserByTelegramId(telegramId);
            Order order = orderService.assignOrderToAppraiser(orderId, user.getId());
            return ResponseEntity.ok(toResponse(order));
        } finally {
            if (traceId != null) {
                MDC.put("traceId", traceId);
            }
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable Long orderId,
            @RequestParam Long telegramId) {
        String traceId = MDC.get("traceId");
        try {
            userService.getUserByTelegramId(telegramId);
            Order order = orderService.getOrderById(orderId);
            return ResponseEntity.ok(toResponse(order));
        } finally {
            if (traceId != null) {
                MDC.put("traceId", traceId);
            }
        }
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(@RequestParam Long telegramId) {
        String traceId = MDC.get("traceId");
        try {
            User user = userService.getUserByTelegramId(telegramId);
            List<Order> orders;
            if (user.getRole().name().equals("CLIENT")) {
                orders = orderService.getOrdersByClientId(user.getId());
            } else {
                orders = orderService.getOrdersByAppraiserId(user.getId());
            }
            return ResponseEntity.ok(orders.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList()));
        } finally {
            if (traceId != null) {
                MDC.put("traceId", traceId);
            }
        }
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .clientId(order.getClientId())
                .appraiserId(order.getAppraiserId())
                .carAdUrl(order.getCarAdUrl())
                .dateCreated(order.getDateCreated())
                .carLocation(order.getCarLocation())
                .carPrice(order.getCarPrice())
                .status(order.getStatus())
                .reportId(order.getReportId())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}

