package com.flash_loan.bank.transaction_service.infrastructure.adapters.out.web;

import com.flash_loan.bank.transaction_service.domain.exception.AccountNotFoundException;
import com.flash_loan.bank.transaction_service.domain.exception.RuleViolationException;
import com.flash_loan.bank.transaction_service.domain.model.AccountInfo;
import com.flash_loan.bank.transaction_service.domain.ports.out.AccountValidationPort;
import com.flash_loan.bank.transaction_service.infrastructure.adapters.out.web.dto.request.AccountBalanceOperationRequestDto;
import com.flash_loan.bank.transaction_service.infrastructure.adapters.out.web.dto.response.AccountServiceErrorResponseDto;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Component
public class AccountWebClientAdapter implements AccountValidationPort {

    private final WebClient webClient;

    public AccountWebClientAdapter(WebClient.Builder webClientBuilder,
                                   @Value("${services.account.url:http://account-service:8082}") String baseUrl) {
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
                        .onStatus(status -> status.equals(HttpStatus.BAD_REQUEST),
                                response -> response.bodyToMono(AccountServiceErrorResponseDto.class)
                                        .map(err -> new RuleViolationException(readMessage(err, "Account validation failed"))))
                        .onStatus(status -> status.isError() && !status.equals(HttpStatus.NOT_FOUND),
                                response -> Mono.error(new RuntimeException("Error communicating with account-service: " + response.statusCode())))
                        .bodyToMono(AccountInfo.class)
        ).firstElement();
    }

    @Override
    public Single<AccountInfo> applyDebit(String accountId, BigDecimal amount, String transactionId) {
        return Flowable.fromPublisher(
                webClient.post()
                        .uri("/api/v1/accounts/{id}/debit", accountId)
                        .bodyValue(new AccountBalanceOperationRequestDto(amount, transactionId))
                        .retrieve()
                        .onStatus(status -> status.equals(HttpStatus.NOT_FOUND),
                                response -> Mono.error(new AccountNotFoundException("Account not found for debit: " + accountId)))
                        .onStatus(status -> status.equals(HttpStatus.BAD_REQUEST),
                                response -> response.bodyToMono(AccountServiceErrorResponseDto.class)
                                        .map(err -> new RuleViolationException(readMessage(err, "Debit operation rejected"))))
                        .onStatus(status -> status.isError() && !status.equals(HttpStatus.NOT_FOUND),
                                response -> Mono.error(new RuntimeException("Critical error debiting account: " + response.statusCode())))
                        .bodyToMono(AccountInfo.class)
        ).firstOrError();
    }

    @Override
    public Single<AccountInfo> applyCredit(String accountId, BigDecimal amount, String transactionId) {
        return Flowable.fromPublisher(
                webClient.post()
                        .uri("/api/v1/accounts/{id}/credit", accountId)
                        .bodyValue(new AccountBalanceOperationRequestDto(amount, transactionId))
                        .retrieve()
                        .onStatus(status -> status.equals(HttpStatus.NOT_FOUND),
                                response -> Mono.error(new AccountNotFoundException("Account not found for credit: " + accountId)))
                        .onStatus(status -> status.equals(HttpStatus.BAD_REQUEST),
                                response -> response.bodyToMono(AccountServiceErrorResponseDto.class)
                                        .map(err -> new RuleViolationException(readMessage(err, "Credit operation rejected"))))
                        .onStatus(status -> status.isError() && !status.equals(HttpStatus.NOT_FOUND),
                                response -> Mono.error(new RuntimeException("Critical error crediting account: " + response.statusCode())))
                        .bodyToMono(AccountInfo.class)
        ).firstOrError();
    }

    private String readMessage(AccountServiceErrorResponseDto error, String fallbackMessage) {
        if (error == null || error.getMessage() == null || error.getMessage().isBlank()) {
            return fallbackMessage;
        }
        return error.getMessage();
    }
}
