package com.swiftpay.ledger.service;

import com.swiftpay.ledger.entity.LedgerEntry;
import com.swiftpay.ledger.entity.LedgerEntryType;
import com.swiftpay.ledger.entity.Payment;
import com.swiftpay.ledger.entity.PaymentStatus;
import com.swiftpay.ledger.entity.User;
import com.swiftpay.ledger.kafka.PaymentCompletedEvent;
import com.swiftpay.ledger.kafka.PaymentEventProducer;
import com.swiftpay.ledger.kafka.PaymentFailedEvent;
import com.swiftpay.ledger.kafka.PaymentInitiatedEvent;
import com.swiftpay.ledger.repository.LedgerEntryRepository;
import com.swiftpay.ledger.repository.PaymentRepository;
import com.swiftpay.ledger.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class LedgerService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PaymentEventProducer eventProducer;

    public LedgerService(
            UserRepository userRepository,
            PaymentRepository paymentRepository,
            LedgerEntryRepository ledgerEntryRepository,
            PaymentEventProducer eventProducer
    ) {
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public void processPayment(
            PaymentInitiatedEvent event
    ) {

        Payment payment = paymentRepository
                .findByTransactionId(
                        event.getTransactionId()
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Payment not found: "
                                        + event.getTransactionId()
                        )
                );

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return;
        }

        // If already failed, don't process it again
        if (payment.getStatus() == PaymentStatus.FAILED) {
            return;
        }
        // Always lock users in ascending id order to avoid deadlocks
        // when two opposite payments (A->B and B->A) run concurrently.
        Long senderId = event.getSenderId();
        Long receiverId = event.getReceiverId();

        Long firstLockId = senderId < receiverId ? senderId : receiverId;
        Long secondLockId = senderId < receiverId ? receiverId : senderId;

        User firstLocked = lockUser(firstLockId);
        User secondLocked = lockUser(secondLockId);

        User sender = firstLocked.getId().equals(senderId)
                ? firstLocked
                : secondLocked;

        User receiver = firstLocked.getId().equals(senderId)
                ? secondLocked
                : firstLocked;

        if (!sender.getCurrency()
                .equalsIgnoreCase(event.getCurrency())) {

            markPaymentFailed(
                    payment,
                    event,
                    "Sender currency mismatch"
            );

            return;
        }

        if (!receiver.getCurrency()
                .equalsIgnoreCase(event.getCurrency())) {

            markPaymentFailed(
                    payment,
                    event,
                    "Receiver currency mismatch"
            );

            return;
        }
        if (sender.getBalance()
                .compareTo(event.getAmount()) < 0) {

            markPaymentFailed(
                    payment,
                    event,
                    "Insufficient funds"
            );

            return;
        }

        BigDecimal senderNewBalance =
                sender.getBalance()
                        .subtract(event.getAmount());

        BigDecimal receiverNewBalance =
                receiver.getBalance()
                        .add(event.getAmount());

        sender.setBalance(senderNewBalance);

        receiver.setBalance(receiverNewBalance);

        userRepository.save(sender);
        userRepository.save(receiver);

        LedgerEntry debitEntry = new LedgerEntry();

        debitEntry.setTransactionId(
                event.getTransactionId()
        );

        debitEntry.setUserId(
                sender.getId()
        );

        debitEntry.setEntryType(
                LedgerEntryType.DEBIT
        );

        debitEntry.setAmount(
                event.getAmount()
        );

        debitEntry.setBalanceAfter(
                senderNewBalance
        );

        ledgerEntryRepository.save(debitEntry);

        LedgerEntry creditEntry = new LedgerEntry();

        creditEntry.setTransactionId(
                event.getTransactionId()
        );

        creditEntry.setUserId(
                receiver.getId()
        );

        creditEntry.setEntryType(
                LedgerEntryType.CREDIT
        );

        creditEntry.setAmount(
                event.getAmount()
        );

        creditEntry.setBalanceAfter(
                receiverNewBalance
        );

        ledgerEntryRepository.save(creditEntry);

        payment.setStatus(
                PaymentStatus.COMPLETED
        );

        paymentRepository.save(payment);

        PaymentCompletedEvent completedEvent =
                new PaymentCompletedEvent(
                        event.getTransactionId(),
                        event.getSenderId(),
                        event.getReceiverId(),
                        event.getAmount(),
                        event.getCurrency(),
                        "COMPLETED"
                );

        eventProducer.publishPaymentCompleted(
                completedEvent
        );
    }

    private User lockUser(Long userId) {

        return userRepository
                .findByIdForUpdate(userId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "User not found: " + userId
                        )
                );
    }

    private void markPaymentFailed(
            Payment payment,
            PaymentInitiatedEvent event,
            String reason
    ) {

        payment.setStatus(
                PaymentStatus.FAILED
        );

        paymentRepository.save(payment);

        PaymentFailedEvent failedEvent =
                new PaymentFailedEvent(
                        event.getTransactionId(),
                        event.getSenderId(),
                        event.getReceiverId(),
                        event.getAmount(),
                        event.getCurrency(),
                        reason
                );

        eventProducer.publishPaymentFailed(
                failedEvent
        );
    }
}
