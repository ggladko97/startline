package com.yhladkevych.appraise.adapter.persistence.entity;

import com.yhladkevych.appraise.domain.model.Report;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "pdf_file", nullable = false, columnDefinition = "BYTEA")
    private byte[] pdfFile;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Report toDomain() {
        return Report.builder()
                .id(this.id)
                .orderId(this.orderId)
                .pdfFile(this.pdfFile)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    public static ReportEntity fromDomain(Report report) {
        return ReportEntity.builder()
                .id(report.getId())
                .orderId(report.getOrderId())
                .pdfFile(report.getPdfFile())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}


