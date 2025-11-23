package com.yhladkevych.appraise.adapter.web.dto;

import com.yhladkevych.appraise.domain.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private Long telegramId;
    private UserRole role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
