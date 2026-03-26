package com.flash_loan.bank.transaction_service.infrastructure.adapters.out.persistence;

import com.flash_loan.bank.transaction_service.domain.model.Transaction;
import com.flash_loan.bank.transaction_service.domain.model.TransactionType;
import com.flash_loan.bank.transaction_service.infrastructure.adapters.out.persistence.entity.TransactionEntity;
import com.flash_loan.bank.transaction_service.infrastructure.adapters.out.persistence.mapper.TransactionPersistenceMapper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionRepositoryAdapterTest {

    @Mock
    private ReactiveTransactionMongoRepository repository;

    private TransactionPersistenceMapper mapper;
    private TransactionRepositoryAdapter adapter;

    private Transaction tx;
    private TransactionEntity entity;

    @BeforeEach
    void setUp() {
        mapper = new TransactionPersistenceMapper();
        adapter = new TransactionRepositoryAdapter(repository, mapper);

        tx = Transaction.builder()
                .id("tx-1")
                .accountId("acc-1")
                .amount(BigDecimal.ONE)
                .type(TransactionType.DEPOSIT)
                .timestamp(LocalDateTime.now())
                .resultingBalance(new BigDecimal("11"))
                .build();

        entity = mapper.toEntity(tx);
    }

    @Test
    void save_ShouldReturnDomain() {
        when(repository.save(any(TransactionEntity.class))).thenReturn(Mono.just(entity));
        Transaction result = adapter.save(tx).blockingGet();
        assertThat(result.getId()).isEqualTo("tx-1");
    }

    @Test
    void findByAccountId_ShouldReturnFlowable() {
        when(repository.findByAccountIdOrderByTimestampDesc(anyString())).thenReturn(Flux.just(entity));
        Flowable<Transaction> flow = adapter.findByAccountId("acc-1");
        assertThat(flow.toList().blockingGet()).hasSize(1);
    }

    @Test
    void findByAccountIdAndDateRange_ShouldReturnFlowable() {
        when(repository.findByAccountIdAndTimestampBetweenOrderByTimestampDesc(anyString(), any(), any()))
                .thenReturn(Flux.just(entity));
        assertThat(adapter.findByAccountIdAndDateRange("acc-1", LocalDateTime.now().minusDays(1), LocalDateTime.now())
                .toList().blockingGet()).hasSize(1);
    }

    @Test
    void findAll_ShouldReturnFlowable() {
        when(repository.findAll()).thenReturn(Flux.just(entity));
        assertThat(adapter.findAll().toList().blockingGet()).hasSize(1);
    }

    @Test
    void findById_ShouldReturnMaybe() {
        when(repository.findById(anyString())).thenReturn(Mono.just(entity));
        assertThat(adapter.findById("tx-1").blockingGet().getId()).isEqualTo("tx-1");
    }

    @Test
    void deleteById_ShouldReturnTrue() {
        when(repository.deleteById(anyString())).thenReturn(Mono.empty());
        Boolean result = adapter.deleteById("tx-1").blockingGet();
        assertThat(result).isTrue();
    }
}
