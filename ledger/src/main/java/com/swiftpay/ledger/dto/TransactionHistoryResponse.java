package com.swiftpay.ledger.dto;

import com.swiftpay.ledger.entity.LedgerEntryType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class TransactionHistoryResponse {

    private String transactionId;
    private LedgerEntryType type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private LocalDateTime createdAt;

    public TransactionHistoryResponse(
            String transactionId,
            LedgerEntryType type,
            BigDecimal amount,
            BigDecimal balanceAfter,
            LocalDateTime createdAt
    ) {
        this.transactionId = transactionId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.createdAt = createdAt;
    }

}
