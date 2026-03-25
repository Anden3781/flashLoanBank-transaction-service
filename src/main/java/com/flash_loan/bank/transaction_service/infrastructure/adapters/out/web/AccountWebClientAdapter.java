package com.flash_loan.bank.transaction_service.infrastructure.adapters.out.web;

import com.flash_loan.bank.transaction_service.domain.exception.AccountNotFoundException;
import com.flash_loan.bank.transaction_service.domain.model.AccountInfo;
import com.flash_loan.bank.transaction_service.domain.ports.out.AccountValidationPort;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Component
@Primary
public class AccountWebClientAdapter implements AccountValidationPort {

    private final WebClient webClient;

    public AccountWebClientAdapter(WebClient.Builder webClientBuilder, 
                                 @Value("${services.account.url:http://localhost:8082/api/v1/accounts}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public Maybe<AccountInfo> getAccountById(String accountId) {
        return Flowable.fromPublisher(
                webClient.get()
                        .uri("/api/v1/accounts/{id}", accountId)
                        .retrieve()
                        .onStatus(status -> status.equals(HttpStatus.NOT_FOUND), 
                                response -> Mono.error(new AccountNotFoundException("Account not found: " + accountId)))
                        .onStatus(status -> status.isError() && !status.equals(HttpStatus.NOT_FOUND),
                                response -> Mono.error(new RuntimeException("Error communicating with account-service: " + response.statusCode())))
                        .bodyToMono(AccountInfo.class)
        ).firstElement();
    }

    @Override
    public Single<Boolean> updateAccountBalance(String accountId, BigDecimal newBalance) {
        return Flowable.fromPublisher(
                webClient.put()
                        .uri("/api/v1/accounts/{id}/balance", accountId)
                        .bodyValue(newBalance)
                        .retrieve()
                        .onStatus(status -> status.equals(HttpStatus.NOT_FOUND),
                                response -> Mono.error(new AccountNotFoundException("Account not found for update: " + accountId)))
                        .onStatus(status -> status.isError() && !status.equals(HttpStatus.NOT_FOUND),
                                response -> Mono.error(new RuntimeException("Critical error updating balance: " + response.statusCode())))
                        .toBodilessEntity()
                        .map(response -> true)
                        .defaultIfEmpty(true)
        ).firstOrError();
    }
}
