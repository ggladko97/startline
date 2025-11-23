package com.yhladkevych.appraise.adapter.web.dto;

import com.yhladkevych.appraise.domain.model.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeOrderStatusRequest {
    @NotNull(message = "Status is required")
    private OrderStatus status;
}


