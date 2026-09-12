package com.swiftpay.ledger.controller;

import com.swiftpay.ledger.dto.TransactionHistoryResponse;
import com.swiftpay.ledger.entity.LedgerEntry;
import com.swiftpay.ledger.repository.LedgerEntryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/users")
public class TransactionHistoryController {

    private final LedgerEntryRepository ledgerEntryRepository;

    public TransactionHistoryController(
            LedgerEntryRepository ledgerEntryRepository
    ) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @GetMapping("/{userId}/transactions")
    public ResponseEntity<List<TransactionHistoryResponse>>
    getTransactionHistory(
            @PathVariable Long userId
    ) {

        List<LedgerEntry> entries =
                ledgerEntryRepository
                        .findByUserIdOrderByCreatedAtDesc(userId);

        List<TransactionHistoryResponse> response =
                entries.stream()
                        .map(entry ->
                                new TransactionHistoryResponse(
                                        entry.getTransactionId(),
                                        entry.getEntryType(),
                                        entry.getAmount(),
                                        entry.getBalanceAfter(),
                                        entry.getCreatedAt()
                                )
                        )
                        .toList();

        return ResponseEntity.ok(response);
    }
}
