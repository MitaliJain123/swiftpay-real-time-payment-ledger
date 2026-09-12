package com.swiftpay.ledger.repository;

import com.swiftpay.ledger.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionId(String transactionId);
}
