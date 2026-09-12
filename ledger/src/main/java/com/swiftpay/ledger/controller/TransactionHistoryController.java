package com.swiftpay.ledger.controller;

import com.swiftpay.ledger.dto.TransactionHistoryResponse;
import com.swiftpay.ledger.entity.LedgerEntry;
import com.swiftpay.ledger.repository.LedgerEntryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/users")
@Tag(name = "Transactions", description = "User ledger / transaction history")
public class TransactionHistoryController {

    private final LedgerEntryRepository ledgerEntryRepository;

    public TransactionHistoryController(
            LedgerEntryRepository ledgerEntryRepository
    ) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @GetMapping("/{userId}/transactions")
    @Operation(
            summary = "Get transaction history for a user",
            description = """
                    Returns double-entry ledger rows (DEBIT / CREDIT) for the user, \
                    newest first. Entries appear after the Ledger Service settles a payment.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Ledger entries for the user",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(
                            schema = @Schema(implementation = TransactionHistoryResponse.class)
                    ),
                    examples = @ExampleObject(
                            value = """
                                    [
                                      {
                                        "transactionId": "txn-001",
                                        "type": "DEBIT",
                                        "amount": 250.00,
                                        "balanceAfter": 9750.00,
                                        "createdAt": "2026-09-12T21:36:00"
                                      }
                                    ]
                                    """
                    )
            )
    )
    public ResponseEntity<List<TransactionHistoryResponse>>
    getTransactionHistory(
            @Parameter(description = "User id", example = "1")
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
