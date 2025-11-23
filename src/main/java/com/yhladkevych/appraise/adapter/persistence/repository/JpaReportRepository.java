package com.yhladkevych.appraise.adapter.persistence.repository;

import com.yhladkevych.appraise.adapter.persistence.entity.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaReportRepository extends JpaRepository<ReportEntity, Long> {
    Optional<ReportEntity> findByOrderId(Long orderId);
}


