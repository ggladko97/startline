package com.yhladkevych.appraise.adapter.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private Long orderId;
    private Long clientId;
    private String carAdUrl;
    private LocalDateTime dateCreated;
    private String carLocation;
    private BigDecimal carPrice;
}


