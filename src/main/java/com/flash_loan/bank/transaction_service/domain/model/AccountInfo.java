package com.flash_loan.bank.transaction_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountInfo {
    private String id;
    private BigDecimal balance;
    private AccountType type;
    
    // Properties strictly for rule validation based on Hexagonal limits
    private Integer maxMonthlyMovements;
    private Integer currentMovements;
    private Integer allowedTransactionDay;
}
