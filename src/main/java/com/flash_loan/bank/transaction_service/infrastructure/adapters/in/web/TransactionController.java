package com.flash_loan.bank.transaction_service.infrastructure.adapters.in.web;

import com.flash_loan.bank.transaction_service.application.dto.TransactionRequest;
import com.flash_loan.bank.transaction_service.application.service.TransactionManagementService;
import com.flash_loan.bank.transaction_service.infrastructure.adapters.in.web.dto.request.TransactionRequestDto;
import com.flash_loan.bank.transaction_service.infrastructure.adapters.in.web.dto.response.TransactionResponseDto;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

  private final TransactionManagementService service;

  @PostMapping
  public Single<ResponseEntity<TransactionResponseDto>> executeTransaction(
          @Valid @RequestBody TransactionRequestDto dto) {
      
      TransactionRequest command = new TransactionRequest();
      command.setAccountId(dto.getAccountId());
      command.setAmount(dto.getAmount());
      command.setType(dto.getType());

      return service.executeTransaction(command)
              .map(this::mapToResponse)
              .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  @GetMapping
  public Flowable<TransactionResponseDto> getAllTransactions() {
      return service.findAll()
              .map(this::mapToResponse);
  }

  @GetMapping("/id/{id}")
  public Single<ResponseEntity<TransactionResponseDto>> getTransactionById(@PathVariable String id) {
      return service.findById(id)
              .map(this::mapToResponse)
              .map(ResponseEntity::ok);
  }

  @GetMapping("/{accountId}")
  public Flowable<TransactionResponseDto> getTransactionsByAccountId(@PathVariable String accountId) {
      return service.getTransactionsByAccountId(accountId)
              .map(this::mapToResponse);
  }

  @DeleteMapping("/{id}")
  public Single<ResponseEntity<Void>> deleteTransaction(@PathVariable String id) {
      return service.deleteTransaction(id)
              .toSingleDefault(ResponseEntity.status(HttpStatus.NO_CONTENT).<Void>build())
              .onErrorReturn(error -> ResponseEntity.notFound().build());
  }

  private TransactionResponseDto mapToResponse(com.flash_loan.bank.transaction_service.domain.model.Transaction tx) {
      return TransactionResponseDto.builder()
              .id(tx.getId())
              .accountId(tx.getAccountId())
              .amount(tx.getAmount())
              .feeApplied(tx.getFeeApplied())
              .type(tx.getType())
              .timestamp(tx.getTimestamp())
              .resultingBalance(tx.getResultingBalance())
              .status(tx.getStatus())
              .build();
  }
}
