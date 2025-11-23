package com.yhladkevych.appraise.adapter.web.controller;

import com.yhladkevych.appraise.adapter.web.dto.CreateUserRequest;
import com.yhladkevych.appraise.adapter.web.dto.UserResponse;
import com.yhladkevych.appraise.application.service.UserService;
import com.yhladkevych.appraise.domain.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody CreateUserRequest request) {
        String traceId = MDC.get("traceId");
        try {
            User user = userService.createOrLoginUser(request.getTelegramId());
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
        } finally {
            if (traceId != null) {
                MDC.put("traceId", traceId);
            }
        }
    }

    @PostMapping("/register-appraiser")
    public ResponseEntity<UserResponse> registerAppraiser(@Valid @RequestBody CreateUserRequest request) {
        String traceId = MDC.get("traceId");
        try {
            User user = userService.createAppraiser(request.getTelegramId());
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
        } finally {
            if (traceId != null) {
                MDC.put("traceId", traceId);
            }
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@RequestParam Long telegramId) {
        String traceId = MDC.get("traceId");
        try {
            User user = userService.getUserByTelegramId(telegramId);
            return ResponseEntity.ok(toResponse(user));
        } finally {
            if (traceId != null) {
                MDC.put("traceId", traceId);
            }
        }
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .telegramId(user.getTelegramId())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}


