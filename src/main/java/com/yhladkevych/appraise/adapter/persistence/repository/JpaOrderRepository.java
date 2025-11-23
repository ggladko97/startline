package com.yhladkevych.appraise.adapter.persistence.repository;

import com.yhladkevych.appraise.adapter.persistence.entity.OrderEntity;
import com.yhladkevych.appraise.domain.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaOrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByClientId(Long clientId);
    List<OrderEntity> findByAppraiserId(Long appraiserId);
    List<OrderEntity> findByStatus(OrderStatus status);
}


