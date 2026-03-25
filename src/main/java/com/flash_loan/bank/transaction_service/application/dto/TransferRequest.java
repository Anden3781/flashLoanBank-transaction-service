package com.flash_loan.bank.transaction_service.application.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * Command object for initiating a bank transfer between two accounts.
 */
@Data
public class TransferRequest {
    /** Account from which the amount will be debited. */
    private String sourceAccountId;
    /** Account to which the amount will be credited. */
    private String targetAccountId;
    /** Amount to transfer (must be positive). */
    private BigDecimal amount;
}
