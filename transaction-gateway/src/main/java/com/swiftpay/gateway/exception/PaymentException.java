package com.swiftpay.gateway.exception;

public class PaymentException extends RuntimeException {

    public PaymentException(String message) {
        super(message);
    }
}