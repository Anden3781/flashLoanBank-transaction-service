package com.flash_loan.bank.transaction_service.application.service;

import com.flash_loan.bank.transaction_service.application.dto.TransactionRequest;
import com.flash_loan.bank.transaction_service.domain.exception.AccountNotFoundException;
import com.flash_loan.bank.transaction_service.domain.exception.RuleViolationException;
import com.flash_loan.bank.transaction_service.domain.model.AccountInfo;
import com.flash_loan.bank.transaction_service.domain.model.AccountType;
import com.flash_loan.bank.transaction_service.domain.model.Transaction;
import com.flash_loan.bank.transaction_service.domain.model.TransactionStatus;
import com.flash_loan.bank.transaction_service.domain.model.TransactionType;
import com.flash_loan.bank.transaction_service.domain.ports.out.AccountValidationPort;
import com.flash_loan.bank.transaction_service.domain.ports.out.TransactionRepositoryPort;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionManagementService {

    private final AccountValidationPort accountPort;
    private final TransactionRepositoryPort transactionRepository;

    private static final BigDecimal CHECKING_FEE = new BigDecimal("2.50");
    private static final BigDecimal SAVINGS_LIMIT_FEE = new BigDecimal("5.00");

    @CircuitBreaker(name = "transactionService", fallbackMethod = "fallbackExecuteTransaction")
    @TimeLimiter(name = "transactionService")
    public Single<Transaction> executeTransaction(TransactionRequest command) {
        return accountPort.getAccountById(command.getAccountId())
                .switchIfEmpty(Maybe.error(new AccountNotFoundException("Account does not exist: " + command.getAccountId())))
                .flatMap(this::validateFixedTerm)
                .flatMap(account -> createTransaction(account, command))
                .flatMapSingle(transactionRepository::save)
                .flatMapSingle(this::processRemoteUpdate)
                .toSingle();
    }

    public Single<Transaction> fallbackExecuteTransaction(TransactionRequest command, Throwable t) {
        log.error("Circuit breaker active for executeTransaction: {}", t.getMessage());
        return Single.error(new RuleViolationException("Servicio temporalmente no disponible, reintente en unos segundos"));
    }

    private Maybe<AccountInfo> validateFixedTerm(AccountInfo account) {
        return Maybe.just(account)
                .filter(acc -> acc.getType() != AccountType.FIXED_TERM || LocalDateTime.now().getDayOfMonth() == acc.getAllowedTransactionDay())
                .switchIfEmpty(Maybe.error(new RuleViolationException("Fixed term accounts can only operate on day " + account.getAllowedTransactionDay())));
    }

    private Maybe<Transaction> createTransaction(AccountInfo account, TransactionRequest command) {
        BigDecimal fee = calculateFee(account);
        BigDecimal impact = command.getType() == TransactionType.DEPOSIT
                ? command.getAmount().subtract(fee)
                : command.getAmount().negate().subtract(fee);

        BigDecimal newBalance = account.getBalance().add(impact);

        return Maybe.just(newBalance)
                .filter(balance -> balance.compareTo(BigDecimal.ZERO) >= 0)
                .switchIfEmpty(Maybe.error(new RuleViolationException("Insufficient funds after applying fees (Resulting Balance: " + newBalance + ")")))
                .map(balance -> Transaction.builder()
                        .accountId(command.getAccountId())
                        .amount(command.getAmount())
                        .feeApplied(fee)
                        .type(command.getType())
                        .timestamp(LocalDateTime.now())
                        .resultingBalance(balance)
                        .status(TransactionStatus.PENDING)
                        .build());
    }

    private Single<Transaction> processRemoteUpdate(Transaction savedTx) {
        return accountPort.updateAccountBalance(savedTx.getAccountId(), savedTx.getResultingBalance())
                .filter(success -> success)
                .flatMap(success -> transactionRepository.save(savedTx.toBuilder().status(TransactionStatus.SUCCESS).build()).toMaybe())
                .switchIfEmpty(markAsFailed(savedTx, "Remote balance update rejected by account-service").toMaybe())
                .toSingle()
                .onErrorResumeNext(error -> {
                    log.error("Distributed Transaction Failed. Executing Compensating Action for Transaction: {}", savedTx.getId(), error);
                    return markAsFailed(savedTx, error.getMessage());
                });
    }

    public Flowable<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    public Single<Transaction> findById(String id) {
        return transactionRepository.findById(id)
                .switchIfEmpty(Maybe.error(new RuleViolationException("Transaction not found: " + id)))
                .toSingle();
    }

    public Flowable<Transaction> getTransactionsByAccountId(String accountId) {
        return transactionRepository.findByAccountId(accountId);
    }

    public Completable deleteTransaction(String id) {
        return transactionRepository.deleteById(id)
                .filter(success -> success)
                .switchIfEmpty(Maybe.error(new RuleViolationException("Transaction not found for deletion: " + id)))
                .ignoreElement();
    }

    private BigDecimal calculateFee(AccountInfo account) {
        return java.util.Optional.ofNullable(account.getType())
                .filter(type -> type == AccountType.CHECKING)
                .map(unused -> CHECKING_FEE)
                .or(() -> java.util.Optional.ofNullable(account.getType())
                        .filter(type -> type == AccountType.SAVINGS)
                        .filter(unused -> account.getCurrentMovements() != null)
                        .filter(unused -> account.getCurrentMovements() >= account.getMaxMonthlyMovements())
                        .map(unused -> SAVINGS_LIMIT_FEE))
                .orElse(BigDecimal.ZERO);
    }

    private Single<Transaction> markAsFailed(Transaction tx, String reason) {
        return transactionRepository.save(tx.toBuilder().status(TransactionStatus.FAILED).build())
                .flatMap(failedTx -> Single.error(new RuntimeException("Distributed Transaction Failed. Local record marked as FAILED. Reason: " + reason)));
    }
}
