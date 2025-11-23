package com.yhladkevych.appraise.domain.model;

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
public class User {
    private Long id;
    private Long telegramId;
    private UserRole role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


