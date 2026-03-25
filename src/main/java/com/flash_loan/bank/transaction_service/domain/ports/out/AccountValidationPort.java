package com.flash_loan.bank.transaction_service.domain.ports.out;

import com.flash_loan.bank.transaction_service.domain.model.AccountInfo;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;

public interface AccountValidationPort {
    Maybe<AccountInfo> getAccountById(String accountId);
    Single<AccountInfo> applyDebit(String accountId, BigDecimal amount, String transactionId);
    Single<AccountInfo> applyCredit(String accountId, BigDecimal amount, String transactionId);
}
