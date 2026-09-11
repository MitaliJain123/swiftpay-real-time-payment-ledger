package com.swiftpay.gateway.service;

import com.swiftpay.gateway.dto.PaymentRequest;
import com.swiftpay.gateway.dto.PaymentResponse;
import com.swiftpay.gateway.entity.Payment;
import com.swiftpay.gateway.entity.PaymentStatus;
import com.swiftpay.gateway.entity.User;
import com.swiftpay.gateway.exception.InsufficientFundsException;
import com.swiftpay.gateway.exception.PaymentException;
import com.swiftpay.gateway.kafka.PaymentEventProducer;
import com.swiftpay.gateway.kafka.PaymentInitiatedEvent;
import com.swiftpay.gateway.redis.IdempotencyService;
import com.swiftpay.gateway.repository.PaymentRepository;
import com.swiftpay.gateway.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final IdempotencyService idempotencyService;
    private final PaymentEventProducer eventProducer;

    public PaymentService(
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            IdempotencyService idempotencyService,
            PaymentEventProducer eventProducer
    ) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.idempotencyService = idempotencyService;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public PaymentResponse initiatePayment(
            PaymentRequest request
    ) {

        // 1. Validate sender != receiver
        if (request.getSenderId()
                .equals(request.getReceiverId())) {

            throw new PaymentException(
                    "Sender and receiver cannot be the same"
            );
        }

        // 2. Check Redis idempotency
        if (idempotencyService.exists(
                request.getTransactionId())) {

            Payment existingPayment =
                    paymentRepository
                            .findByTransactionId(
                                    request.getTransactionId()
                            )
                            .orElseThrow(() ->
                                    new PaymentException(
                                            "Duplicate transaction"
                                    )
                            );

            return new PaymentResponse(
                    existingPayment.getTransactionId(),
                    existingPayment.getStatus(),
                    "Transaction already processed"
            );
        }

        // 3. Validate sender
        User sender = userRepository
                .findById(request.getSenderId())
                .orElseThrow(() ->
                        new PaymentException(
                                "Sender not found"
                        )
                );

        // 4. Validate receiver
        userRepository
                .findById(request.getReceiverId())
                .orElseThrow(() ->
                        new PaymentException(
                                "Receiver not found"
                        )
                );

        // 5. Check balance
        if (sender.getBalance()
                .compareTo(request.getAmount()) < 0) {

            throw new InsufficientFundsException(
                    "Insufficient funds"
            );
        }

        // 6. Create PENDING payment
        Payment payment = new Payment();

        payment.setTransactionId(
                request.getTransactionId()
        );

        payment.setSenderId(
                request.getSenderId()
        );

        payment.setReceiverId(
                request.getReceiverId()
        );

        payment.setAmount(
                request.getAmount()
        );

        payment.setCurrency(
                request.getCurrency()
        );

        payment.setStatus(
                PaymentStatus.PENDING
        );

        paymentRepository.save(payment);

        // 7. Mark transaction as processed in Redis
        idempotencyService.save(
                request.getTransactionId()
        );

        // 8. Publish Kafka event
        PaymentInitiatedEvent event =
                new PaymentInitiatedEvent(
                        request.getTransactionId(),
                        request.getSenderId(),
                        request.getReceiverId(),
                        request.getAmount(),
                        request.getCurrency()
                );

        eventProducer.publishPaymentInitiated(event);

        // 9. Return response
        return new PaymentResponse(
                payment.getTransactionId(),
                PaymentStatus.PENDING,
                "Payment initiated successfully"
        );
    }
}