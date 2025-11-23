package com.yhladkevych.appraise.application.service;

import com.yhladkevych.appraise.domain.exception.InvalidOrderStateException;
import com.yhladkevych.appraise.domain.exception.OrderNotFoundException;
import com.yhladkevych.appraise.domain.exception.ReportNotFoundException;
import com.yhladkevych.appraise.domain.exception.UnauthorizedException;
import com.yhladkevych.appraise.domain.model.Order;
import com.yhladkevych.appraise.domain.model.OrderStatus;
import com.yhladkevych.appraise.domain.model.Report;
import com.yhladkevych.appraise.domain.port.OrderRepository;
import com.yhladkevych.appraise.domain.port.ReportRepository;

import java.time.LocalDateTime;

public class ReportService {
    private final ReportRepository reportRepository;
    private final OrderRepository orderRepository;

    public ReportService(ReportRepository reportRepository, OrderRepository orderRepository) {
        this.reportRepository = reportRepository;
        this.orderRepository = orderRepository;
    }

    public Report createReport(Long orderId, byte[] pdfFile, Long appraiserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        if (order.getAppraiserId() == null || !order.getAppraiserId().equals(appraiserId)) {
            throw new UnauthorizedException("Appraiser is not assigned to this order");
        }

        if (order.getStatus() != OrderStatus.IN_PROGRESS) {
            throw new InvalidOrderStateException("Report can only be added when order status is IN_PROGRESS");
        }

        Report report = Report.builder()
                .orderId(orderId)
                .pdfFile(pdfFile)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Report savedReport = reportRepository.save(report);
        order.setReportId(savedReport.getId());
        order.setStatus(OrderStatus.DONE);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        return savedReport;
    }

    public Report getReportById(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException("Report not found with ID: " + reportId));
    }

    public Report getReportByOrderId(Long orderId) {
        return reportRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ReportNotFoundException("Report not found for order ID: " + orderId));
    }
}


