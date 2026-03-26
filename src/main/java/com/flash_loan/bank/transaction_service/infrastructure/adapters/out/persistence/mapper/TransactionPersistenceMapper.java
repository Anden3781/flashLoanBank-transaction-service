package com.flash_loan.bank.transaction_service.infrastructure.adapters.out.persistence.mapper;

import com.flash_loan.bank.transaction_service.domain.model.Transaction;
import com.flash_loan.bank.transaction_service.infrastructure.adapters.out.persistence.entity.TransactionEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TransactionPersistenceMapper {

    public TransactionEntity toEntity(Transaction domain) {
        return Optional.ofNullable(domain)
                .map(d -> TransactionEntity.builder()
                        .id(d.getId())
                        .accountId(d.getAccountId())
                        .amount(d.getAmount())
                        .feeApplied(d.getFeeApplied())
                        .type(d.getType())
                        .timestamp(d.getTimestamp())
                        .resultingBalance(d.getResultingBalance())
                        .status(d.getStatus())
                        .errorMessage(d.getErrorMessage())
                        .build())
                .orElse(null);
    }

    public Transaction toDomain(TransactionEntity entity) {
        return Optional.ofNullable(entity)
                .map(e -> Transaction.builder()
                        .id(e.getId())
                        .accountId(e.getAccountId())
                        .amount(e.getAmount())
                        .feeApplied(e.getFeeApplied())
                        .type(e.getType())
                        .timestamp(e.getTimestamp())
                        .resultingBalance(e.getResultingBalance())
                        .status(e.getStatus())
                        .errorMessage(e.getErrorMessage())
                        .build())
                .orElse(null);
    }
}
