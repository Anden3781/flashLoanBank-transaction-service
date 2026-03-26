package com.flash_loan.bank.transaction_service.application.service;

import com.flash_loan.bank.transaction_service.application.dto.TransactionRequest;
import com.flash_loan.bank.transaction_service.domain.exception.AccountNotFoundException;
import com.flash_loan.bank.transaction_service.domain.exception.RuleViolationException;
import com.flash_loan.bank.transaction_service.domain.model.*;
import com.flash_loan.bank.transaction_service.domain.ports.out.AccountValidationPort;
import com.flash_loan.bank.transaction_service.domain.ports.out.TransactionRepositoryPort;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionManagementServiceTest {

    @Mock
    private AccountValidationPort accountPort;
    @Mock
    private TransactionRepositoryPort transactionRepository;

    @InjectMocks
    private TransactionManagementService service;

    private AccountInfo accountInfo;
    private TransactionRequest request;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        accountInfo = AccountInfo.builder()
                .id("acc-123")
                .type(AccountType.SAVINGS)
                .balance(BigDecimal.valueOf(1000))
                .maxMonthlyMovements(10)
                .currentMovements(0)
                .build();

        request = new TransactionRequest();
        request.setAccountId("acc-123");
        request.setAmount(BigDecimal.valueOf(100));
        request.setType(TransactionType.DEPOSIT);

        transaction = Transaction.builder()
                .id("tx-123")
                .accountId("acc-123")
                .amount(BigDecimal.valueOf(100))
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.PENDING)
                .build();
    }

    @Test
    void executeTransaction_Success_Deposit() {
        when(accountPort.getAccountById(anyString())).thenReturn(Maybe.just(accountInfo));
        when(transactionRepository.save(any())).thenReturn(Single.just(transaction));
        when(accountPort.applyCredit(anyString(), any(), anyString())).thenReturn(Single.just(accountInfo));

        Single<Transaction> result = service.executeTransaction(request);

        assertThat(result.blockingGet()).isNotNull();
        verify(accountPort).applyCredit(anyString(), any(), anyString());
    }

    @Test
    void executeTransaction_Error_AccountNotFound() {
        when(accountPort.getAccountById(anyString())).thenReturn(Maybe.empty());

        assertThatThrownBy(() -> service.executeTransaction(request).blockingGet())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Account does not exist");
    }

    @Test
    void executeTransaction_Error_InsufficientFunds() {
        accountInfo.setBalance(BigDecimal.ZERO);
        request.setType(TransactionType.WITHDRAWAL);
        when(accountPort.getAccountById(anyString())).thenReturn(Maybe.just(accountInfo));

        assertThatThrownBy(() -> service.executeTransaction(request).blockingGet())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    void getReportByDateRange_Success() {
        when(transactionRepository.findByAccountIdAndDateRange(anyString(), any(), any()))
                .thenReturn(Flowable.just(transaction));

        Flowable<Transaction> result = service.getReportByDateRange("acc-123", LocalDateTime.now(), LocalDateTime.now());

        assertThat(result.toList().blockingGet()).hasSize(1);
    }

    @Test
    void getLastTenMovements_Success() {
        when(transactionRepository.findByAccountId(anyString())).thenReturn(Flowable.just(transaction));

        Flowable<Transaction> result = service.getLastTenMovements("acc-123");

        assertThat(result.toList().blockingGet()).hasSize(1);
    }

    @Test
    void transfer_Success() {
        AccountInfo targetAccount = AccountInfo.builder()
                .id("acc-456")
                .type(AccountType.SAVINGS)
                .balance(BigDecimal.valueOf(1000))
                .maxMonthlyMovements(10)
                .currentMovements(0)
                .build();
        Transaction debitTx = transaction.toBuilder().status(TransactionStatus.SUCCESS).type(TransactionType.WITHDRAWAL).build();
        Transaction creditTx = transaction.toBuilder().status(TransactionStatus.SUCCESS).type(TransactionType.DEPOSIT).build();

        // Mocks para el debit (source)
        when(accountPort.getAccountById("acc-123")).thenReturn(Maybe.just(accountInfo));
        when(transactionRepository.save(any())).thenReturn(Single.just(debitTx));
        when(accountPort.applyDebit(anyString(), any(), anyString())).thenReturn(Single.just(accountInfo));

        // Mocks para el credit (target)
        when(accountPort.getAccountById("acc-456")).thenReturn(Maybe.just(targetAccount));
        // Aquí hay un detalle: el segundo save debe devolver el creditTx
        when(transactionRepository.save(any())).thenReturn(Single.just(debitTx)).thenReturn(Single.just(creditTx));
        when(accountPort.applyCredit(anyString(), any(), anyString())).thenReturn(Single.just(targetAccount));

        Single<Transaction> result = service.transfer("acc-123", "acc-456", BigDecimal.valueOf(100));

        assertThat(result.blockingGet()).isEqualTo(creditTx);
    }

    @Test
    void findById_Success() {
        when(transactionRepository.findById(anyString())).thenReturn(Maybe.just(transaction));

        Single<Transaction> result = service.findById("tx-123");

        assertThat(result.blockingGet()).isEqualTo(transaction);
    }

    @Test
    void deleteTransaction_Success() {
        when(transactionRepository.deleteById(anyString())).thenReturn(Single.just(true));

        service.deleteTransaction("tx-123").blockingAwait();

        verify(transactionRepository).deleteById("tx-123");
    }
}
