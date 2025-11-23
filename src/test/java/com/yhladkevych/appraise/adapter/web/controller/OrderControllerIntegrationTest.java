package com.yhladkevych.appraise.adapter.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yhladkevych.appraise.adapter.web.dto.ChangeOrderStatusRequest;
import com.yhladkevych.appraise.adapter.web.dto.CreateOrderRequest;
import com.yhladkevych.appraise.adapter.web.dto.CreateUserRequest;
import com.yhladkevych.appraise.domain.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OrderControllerIntegrationTest {
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

    private Long clientTelegramId;
    private Long appraiserTelegramId;

    @BeforeEach
    void setUp() throws Exception {
        clientTelegramId = 111222333L;
        appraiserTelegramId = 123456789L;

        CreateUserRequest clientRequest = new CreateUserRequest();
        clientRequest.setTelegramId(clientTelegramId);
        mockMvc.perform(post("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clientRequest)));

        CreateUserRequest appraiserRequest = new CreateUserRequest();
        appraiserRequest.setTelegramId(appraiserTelegramId);
        mockMvc.perform(post("/api/v1/users/register-appraiser")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appraiserRequest)));
    }

    @Test
    void createOrder_ShouldCreateOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCarAdUrl("https://example.com/car");
        request.setCarLocation("Kyiv");
        request.setCarPrice(new BigDecimal("50000"));

        mockMvc.perform(post("/api/v1/orders")
                        .param("telegramId", String.valueOf(clientTelegramId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.carAdUrl").value("https://example.com/car"));
    }

    @Test
    void payOrder_WhenStatusIsCreated_ShouldChangeToPaid() throws Exception {
        Long orderId = createOrder();

        mockMvc.perform(post("/api/v1/orders/{orderId}/pay", orderId)
                        .param("telegramId", String.valueOf(clientTelegramId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void changeOrderStatus_WhenValidTransition_ShouldUpdateStatus() throws Exception {
        Long orderId = createOrder();
        payOrder(orderId);

        ChangeOrderStatusRequest request = new ChangeOrderStatusRequest();
        request.setStatus(OrderStatus.APPRAISOR_SEARCH);

        mockMvc.perform(put("/api/v1/orders/{orderId}/status", orderId)
                        .param("telegramId", String.valueOf(clientTelegramId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPRAISOR_SEARCH"));
    }

    @Test
    void getOrder_ShouldReturnOrder() throws Exception {
        Long orderId = createOrder();

        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId)
                        .param("telegramId", String.valueOf(clientTelegramId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId));
    }

    @Test
    void getOrders_ShouldReturnListOfOrders() throws Exception {
        Long orderId1 = createOrder();
        Long orderId2 = createOrder();

        String response = mockMvc.perform(get("/api/v1/orders")
                        .param("telegramId", String.valueOf(clientTelegramId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        // Verify that at least our 2 orders are in the list
        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(response);
        assert jsonNode.isArray();
        assert jsonNode.size() >= 2;
    }

    private Long createOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCarAdUrl("https://example.com/car");
        request.setCarLocation("Kyiv");
        request.setCarPrice(new BigDecimal("50000"));

        String response = mockMvc.perform(post("/api/v1/orders")
                        .param("telegramId", String.valueOf(clientTelegramId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private void payOrder(Long orderId) throws Exception {
        mockMvc.perform(post("/api/v1/orders/{orderId}/pay", orderId)
                .param("telegramId", String.valueOf(clientTelegramId)))
                .andExpect(status().isOk());
    }
}

