package com.flash_loan.bank.transaction_service.infrastructure.adapters.in.web;

import com.flash_loan.bank.transaction_service.application.service.TransactionManagementService;
import com.flash_loan.bank.transaction_service.domain.model.Transaction;
import com.flash_loan.bank.transaction_service.domain.model.TransactionStatus;
import com.flash_loan.bank.transaction_service.domain.model.TransactionType;
import com.flash_loan.bank.transaction_service.infrastructure.adapters.in.web.dto.request.TransactionRequestDto;
import com.flash_loan.bank.transaction_service.infrastructure.adapters.in.web.dto.request.TransferRequestDto;
import com.flash_loan.bank.transaction_service.infrastructure.adapters.in.web.dto.response.TransactionResponseDto;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionManagementService service;

    @InjectMocks
    private TransactionController controller;

    private Transaction tx;

    @BeforeEach
    void setUp() {
        tx = Transaction.builder()
                .id("tx-1")
                .accountId("acc-1")
                .amount(BigDecimal.TEN)
                .type(TransactionType.DEPOSIT)
                .timestamp(LocalDateTime.now())
                .resultingBalance(new BigDecimal("110"))
                .status(TransactionStatus.SUCCESS)
                .build();
    }

    @Test
    void executeTransaction_ShouldReturnCreated() {
        TransactionRequestDto dto = new TransactionRequestDto();
        dto.setAccountId("acc-1");
        dto.setAmount(BigDecimal.TEN);
        dto.setType(TransactionType.DEPOSIT);
        when(service.executeTransaction(any())).thenReturn(Single.just(tx));

        ResponseEntity<TransactionResponseDto> response = controller.executeTransaction(dto).blockingGet();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isEqualTo("tx-1");
    }

    @Test
    void getAllTransactions_ShouldReturnFlowable() {
        when(service.findAll()).thenReturn(Flowable.just(tx));
        assertThat(controller.getAllTransactions().toList().blockingGet()).hasSize(1);
    }

    @Test
    void getTransactionById_ShouldReturnOk() {
        when(service.findById(anyString())).thenReturn(Single.just(tx));
        ResponseEntity<TransactionResponseDto> response = controller.getTransactionById("tx-1").blockingGet();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getTransactionsByAccountId_ShouldReturnFlowable() {
        when(service.getTransactionsByAccountId(anyString())).thenReturn(Flowable.just(tx));
        assertThat(controller.getTransactionsByAccountId("acc-1").toList().blockingGet()).hasSize(1);
    }

    @Test
    void getReportByDateRange_ShouldReturnFlowable() {
        when(service.getReportByDateRange(anyString(), any(), any())).thenReturn(Flowable.just(tx));
        assertThat(controller.getReportByDateRange("acc-1", LocalDateTime.now().minusDays(1), LocalDateTime.now())
                .toList().blockingGet()).hasSize(1);
    }

    @Test
    void getLastTenMovements_ShouldReturnFlowable() {
        when(service.getLastTenMovements(anyString())).thenReturn(Flowable.just(tx));
        assertThat(controller.getLastTenMovements("acc-1").toList().blockingGet()).hasSize(1);
    }

    @Test
    void transfer_ShouldReturnOk() {
        TransferRequestDto dto = new TransferRequestDto();
        dto.setSourceAccountId("acc-1");
        dto.setTargetAccountId("acc-2");
        dto.setAmount(BigDecimal.TEN);
        when(service.transfer(anyString(), anyString(), any())).thenReturn(Single.just(tx));
        ResponseEntity<TransactionResponseDto> response = controller.transfer(dto).blockingGet();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteTransaction_NotFound_ShouldReturn404() {
        when(service.deleteTransaction(anyString())).thenReturn(io.reactivex.rxjava3.core.Completable.error(new RuntimeException("not found")));
        ResponseEntity<Void> response = controller.deleteTransaction("tx-1").blockingGet();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
