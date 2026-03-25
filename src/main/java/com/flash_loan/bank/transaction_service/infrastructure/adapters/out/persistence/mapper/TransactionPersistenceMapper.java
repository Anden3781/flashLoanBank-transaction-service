package com.flash_loan.bank.transaction_service.infrastructure.adapters.out.persistence.mapper;

import com.flash_loan.bank.transaction_service.domain.model.Transaction;
import com.flash_loan.bank.transaction_service.infrastructure.adapters.out.persistence.entity.TransactionEntity;
import org.springframework.stereotype.Component;

@Component
public class TransactionPersistenceMapper {

    public TransactionEntity toEntity(Transaction domain) {
        if (domain == null) return null;
        return TransactionEntity.builder()
                .id(domain.getId())
                .accountId(domain.getAccountId())
                .amount(domain.getAmount())
                .feeApplied(domain.getFeeApplied())
                .type(domain.getType())
                .timestamp(domain.getTimestamp())
                .resultingBalance(domain.getResultingBalance())
                .status(domain.getStatus())
                .build();
    }

    public Transaction toDomain(TransactionEntity entity) {
        if (entity == null) return null;
        return Transaction.builder()
                .id(entity.getId())
                .accountId(entity.getAccountId())
                .amount(entity.getAmount())
                .feeApplied(entity.getFeeApplied())
                .type(entity.getType())
                .timestamp(entity.getTimestamp())
                .resultingBalance(entity.getResultingBalance())
                .status(entity.getStatus())
                .build();
    }
}
