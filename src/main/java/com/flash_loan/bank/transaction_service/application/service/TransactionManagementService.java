package com.flash_loan.bank.transaction_service.application.service;

import com.flash_loan.bank.transaction_service.application.dto.TransactionRequest;
import com.flash_loan.bank.transaction_service.domain.exception.RuleViolationException;
import com.flash_loan.bank.transaction_service.domain.model.AccountType;
import com.flash_loan.bank.transaction_service.domain.model.Transaction;
import com.flash_loan.bank.transaction_service.domain.model.TransactionType;
import com.flash_loan.bank.transaction_service.domain.ports.out.AccountValidationPort;
import com.flash_loan.bank.transaction_service.domain.ports.out.TransactionRepositoryPort;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionManagementService {

    private final AccountValidationPort accountPort;
    private final TransactionRepositoryPort transactionRepository;

    private static final BigDecimal CHECKING_FEE = new BigDecimal("2.50");
    private static final BigDecimal SAVINGS_LIMIT_FEE = new BigDecimal("5.00");

    public Single<Transaction> executeTransaction(TransactionRequest command) {
        // 1. Fetch
        return accountPort.getAccount(command.getAccountId())
                .switchIfEmpty(Maybe.error(new RuleViolationException("Account does not exist")))
                .toSingle()
                
                // 2. Validate Rules
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
                
                // 3. Calculate Fees and Check Funds
                .flatMap(account -> {
                    BigDecimal fee = BigDecimal.ZERO;
                    
                    if (account.getType() == AccountType.CHECKING) {
                        fee = CHECKING_FEE;
                    } else if (account.getType() == AccountType.SAVINGS) {
                        if (account.getCurrentMovements() != null && account.getCurrentMovements() >= account.getMaxMonthlyMovements()) {
                            fee = SAVINGS_LIMIT_FEE;
                        }
                    }

                    // Balance impact: for DEPOSIT, it's (+amount - fee). For WITHDRAWAL, it's (-amount - fee).
                    BigDecimal impact = command.getType() == TransactionType.DEPOSIT
                            ? command.getAmount().subtract(fee)
                            : command.getAmount().negate().subtract(fee);

                    BigDecimal newBalance = account.getBalance().add(impact);

                    if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                        return Single.error(new RuleViolationException("Insufficient funds after applying fees"));
                    }

                    Transaction tx = Transaction.builder()
                            .accountId(command.getAccountId())
                            .amount(command.getAmount())
                            .feeApplied(fee)
                            .type(command.getType())
                            .timestamp(LocalDateTime.now())
                            .resultingBalance(newBalance)
                            .build();

                    return Single.just(tx);
                })
                
                // 4. Persist (Local Transaction)
                .flatMap(transactionRepository::save)
                
                // 5. Update (Remote Account-Service) y Distributed Transaction atomicity (SAGA logic)
                .flatMap(tx -> accountPort.updateBalance(tx.getAccountId(), tx.getResultingBalance())
                        .flatMap(success -> {
                            if (!success) {
                                return Single.error(new RuntimeException("Remote balance update rejected."));
                            }
                            return Single.just(tx);
                        })
                        
                        // SAGA PATTERN: Compensating Transaction logic if Step 5 fails
                        .onErrorResumeNext(error -> {
                            /**
                             * [ATOMICITY IN MICROSERVICES]
                             * If the remote update fails or times out, the local MongoDB already persisted the transaction!
                             * This creates an inconsistency. 
                             * Here we apply the Compensating Transaction of the SAGA Pattern:
                             * 1. Revert the local transaction state to 'FAILED'.
                             *    (We would need a status field in Transaction model and save it here).
                             * 2. Log exactly what failed.
                             * 3. Finally, propagate the error up so the API returns 500 to the client.
                             */
                            // Pendent: transactionRepository.markAsFailed(tx.getId()) // Compensating action
                            return Single.error(new RuntimeException("Distributed Transaction Failed. The local record must be marked as FAILED. Caused by: " + error.getMessage()));
                        })
                );
    }
}
