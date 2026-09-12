package com.swiftpay.ledger.kafka;

import java.math.BigDecimal;

public class PaymentInitiatedEvent {

    private String transactionId;
    private Long senderId;
    private Long receiverId;
    private BigDecimal amount;
    private String currency;

    public PaymentInitiatedEvent() {
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }
}
