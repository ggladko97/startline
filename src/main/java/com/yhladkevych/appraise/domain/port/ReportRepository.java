package com.yhladkevych.appraise.domain.port;

import com.yhladkevych.appraise.domain.model.Report;

import java.util.Optional;

public interface ReportRepository {
    Report save(Report report);
    Optional<Report> findById(Long id);
    Optional<Report> findByOrderId(Long orderId);
}


