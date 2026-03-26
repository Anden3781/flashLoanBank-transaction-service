package com.flash_loan.bank.transaction_service.infrastructure.adapters.out.persistence.mapper;

import com.flash_loan.bank.transaction_service.domain.model.Transaction;
import com.flash_loan.bank.transaction_service.domain.model.TransactionStatus;
import com.flash_loan.bank.transaction_service.domain.model.TransactionType;
import com.flash_loan.bank.transaction_service.infrastructure.adapters.out.persistence.entity.TransactionEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionPersistenceMapperTest {

    private final TransactionPersistenceMapper mapper = new TransactionPersistenceMapper();

    @Test
    void toEntity_and_toDomain_ShouldPreserveFields() {
        Transaction tx = Transaction.builder()
                .id("tx-1")
                .accountId("acc-1")
                .amount(BigDecimal.TEN)
                .feeApplied(BigDecimal.ZERO)
                .type(TransactionType.DEPOSIT)
                .timestamp(LocalDateTime.now())
                .resultingBalance(new BigDecimal("110"))
                .status(TransactionStatus.SUCCESS)
                .errorMessage(null)
                .build();

        TransactionEntity entity = mapper.toEntity(tx);
        assertThat(entity.getAccountId()).isEqualTo("acc-1");
        assertThat(entity.getResultingBalance()).isEqualByComparingTo("110");

        Transaction back = mapper.toDomain(entity);
        assertThat(back.getId()).isEqualTo("tx-1");
        assertThat(back.getType()).isEqualTo(TransactionType.DEPOSIT);
    }

    @Test
    void toEntity_Null_ReturnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toDomain_Null_ReturnsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }
}
