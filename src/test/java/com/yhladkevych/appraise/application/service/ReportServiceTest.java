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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    @Mock
    private ReportRepository reportRepository;

    @Mock
    private OrderRepository orderRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(reportRepository, orderRepository);
    }

    @Test
    void createReport_WhenOrderInProgress_ShouldCreateReportAndUpdateOrder() {
        Long orderId = 1L;
        Long appraiserId = 2L;
        byte[] pdfContent = "test pdf content".getBytes();

        Order order = Order.builder()
                .id(orderId)
                .clientId(1L)
                .appraiserId(appraiserId)
                .status(OrderStatus.IN_PROGRESS)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            report.setId(1L);
            return report;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Report result = reportService.createReport(orderId, pdfContent, appraiserId);

        assertEquals(pdfContent, result.getPdfFile());
        assertEquals(orderId, result.getOrderId());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createReport_WhenOrderNotInProgress_ShouldThrowException() {
        Long orderId = 1L;
        Long appraiserId = 2L;
        byte[] pdfContent = "test pdf content".getBytes();

        Order order = Order.builder()
                .id(orderId)
                .appraiserId(appraiserId)
                .status(OrderStatus.ASSIGNED)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStateException.class,
                () -> reportService.createReport(orderId, pdfContent, appraiserId));
    }

    @Test
    void createReport_WhenAppraiserNotAssigned_ShouldThrowException() {
        Long orderId = 1L;
        Long appraiserId = 2L;
        Long otherAppraiserId = 3L;
        byte[] pdfContent = "test pdf content".getBytes();

        Order order = Order.builder()
                .id(orderId)
                .appraiserId(otherAppraiserId)
                .status(OrderStatus.IN_PROGRESS)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(UnauthorizedException.class,
                () -> reportService.createReport(orderId, pdfContent, appraiserId));
    }

    @Test
    void createReport_WhenOrderNotFound_ShouldThrowException() {
        Long orderId = 1L;
        Long appraiserId = 2L;
        byte[] pdfContent = "test pdf content".getBytes();

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> reportService.createReport(orderId, pdfContent, appraiserId));
    }

    @Test
    void getReportById_WhenExists_ShouldReturnReport() {
        Long reportId = 1L;
        Report report = Report.builder()
                .id(reportId)
                .orderId(1L)
                .pdfFile("test content".getBytes())
                .build();

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));

        Report result = reportService.getReportById(reportId);

        assertEquals(report, result);
    }

    @Test
    void getReportById_WhenNotExists_ShouldThrowException() {
        Long reportId = 1L;
        when(reportRepository.findById(reportId)).thenReturn(Optional.empty());

        assertThrows(ReportNotFoundException.class, () -> reportService.getReportById(reportId));
    }

    @Test
    void getReportByOrderId_WhenExists_ShouldReturnReport() {
        Long orderId = 1L;
        Report report = Report.builder()
                .id(1L)
                .orderId(orderId)
                .pdfFile("test content".getBytes())
                .build();

        when(reportRepository.findByOrderId(orderId)).thenReturn(Optional.of(report));

        Report result = reportService.getReportByOrderId(orderId);

        assertEquals(report, result);
    }

    @Test
    void getReportByOrderId_WhenNotExists_ShouldThrowException() {
        Long orderId = 1L;
        when(reportRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThrows(ReportNotFoundException.class, () -> reportService.getReportByOrderId(orderId));
    }
}

