package com.yhladkevych.appraise.adapter.persistence.entity;

import com.yhladkevych.appraise.domain.model.Order;
import com.yhladkevych.appraise.domain.model.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long clientId;

    @Column
    private Long appraiserId;

    @Column(nullable = false, length = 2048)
    private String carAdUrl;

    @Column(nullable = false)
    private LocalDateTime dateCreated;

    @Column(nullable = false)
    private String carLocation;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal carPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column
    private Long reportId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Order toDomain() {
        return Order.builder()
                .id(this.id)
                .clientId(this.clientId)
                .appraiserId(this.appraiserId)
                .carAdUrl(this.carAdUrl)
                .dateCreated(this.dateCreated)
                .carLocation(this.carLocation)
                .carPrice(this.carPrice)
                .status(this.status)
                .reportId(this.reportId)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    public static OrderEntity fromDomain(Order order) {
        return OrderEntity.builder()
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


