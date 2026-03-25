package com.flash_loan.bank.transaction_service.infrastructure.adapters.out.persistence;

import com.flash_loan.bank.transaction_service.infrastructure.adapters.out.persistence.entity.TransactionEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ReactiveTransactionMongoRepository extends ReactiveMongoRepository<TransactionEntity, String> {
    Flux<TransactionEntity> findByAccountId(String accountId);
}
