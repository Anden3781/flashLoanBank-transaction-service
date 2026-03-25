package com.flash_loan.bank.transaction_service.infrastructure.adapters.out.web.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceOperationRequestDto {
    private BigDecimal amount;
    private String transactionId;
}

