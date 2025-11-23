package com.yhladkevych.appraise.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    @NotBlank(message = "Car ad URL is required")
    private String carAdUrl;

    @NotBlank(message = "Car location is required")
    private String carLocation;

    @NotNull(message = "Car price is required")
    private BigDecimal carPrice;
}


