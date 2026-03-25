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

    public Single<Transaction> executeTransaction(TransactionRequest command) {
        return accountPort.getAccountById(command.getAccountId())
                .switchIfEmpty(Maybe.error(new AccountNotFoundException("Account does not exist: " + command.getAccountId())))
                .toSingle()
                .flatMap(account -> {
                    if (account.getType() == AccountType.FIXED_TERM) {
                        int expectedDay = account.getAllowedTransactionDay();
                        int currentDay = LocalDateTime.now().getDayOfMonth();
                        if (currentDay != expectedDay) {
                            return Single.error(new RuleViolationException("Fixed term accounts can only operate on day " + expectedDay));
                        }
                    }
                    return Single.just(account);
                })
                .flatMap(account -> {
                    BigDecimal fee = calculateFee(account);
                    BigDecimal impact = command.getType() == TransactionType.DEPOSIT
                            ? command.getAmount().subtract(fee)
                            : command.getAmount().negate().subtract(fee);

                    BigDecimal newBalance = account.getBalance().add(impact);

                    if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                        return Single.error(new RuleViolationException("Insufficient funds after applying fees (Resulting Balance: " + newBalance + ")"));
                    }

                    Transaction tx = Transaction.builder()
                            .accountId(command.getAccountId())
                            .amount(command.getAmount())
                            .feeApplied(fee)
                            .type(command.getType())
                            .timestamp(LocalDateTime.now())
                            .resultingBalance(newBalance)
                            .status(TransactionStatus.PENDING)
                            .build();

                    return Single.just(tx);
                })
                .flatMap(transactionRepository::save)
                .flatMap(savedTx -> accountPort.updateAccountBalance(savedTx.getAccountId(), savedTx.getResultingBalance())
                        .flatMap(success -> {
                            if (!success) {
                                return markAsFailed(savedTx, "Remote balance update rejected by account-service");
                            }
                            return transactionRepository.save(savedTx.toBuilder().status(TransactionStatus.SUCCESS).build());
                        })
                        .onErrorResumeNext(error -> {
                            log.error("Distributed Transaction Failed. Executing Compensating Action for Transaction: {}", savedTx.getId(), error);
                            return markAsFailed(savedTx, error.getMessage());
                        })
                );
    }

    public Flowable<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    public Single<Transaction> findById(String id) {
        return transactionRepository.findById(id)
                .switchIfEmpty(Single.error(new RuleViolationException("Transaction not found: " + id)));
    }

    public Flowable<Transaction> getTransactionsByAccountId(String accountId) {
        return transactionRepository.findByAccountId(accountId);
    }

    public Completable deleteTransaction(String id) {
        return transactionRepository.deleteById(id)
                .flatMapCompletable(success -> {
                    if (!success) {
                        return Completable.error(new RuleViolationException("Transaction not found for deletion: " + id));
                    }
                    return Completable.complete();
                });
    }

    private BigDecimal calculateFee(AccountInfo account) {
        if (account.getType() == AccountType.CHECKING) {
            return CHECKING_FEE;
        } else if (account.getType() == AccountType.SAVINGS) {
            if (account.getCurrentMovements() != null && account.getCurrentMovements() >= account.getMaxMonthlyMovements()) {
                return SAVINGS_LIMIT_FEE;
            }
        }
        return BigDecimal.ZERO;
    }

    private Single<Transaction> markAsFailed(Transaction tx, String reason) {
        return transactionRepository.save(tx.toBuilder().status(TransactionStatus.FAILED).build())
                .flatMap(failedTx -> Single.error(new RuntimeException("Distributed Transaction Failed. Local record marked as FAILED. Reason: " + reason)));
    }
}
