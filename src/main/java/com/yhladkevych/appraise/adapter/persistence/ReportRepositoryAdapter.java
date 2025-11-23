package com.yhladkevych.appraise.adapter.persistence;

import com.yhladkevych.appraise.adapter.persistence.entity.ReportEntity;
import com.yhladkevych.appraise.adapter.persistence.repository.JpaReportRepository;
import com.yhladkevych.appraise.domain.model.Report;
import com.yhladkevych.appraise.domain.port.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReportRepositoryAdapter implements ReportRepository {
    private final JpaReportRepository jpaReportRepository;

    @Override
    public Report save(Report report) {
        if (report.getPdfFile() == null) {
            throw new IllegalArgumentException("PDF file cannot be null");
        }
        
        ReportEntity entity = report.getId() != null
                ? jpaReportRepository.findById(report.getId())
                .map(e -> {
                    e.setOrderId(report.getOrderId());
                    e.setPdfFile(report.getPdfFile());
                    e.setUpdatedAt(report.getUpdatedAt());
                    return e;
                })
                .orElse(ReportEntity.fromDomain(report))
                : ReportEntity.fromDomain(report);
        ReportEntity saved = jpaReportRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Report> findById(Long id) {
        return jpaReportRepository.findById(id)
                .map(ReportEntity::toDomain);
    }

    @Override
    public Optional<Report> findByOrderId(Long orderId) {
        return jpaReportRepository.findByOrderId(orderId)
                .map(ReportEntity::toDomain);
    }
}


