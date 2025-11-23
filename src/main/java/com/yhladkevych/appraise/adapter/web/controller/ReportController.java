package com.yhladkevych.appraise.adapter.web.controller;

import com.yhladkevych.appraise.application.service.ReportService;
import com.yhladkevych.appraise.application.service.UserService;
import com.yhladkevych.appraise.domain.model.Report;
import com.yhladkevych.appraise.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;
    private final UserService userService;

    @PostMapping("/orders/{orderId}")
    public ResponseEntity<Void> uploadReport(
            @PathVariable Long orderId,
            @RequestParam("file") MultipartFile file,
            @RequestParam Long telegramId) {
        String traceId = MDC.get("traceId");
        try {
            User user = userService.getUserByTelegramId(telegramId);
            if (!user.getRole().name().equals("APPRAISER")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            byte[] pdfContent = file.getBytes();
            reportService.createReport(orderId, pdfContent, user.getId());
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } finally {
            if (traceId != null) {
                MDC.put("traceId", traceId);
            }
        }
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<byte[]> getReportByOrderId(
            @PathVariable Long orderId,
            @RequestParam Long telegramId) {
        String traceId = MDC.get("traceId");
        try {
            userService.getUserByTelegramId(telegramId);
            Report report = reportService.getReportByOrderId(orderId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "report-" + orderId + ".pdf");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(report.getPdfFile());
        } finally {
            if (traceId != null) {
                MDC.put("traceId", traceId);
            }
        }
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<byte[]> getReport(
            @PathVariable Long reportId,
            @RequestParam Long telegramId) {
        String traceId = MDC.get("traceId");
        try {
            userService.getUserByTelegramId(telegramId);
            Report report = reportService.getReportById(reportId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "report-" + reportId + ".pdf");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(report.getPdfFile());
        } finally {
            if (traceId != null) {
                MDC.put("traceId", traceId);
            }
        }
    }
}

