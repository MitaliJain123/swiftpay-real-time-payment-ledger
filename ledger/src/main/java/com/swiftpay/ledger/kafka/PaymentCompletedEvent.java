package com.swiftpay.ledger.kafka;

import java.math.BigDecimal;

public class PaymentCompletedEvent {

    private String transactionId;
    private Long senderId;
    private Long receiverId;
    private BigDecimal amount;
    private String currency;
    private String status;

    public PaymentCompletedEvent() {
    }

    public PaymentCompletedEvent(
            String transactionId,
            Long senderId,
            Long receiverId,
            BigDecimal amount,
            String currency,
            String status
    ) {
        this.transactionId = transactionId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
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

    public String getStatus() {
        return status;
    }
}
