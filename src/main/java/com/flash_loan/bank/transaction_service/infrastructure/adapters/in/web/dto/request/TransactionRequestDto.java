package com.flash_loan.bank.transaction_service.infrastructure.adapters.in.web.dto.request;

import com.flash_loan.bank.transaction_service.domain.model.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransactionRequestDto {
    @NotBlank(message = "accountId is required")
    private String accountId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "type is required")
    private TransactionType type;
}
