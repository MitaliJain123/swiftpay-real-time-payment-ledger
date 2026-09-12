package com.swiftpay.ledger.consumer;

import com.swiftpay.ledger.kafka.PaymentInitiatedEvent;
import com.swiftpay.ledger.service.LedgerService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private final LedgerService ledgerService;

    public PaymentEventConsumer(
            LedgerService ledgerService
    ) {
        this.ledgerService = ledgerService;
    }

    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    @KafkaListener(
            topics = "payment-initiated",
            groupId = "ledger-service"
    )
    public void consumePaymentInitiated(
            PaymentInitiatedEvent event
    ) {

        ledgerService.processPayment(event);
    }
}
