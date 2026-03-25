package com.flash_loan.bank.transaction_service.domain.ports.out;

import com.flash_loan.bank.transaction_service.domain.model.Transaction;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

import java.time.LocalDateTime;

public interface TransactionRepositoryPort {
    Single<Transaction> save(Transaction transaction);
    
    // Podría ser útil para calcular cuantas transacciones lleva en el mes
    Flowable<Transaction> findByAccountId(String accountId);

    Flowable<Transaction> findByAccountIdAndDateRange(String accountId, LocalDateTime start, LocalDateTime end);

    Flowable<Transaction> findAll();

    Maybe<Transaction> findById(String id);

    Single<Boolean> deleteById(String id);
}
