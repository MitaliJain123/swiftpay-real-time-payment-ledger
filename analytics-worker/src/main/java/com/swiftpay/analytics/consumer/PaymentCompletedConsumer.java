package com.swiftpay.analytics.consumer;

import com.swiftpay.analytics.kafka.PaymentCompletedEvent;
import com.swiftpay.analytics.service.AnalyticsService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentCompletedConsumer {

    private final AnalyticsService analyticsService;

    public PaymentCompletedConsumer(
            AnalyticsService analyticsService
    ) {
        this.analyticsService = analyticsService;
    }

    @KafkaListener(
            topics = "payment-completed",
            groupId = "analytics-worker"
    )
    public void consumePaymentCompleted(
            PaymentCompletedEvent event
    ) {
        analyticsService.recordPayment(event);
    }
}
