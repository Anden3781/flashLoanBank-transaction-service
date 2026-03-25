package com.flash_loan.bank.transaction_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private String id;
    private String accountId;
    private BigDecimal amount;
    private BigDecimal feeApplied;
    private TransactionType type;
    private LocalDateTime timestamp;
    private BigDecimal resultingBalance;
    private TransactionStatus status;
}
