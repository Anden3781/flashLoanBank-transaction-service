package com.flash_loan.bank.transaction_service.infrastructure.adapters.out.persistence.entity;

import com.flash_loan.bank.transaction_service.domain.model.TransactionStatus;
import com.flash_loan.bank.transaction_service.domain.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transactions")
public class TransactionEntity {
    @Id
    private String id;
    private String accountId;
    private BigDecimal amount;
    private BigDecimal feeApplied;
    private TransactionType type;
    private LocalDateTime timestamp;
    private BigDecimal resultingBalance;
    private TransactionStatus status;
}
