package com.swiftpay.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Payment initiation request")
public class PaymentRequest {

    @NotBlank(message = "transactionId is required")
    @Schema(description = "Client-generated unique id (idempotency key)", example = "txn-001")
    private String transactionId;

    @NotNull(message = "senderId is required")
    @Schema(description = "Sender user id", example = "1")
    private Long senderId;

    @NotNull(message = "receiverId is required")
    @Schema(description = "Receiver user id", example = "2")
    private Long receiverId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than zero")
    @Schema(description = "Amount to transfer", example = "250.00")
    private BigDecimal amount;

    @NotBlank(message = "currency is required")
    @Schema(description = "ISO currency code (must match both users)", example = "INR")
    private String currency;

}
