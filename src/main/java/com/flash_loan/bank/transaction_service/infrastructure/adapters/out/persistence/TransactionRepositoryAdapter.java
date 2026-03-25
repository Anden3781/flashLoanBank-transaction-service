package com.flash_loan.bank.transaction_service.infrastructure.adapters.out.persistence;

import com.flash_loan.bank.transaction_service.domain.model.Transaction;
import com.flash_loan.bank.transaction_service.domain.ports.out.TransactionRepositoryPort;
import com.flash_loan.bank.transaction_service.infrastructure.adapters.out.persistence.mapper.TransactionPersistenceMapper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Primary
@RequiredArgsConstructor
public class TransactionRepositoryAdapter implements TransactionRepositoryPort {

    private final ReactiveTransactionMongoRepository repository;
    private final TransactionPersistenceMapper mapper;

    @Override
    public Single<Transaction> save(Transaction transaction) {
        return Single.fromPublisher(repository.save(mapper.toEntity(transaction)))
                .map(mapper::toDomain);
    }

    @Override
    public Flowable<Transaction> findByAccountId(String accountId) {
        return Flowable.fromPublisher(repository.findByAccountIdOrderByTimestampDesc(accountId))
                .map(mapper::toDomain);
    }

    @Override
    public Flowable<Transaction> findByAccountIdAndDateRange(String accountId, LocalDateTime start, LocalDateTime end) {
        return Flowable.fromPublisher(repository.findByAccountIdAndTimestampBetweenOrderByTimestampDesc(accountId, start, end))
                .map(mapper::toDomain);
    }

    @Override
    public Flowable<Transaction> findAll() {
        return Flowable.fromPublisher(repository.findAll())
                .map(mapper::toDomain);
    }

    @Override
    public io.reactivex.rxjava3.core.Maybe<Transaction> findById(String id) {
        return io.reactivex.rxjava3.core.Maybe.fromPublisher(repository.findById(id))
                .map(mapper::toDomain);
    }

    @Override
    public Single<Boolean> deleteById(String id) {
        return Single.fromPublisher(repository.deleteById(id).thenReturn(true).defaultIfEmpty(false));
    }
}
