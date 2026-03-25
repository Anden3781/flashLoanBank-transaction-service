package com.flash_loan.bank.transaction_service.application.service;

import com.flash_loan.bank.transaction_service.application.dto.TransferRequest;
import com.flash_loan.bank.transaction_service.application.dto.TransferResponse;
import com.flash_loan.bank.transaction_service.domain.exception.AccountNotFoundException;
import com.flash_loan.bank.transaction_service.domain.exception.RuleViolationException;
import com.flash_loan.bank.transaction_service.domain.model.AccountInfo;
import com.flash_loan.bank.transaction_service.domain.model.Transaction;
import com.flash_loan.bank.transaction_service.domain.model.TransactionStatus;
import com.flash_loan.bank.transaction_service.domain.model.TransactionType;
import com.flash_loan.bank.transaction_service.domain.ports.out.AccountValidationPort;
import com.flash_loan.bank.transaction_service.domain.ports.out.TransactionRepositoryPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Application service responsible for orchestrating bank transfers between accounts.
 * Implements a reactive Saga pattern: validate both accounts in parallel, debit source,
 * credit target, record both legs. Protected by Resilience4j circuit breaker.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountValidationPort accountPort;
    private final TransactionRepositoryPort transactionRepository;

    /**
     * Executes a transfer between two accounts.
     * Steps: validate both accounts in parallel → check funds → debit source → credit target.
     *
     * @param request command with sourceAccountId, targetAccountId, and amount
     * @return a {@link TransferResponse} with both transaction records
     */
    @CircuitBreaker(name = "transactionService", fallbackMethod = "fallbackTransfer")
    public Single<TransferResponse> executeTransfer(TransferRequest request) {
        String transferId = UUID.randomUUID().toString();

        Single<AccountInfo> sourceSingle = resolveAccount(request.getSourceAccountId(), "Source");
        Single<AccountInfo> targetSingle = resolveAccount(request.getTargetAccountId(), "Target");

        return Single.zip(sourceSingle, targetSingle, this::validateAndPair)
                .flatMap(pair -> executeDebit(pair[0], request.getAmount(), transferId)
                        .flatMap(debitTx -> executeCredit(pair[1], request.getAmount(), transferId)
                                .onErrorResumeNext(err -> compensate(pair[0], request.getAmount(), transferId, err))
                                .map(creditTx -> TransferResponse.builder()
                                        .sourceTransaction(debitTx)
                                        .targetTransaction(creditTx)
                                        .build())));
    }

    /** Fallback invoked when circuit breaker is open or a timeout occurs. */
    public Single<TransferResponse> fallbackTransfer(TransferRequest request, Throwable t) {
        log.error("Circuit breaker active for transfer {}->{}: {}",
                request.getSourceAccountId(), request.getTargetAccountId(), t.getMessage());
        return Single.error(new RuleViolationException(
                "Servicio temporalmente no disponible, reintente en unos segundos"));
    }

    /** Fetches an account and wraps absence in a business exception. */
    private Single<AccountInfo> resolveAccount(String accountId, String role) {
        return accountPort.getAccountById(accountId)
                .switchIfEmpty(Single.error(
                        new AccountNotFoundException(role + " account not found: " + accountId)));
    }

    /**
     * Validates sufficient funds and returns both accounts as an array for zip chaining.
     * Throws {@link RuleViolationException} if balance is insufficient.
     */
    /**
     * Pairs source and target accounts for zip chaining.
     * Actual balance validation is deferred to the debit step where the amount is available.
     */
    private AccountInfo[] validateAndPair(AccountInfo source, AccountInfo target) {
        return new AccountInfo[]{source, target};
    }

    /** Debits the source account and persists the TRANSFER_OUT record. */
    private Single<Transaction> executeDebit(AccountInfo source, BigDecimal amount, String transferId) {
        BigDecimal newBalance = source.getBalance().subtract(amount);
        boolean insufficient = newBalance.compareTo(BigDecimal.ZERO) < 0;
        return Single.just(insufficient)
                .flatMap(isInsufficient -> isInsufficient
                        ? Single.error(new RuleViolationException(
                                "Insufficient funds. Available: " + source.getBalance() + ", Requested: " + amount))
                        : accountPort.updateAccountBalance(source.getId(), newBalance))
                .flatMap(success -> transactionRepository.save(buildTx(
                        source.getId(), transferId, amount, TransactionType.TRANSFER_OUT, newBalance)));
    }

    /** Credits the target account and persists the TRANSFER_IN record. */
    private Single<Transaction> executeCredit(AccountInfo target, BigDecimal amount, String transferId) {
        BigDecimal newBalance = target.getBalance().add(amount);
        return accountPort.updateAccountBalance(target.getId(), newBalance)
                .flatMap(success -> transactionRepository.save(buildTx(
                        target.getId(), transferId, amount, TransactionType.TRANSFER_IN, newBalance)));
    }

    /**
     * Compensating action: reverts the debit if the credit leg fails.
     * Marks the source transaction as FAILED for audit traceability.
     */
    private Single<Transaction> compensate(AccountInfo source, BigDecimal amount,
                                            String transferId, Throwable error) {
        log.error("Credit leg failed for transfer {}. Initiating compensating reversal.", transferId, error);
        return accountPort.updateAccountBalance(source.getId(), source.getBalance())
                .flatMap(ok -> transactionRepository.save(buildTx(
                        source.getId(), transferId, amount, TransactionType.TRANSFER_OUT, source.getBalance())
                        .toBuilder().status(TransactionStatus.FAILED).build()))
                .flatMap(saved -> Single.error(new RuntimeException(
                        "Transfer " + transferId + " FAILED. Compensating reversal applied. Cause: "
                                + error.getMessage())));
    }

    /** Builds a successful transaction record. */
    private Transaction buildTx(String accountId, String transferId, BigDecimal amount,
                                  TransactionType type, BigDecimal resultingBalance) {
        return Transaction.builder()
                .accountId(accountId)
                .transferId(transferId)
                .amount(amount)
                .feeApplied(BigDecimal.ZERO)
                .type(type)
                .timestamp(LocalDateTime.now())
                .resultingBalance(resultingBalance)
                .status(TransactionStatus.SUCCESS)
                .build();
    }
}
