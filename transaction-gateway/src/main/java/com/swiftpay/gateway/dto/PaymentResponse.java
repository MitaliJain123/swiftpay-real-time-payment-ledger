package com.swiftpay.gateway.dto;

import com.swiftpay.gateway.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
@AllArgsConstructor
public class PaymentResponse {

    private String transactionId;
    private PaymentStatus status;
    private String message;

}