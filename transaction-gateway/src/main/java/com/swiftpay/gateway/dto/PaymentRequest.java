package com.swiftpay.gateway.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class PaymentRequest {

    @NotBlank(message = "transactionId is required")
    private String transactionId;

    @NotNull(message = "senderId is required")
    private Long senderId;

    @NotNull(message = "receiverId is required")
    private Long receiverId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "currency is required")
    private String currency;

}