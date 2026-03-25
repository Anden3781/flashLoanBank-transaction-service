package com.flash_loan.bank.transaction_service.infrastructure.adapters.in.web;

import com.flash_loan.bank.transaction_service.application.dto.TransactionRequest;
import com.flash_loan.bank.transaction_service.application.service.TransactionManagementService;
import com.flash_loan.bank.transaction_service.infrastructure.adapters.in.web.dto.request.TransactionRequestDto;
import com.flash_loan.bank.transaction_service.infrastructure.adapters.in.web.dto.response.TransactionResponseDto;
import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

  private final TransactionManagementService service;

  @PostMapping
  public Single<ResponseEntity<TransactionResponseDto>> executeTransaction(
          @Valid @RequestBody TransactionRequestDto dto) {
      
      // Map DTO to Application Command
      TransactionRequest command = new TransactionRequest();
      command.setAccountId(dto.getAccountId());
      command.setAmount(dto.getAmount());
      command.setType(dto.getType());

      return service.executeTransaction(command)
              .map(tx -> TransactionResponseDto.builder()
                      .id(tx.getId())
                      .accountId(tx.getAccountId())
                      .amount(tx.getAmount())
                      .feeApplied(tx.getFeeApplied())
                      .type(tx.getType())
                      .timestamp(tx.getTimestamp())
                      .resultingBalance(tx.getResultingBalance())
                      .build())
              .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }
}
