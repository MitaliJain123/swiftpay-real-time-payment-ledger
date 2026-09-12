package com.swiftpay.ledger.kafka;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class PaymentFailedEvent {

    private String transactionId;
    private Long senderId;
    private Long receiverId;
    private BigDecimal amount;
    private String currency;
    private String reason;

    public PaymentFailedEvent() {
    }

    public PaymentFailedEvent(
            String transactionId,
            Long senderId,
            Long receiverId,
            BigDecimal amount,
            String currency,
            String reason
    ) {
        this.transactionId = transactionId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amount = amount;
        this.currency = currency;
        this.reason = reason;
    }
}
