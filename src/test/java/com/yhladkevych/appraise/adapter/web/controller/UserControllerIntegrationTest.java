package com.yhladkevych.appraise.adapter.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yhladkevych.appraise.adapter.web.dto.CreateUserRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UserControllerIntegrationTest {
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

    @Test
    void registerUser_ShouldCreateNewUser() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setTelegramId(111222333L);

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.telegramId").value(111222333L))
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    @Test
    void registerUser_WhenUserExists_ShouldReturnExistingUser() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setTelegramId(111222333L);

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.telegramId").value(111222333L));
    }

    @Test
    void registerAppraiser_WhenInWhitelist_ShouldCreateAppraiser() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setTelegramId(123456789L);

        mockMvc.perform(post("/api/v1/users/register-appraiser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("APPRAISER"));
    }

    @Test
    void registerAppraiser_WhenNotInWhitelist_ShouldReturnBadRequest() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setTelegramId(999999999L);

        mockMvc.perform(post("/api/v1/users/register-appraiser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCurrentUser_WhenExists_ShouldReturnUser() throws Exception {
        Long telegramId = 111222333L;
        CreateUserRequest request = new CreateUserRequest();
        request.setTelegramId(telegramId);

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/users/me")
                        .param("telegramId", String.valueOf(telegramId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telegramId").value(telegramId));
    }

    @Test
    void getCurrentUser_WhenNotExists_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .param("telegramId", "999999999"))
                .andExpect(status().isNotFound());
    }
}

