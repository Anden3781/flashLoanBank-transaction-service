package com.flash_loan.bank.transaction_service.application.dto;

import com.flash_loan.bank.transaction_service.domain.model.Transaction;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for a completed bank transfer, containing both leg records.
 */
@Data
@Builder
public class TransferResponse {
    /** The debit record on the source account. */
    private Transaction sourceTransaction;
    /** The credit record on the target account. */
    private Transaction targetTransaction;
}
