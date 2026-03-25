package com.flash_loan.bank.transaction_service.application.dto;

import com.flash_loan.bank.transaction_service.domain.model.TransactionType;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransactionRequest {
    private String accountId;
    private BigDecimal amount;
    private TransactionType type;
}
