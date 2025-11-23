package com.yhladkevych.appraise.domain.model;

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
public class Order {
    private Long id;
    private Long clientId;
    private Long appraiserId;
    private String carAdUrl;
    private LocalDateTime dateCreated;
    private String carLocation;
    private BigDecimal carPrice;
    private OrderStatus status;
    private Long reportId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


