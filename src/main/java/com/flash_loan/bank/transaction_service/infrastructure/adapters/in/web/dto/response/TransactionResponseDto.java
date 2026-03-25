package com.flash_loan.bank.transaction_service.infrastructure.adapters.in.web.dto.response;

import com.flash_loan.bank.transaction_service.domain.model.TransactionType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponseDto {
    private String id;
    private String accountId;
    private BigDecimal amount;
    private BigDecimal feeApplied;
    private TransactionType type;
    private LocalDateTime timestamp;
    private BigDecimal resultingBalance;
}
