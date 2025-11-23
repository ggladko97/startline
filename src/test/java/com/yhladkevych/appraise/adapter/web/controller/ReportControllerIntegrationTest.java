package com.yhladkevych.appraise.adapter.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yhladkevych.appraise.adapter.persistence.entity.ReportEntity;
import com.yhladkevych.appraise.adapter.persistence.repository.JpaReportRepository;
import com.yhladkevych.appraise.adapter.telegram.TelegramNotificationServiceAdapter;
import com.yhladkevych.appraise.adapter.web.dto.CreateOrderRequest;
import com.yhladkevych.appraise.adapter.web.dto.CreateUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ReportControllerIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.telegram.appraiser-whitelist", () -> "123456789");
        registry.add("app.telegram.bot-token", () -> "test-token");
        registry.add("app.telegram.api-url", () -> "https://api.telegram.org");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JpaReportRepository jpaReportRepository;

    @MockBean
    private TelegramNotificationServiceAdapter telegramNotificationServiceAdapter;

    private Long clientTelegramId;
    private Long appraiserTelegramId;

    @BeforeEach
    void setUp() throws Exception {
        clientTelegramId = 111222333L;
        appraiserTelegramId = 123456789L;

        CreateUserRequest clientRequest = new CreateUserRequest();
        clientRequest.setTelegramId(clientTelegramId);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clientRequest)));

        CreateUserRequest appraiserRequest = new CreateUserRequest();
        appraiserRequest.setTelegramId(appraiserTelegramId);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/users/register-appraiser")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appraiserRequest)));
    }

    @Test
    void uploadReport_WhenAppraiser_ShouldCreateReport() throws Exception {
        Long orderId = createOrderAndSetToInProgress();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.pdf",
                "application/pdf",
                "test pdf content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/reports/orders/{orderId}", orderId)
                        .file(file)
                        .param("telegramId", String.valueOf(appraiserTelegramId)))
                .andExpect(status().isCreated());
    }

    @Test
    void uploadReport_WhenNotAppraiser_ShouldReturnForbidden() throws Exception {
        Long orderId = createOrderAndSetToInProgress();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.pdf",
                "application/pdf",
                "test pdf content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/reports/orders/{orderId}", orderId)
                        .file(file)
                        .param("telegramId", String.valueOf(clientTelegramId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getReportByOrderId_ShouldReturnPdf() throws Exception {
        Long orderId = createOrderAndSetToInProgress();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.pdf",
                "application/pdf",
                "test pdf content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/reports/orders/{orderId}", orderId)
                        .file(file)
                        .param("telegramId", String.valueOf(appraiserTelegramId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/reports/orders/{orderId}", orderId)
                        .param("telegramId", String.valueOf(clientTelegramId)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(content().bytes("test pdf content".getBytes()));
    }

    private Long createOrderAndSetToInProgress() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCarAdUrl("https://example.com/car");
        request.setCarLocation("Kyiv");
        request.setCarPrice(new BigDecimal("50000"));

        String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/orders")
                        .param("telegramId", String.valueOf(clientTelegramId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long orderId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/orders/{orderId}/pay", orderId)
                .param("telegramId", String.valueOf(clientTelegramId)))
                .andExpect(status().isOk());

        // Wait for async OrderCreatedEvent to process (it automatically transitions to APPRAISOR_SEARCH)
        Thread.sleep(100);

        // Check current order status and transition to APPRAISOR_SEARCH if still PAID
        // (The async event might have already changed it to APPRAISOR_SEARCH)
        String orderResponse = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/orders/{orderId}", orderId)
                        .param("telegramId", String.valueOf(clientTelegramId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String currentStatus = objectMapper.readTree(orderResponse).get("status").asText();
        
        // Only transition to APPRAISOR_SEARCH if still PAID (async event might have already processed)
        if ("PAID".equals(currentStatus)) {
            com.yhladkevych.appraise.adapter.web.dto.ChangeOrderStatusRequest statusRequest = 
                    new com.yhladkevych.appraise.adapter.web.dto.ChangeOrderStatusRequest();
            statusRequest.setStatus(com.yhladkevych.appraise.domain.model.OrderStatus.APPRAISOR_SEARCH);
            
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/orders/{orderId}/status", orderId)
                    .param("telegramId", String.valueOf(clientTelegramId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(statusRequest)))
                    .andExpect(status().isOk());
        }

        // Change status from APPRAISOR_SEARCH to ASSIGNED
        com.yhladkevych.appraise.adapter.web.dto.ChangeOrderStatusRequest statusRequest = 
                new com.yhladkevych.appraise.adapter.web.dto.ChangeOrderStatusRequest();
        statusRequest.setStatus(com.yhladkevych.appraise.domain.model.OrderStatus.ASSIGNED);
        
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/orders/{orderId}/status", orderId)
                .param("telegramId", String.valueOf(clientTelegramId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk());

        // Assign order to appraiser
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/orders/{orderId}/assign", orderId)
                .param("telegramId", String.valueOf(appraiserTelegramId)))
                .andExpect(status().isOk());

        // Change status to IN_PROGRESS
        statusRequest = new com.yhladkevych.appraise.adapter.web.dto.ChangeOrderStatusRequest();
        statusRequest.setStatus(com.yhladkevych.appraise.domain.model.OrderStatus.IN_PROGRESS);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/orders/{orderId}/status", orderId)
                .param("telegramId", String.valueOf(appraiserTelegramId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk());

        return orderId;
    }

    @Test
    void uploadReport_ShouldStorePdfAsByteaInDatabase() throws Exception {
        Long orderId = createOrderAndSetToInProgress();

        // Create a realistic PDF byte array (PDF header + content)
        byte[] pdfContent = createRealisticPdfContent();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.pdf",
                "application/pdf",
                pdfContent
        );

        // Upload the report
        mockMvc.perform(multipart("/api/v1/reports/orders/{orderId}", orderId)
                        .file(file)
                        .param("telegramId", String.valueOf(appraiserTelegramId)))
                .andExpect(status().isCreated());

        // Verify the report is stored correctly in the database
        Optional<ReportEntity> savedReport = jpaReportRepository.findByOrderId(orderId);
        assertTrue(savedReport.isPresent(), "Report should be saved in database");

        ReportEntity reportEntity = savedReport.get();
        assertNotNull(reportEntity.getId(), "Report should have an ID");
        assertEquals(orderId, reportEntity.getOrderId(), "Report should be associated with the correct order");
        assertNotNull(reportEntity.getPdfFile(), "PDF file should not be null");
        assertTrue(reportEntity.getPdfFile().length > 0, "PDF file should not be empty");

        // Verify the byte array matches exactly what was uploaded
        assertArrayEquals(pdfContent, reportEntity.getPdfFile(),
                "PDF content stored in database should match uploaded content exactly");

        // Verify PDF header is preserved (PDF files start with %PDF-)
        String pdfHeader = new String(reportEntity.getPdfFile(), 0, Math.min(4, reportEntity.getPdfFile().length));
        assertEquals("%PDF", pdfHeader, "PDF header should be preserved");
    }

    @Test
    void uploadReport_WithLargePdf_ShouldStoreCorrectlyAsBytea() throws Exception {
        Long orderId = createOrderAndSetToInProgress();

        // Create a larger PDF (simulating a real report with more content)
        byte[] largePdfContent = createLargePdfContent(100 * 1024); // 100KB

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large-report.pdf",
                "application/pdf",
                largePdfContent
        );

        // Upload the report
        mockMvc.perform(multipart("/api/v1/reports/orders/{orderId}", orderId)
                        .file(file)
                        .param("telegramId", String.valueOf(appraiserTelegramId)))
                .andExpect(status().isCreated());

        // Verify the large PDF is stored correctly
        Optional<ReportEntity> savedReport = jpaReportRepository.findByOrderId(orderId);
        assertTrue(savedReport.isPresent(), "Report should be saved in database");

        ReportEntity reportEntity = savedReport.get();
        assertEquals(largePdfContent.length, reportEntity.getPdfFile().length,
                "Large PDF size should match uploaded size");
        assertArrayEquals(largePdfContent, reportEntity.getPdfFile(),
                "Large PDF content should match uploaded content exactly");
    }

    private byte[] createRealisticPdfContent() {
        // Create a minimal valid PDF structure
        // PDF header: %PDF-1.4
        // This simulates a real PDF file structure
        String pdfContent = "%PDF-1.4\n" +
                "%\u00E2\u00E3\u00CF\u00D3\n" +
                "1 0 obj\n" +
                "<<\n" +
                "/Type /Catalog\n" +
                "/Pages 2 0 R\n" +
                ">>\n" +
                "endobj\n" +
                "2 0 obj\n" +
                "<<\n" +
                "/Type /Pages\n" +
                "/Kids [3 0 R]\n" +
                "/Count 1\n" +
                ">>\n" +
                "endobj\n" +
                "3 0 obj\n" +
                "<<\n" +
                "/Type /Page\n" +
                "/Parent 2 0 R\n" +
                "/MediaBox [0 0 612 792]\n" +
                "/Contents 4 0 R\n" +
                "/Resources <<\n" +
                "/Font <<\n" +
                "/F1 5 0 R\n" +
                ">>\n" +
                ">>\n" +
                ">>\n" +
                "endobj\n" +
                "4 0 obj\n" +
                "<<\n" +
                "/Length 44\n" +
                ">>\n" +
                "stream\n" +
                "BT\n" +
                "/F1 12 Tf\n" +
                "100 700 Td\n" +
                "(Test PDF Content) Tj\n" +
                "ET\n" +
                "endstream\n" +
                "endobj\n" +
                "5 0 obj\n" +
                "<<\n" +
                "/Type /Font\n" +
                "/Subtype /Type1\n" +
                "/BaseFont /Helvetica\n" +
                ">>\n" +
                "endobj\n" +
                "xref\n" +
                "0 6\n" +
                "0000000000 65535 f \n" +
                "0000000009 00000 n \n" +
                "0000000058 00000 n \n" +
                "0000000115 00000 n \n" +
                "0000000307 00000 n \n" +
                "0000000440 00000 n \n" +
                "trailer\n" +
                "<<\n" +
                "/Size 6\n" +
                "/Root 1 0 R\n" +
                ">>\n" +
                "startxref\n" +
                "527\n" +
                "%%EOF";
        return pdfContent.getBytes();
    }

    private byte[] createLargePdfContent(int size) {
        byte[] content = new byte[size];
        // Fill with PDF header and then pad with data
        byte[] header = "%PDF-1.4\n".getBytes();
        System.arraycopy(header, 0, content, 0, Math.min(header.length, size));
        // Fill the rest with test data
        for (int i = header.length; i < size; i++) {
            content[i] = (byte) (i % 256);
        }
        return content;
    }
}

