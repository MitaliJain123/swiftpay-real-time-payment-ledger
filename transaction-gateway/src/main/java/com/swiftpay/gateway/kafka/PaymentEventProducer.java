package com.swiftpay.gateway.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventProducer {

    private static final String PAYMENT_INITIATED_TOPIC =
            "payment-initiated";

    private final KafkaTemplate<String, PaymentInitiatedEvent> kafkaTemplate;

    public PaymentEventProducer(
            KafkaTemplate<String, PaymentInitiatedEvent> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaymentInitiated(
            PaymentInitiatedEvent event
    ) {
        kafkaTemplate.send(
                PAYMENT_INITIATED_TOPIC,
                event.getTransactionId(),
                event
        );
    }
}