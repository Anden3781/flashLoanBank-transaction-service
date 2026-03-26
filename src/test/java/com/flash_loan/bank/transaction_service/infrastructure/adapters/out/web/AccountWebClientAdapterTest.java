package com.flash_loan.bank.transaction_service.infrastructure.adapters.out.web;

import com.flash_loan.bank.transaction_service.domain.exception.AccountNotFoundException;
import com.flash_loan.bank.transaction_service.domain.exception.RuleViolationException;
import com.flash_loan.bank.transaction_service.domain.model.AccountInfo;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountWebClientAdapterTest {

    private AccountWebClientAdapter adapter(ExchangeFunction fn) {
        return new AccountWebClientAdapter(WebClient.builder().exchangeFunction(fn), "http://account");
    }

    @Test
    void getAccountById_Success() {
        String json = "{\"id\":\"acc-1\",\"customerId\":\"cust-1\",\"type\":\"SAVINGS\",\"balance\":1000,\"maxMonthlyMovements\":10,\"currentMovements\":0}";
        ExchangeFunction fn = req -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .build());
        AccountWebClientAdapter adapter = adapter(fn);
        AccountInfo info = adapter.getAccountById("acc-1").blockingGet();
        assertThat(info.getId()).isEqualTo("acc-1");
    }

    @Test
    void getAccountById_NotFound() {
        ExchangeFunction fn = req -> Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build());
        AccountWebClientAdapter adapter = adapter(fn);
        assertThatThrownBy(() -> adapter.getAccountById("acc-x").blockingGet())
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void getAccountById_BadRequest_RuleViolation() {
        String err = "{\"message\":\"Invalid account\"}";
        ExchangeFunction fn = req -> Mono.just(ClientResponse.create(HttpStatus.BAD_REQUEST)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(err)
                .build());
        AccountWebClientAdapter adapter = adapter(fn);
        assertThatThrownBy(() -> adapter.getAccountById("acc-1").blockingGet())
                .isInstanceOf(RuleViolationException.class)
                .hasMessageContaining("Invalid account");
    }

    @Test
    void getAccountById_ServerError_RuntimeException() {
        ExchangeFunction fn = req -> Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build());
        AccountWebClientAdapter adapter = adapter(fn);
        assertThatThrownBy(() -> adapter.getAccountById("acc-1").blockingGet())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("account-service");
    }

    @Test
    void applyDebit_Success() {
        String json = "{\"id\":\"acc-1\",\"customerId\":\"cust-1\",\"type\":\"SAVINGS\",\"balance\":990,\"maxMonthlyMovements\":10,\"currentMovements\":1}";
        ExchangeFunction fn = req -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .build());
        AccountWebClientAdapter adapter = adapter(fn);
        AccountInfo info = adapter.applyDebit("acc-1", BigDecimal.TEN, "tx-1").blockingGet();
        assertThat(info.getBalance()).isEqualByComparingTo("990");
    }

    @Test
    void applyCredit_BadRequest_RuleViolation() {
        String err = "{\"message\":\"Limit exceeded\"}";
        ExchangeFunction fn = req -> Mono.just(ClientResponse.create(HttpStatus.BAD_REQUEST)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(err)
                .build());
        AccountWebClientAdapter adapter = adapter(fn);
        assertThatThrownBy(() -> adapter.applyCredit("acc-1", BigDecimal.TEN, "tx-1").blockingGet())
                .isInstanceOf(RuleViolationException.class)
                .hasMessageContaining("Limit exceeded");
    }

    @Test
    void applyDebit_NotFound() {
        ExchangeFunction fn = req -> Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build());
        AccountWebClientAdapter adapter = adapter(fn);
        assertThatThrownBy(() -> adapter.applyDebit("acc-x", BigDecimal.ONE, "tx-1").blockingGet())
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void applyCredit_ServerError_RuntimeException() {
        ExchangeFunction fn = req -> Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build());
        AccountWebClientAdapter adapter = adapter(fn);
        assertThatThrownBy(() -> adapter.applyCredit("acc-1", BigDecimal.ONE, "tx-1").blockingGet())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Critical error crediting account");
    }

    @Test
    void getAccountById_BadRequest_EmptyMessage_UsesFallback() {
        String err = "{}";
        ExchangeFunction fn = req -> Mono.just(ClientResponse.create(HttpStatus.BAD_REQUEST)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(err)
                .build());
        AccountWebClientAdapter adapter = adapter(fn);
        assertThatThrownBy(() -> adapter.getAccountById("acc-1").blockingGet())
                .isInstanceOf(RuleViolationException.class)
                .hasMessageContaining("Account validation failed");
    }
}
