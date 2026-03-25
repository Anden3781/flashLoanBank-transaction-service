package com.flash_loan.bank.transaction_service.infrastructure.adapters.out.web.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountServiceErrorResponseDto {
    private String message;
}

