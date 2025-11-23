package com.yhladkevych.appraise.adapter.persistence;

import com.yhladkevych.appraise.adapter.persistence.entity.OrderEntity;
import com.yhladkevych.appraise.adapter.persistence.repository.JpaOrderRepository;
import com.yhladkevych.appraise.domain.model.Order;
import com.yhladkevych.appraise.domain.model.OrderStatus;
import com.yhladkevych.appraise.domain.port.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {
    private final JpaOrderRepository jpaOrderRepository;

    @Override
    public Order save(Order order) {
        OrderEntity entity = order.getId() != null
                ? jpaOrderRepository.findById(order.getId())
                .map(e -> {
                    e.setClientId(order.getClientId());
                    e.setAppraiserId(order.getAppraiserId());
                    e.setCarAdUrl(order.getCarAdUrl());
                    e.setDateCreated(order.getDateCreated());
                    e.setCarLocation(order.getCarLocation());
                    e.setCarPrice(order.getCarPrice());
                    e.setStatus(order.getStatus());
                    e.setReportId(order.getReportId());
                    e.setUpdatedAt(order.getUpdatedAt());
                    return e;
                })
                .orElse(OrderEntity.fromDomain(order))
                : OrderEntity.fromDomain(order);
        OrderEntity saved = jpaOrderRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Order> findById(Long id) {
        return jpaOrderRepository.findById(id)
                .map(OrderEntity::toDomain);
    }

    @Override
    public List<Order> findByClientId(Long clientId) {
        return jpaOrderRepository.findByClientId(clientId).stream()
                .map(OrderEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findByAppraiserId(Long appraiserId) {
        return jpaOrderRepository.findByAppraiserId(appraiserId).stream()
                .map(OrderEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return jpaOrderRepository.findByStatus(status).stream()
                .map(OrderEntity::toDomain)
                .collect(Collectors.toList());
    }
}


