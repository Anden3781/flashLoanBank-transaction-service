package com.flash_loan.bank.transaction_service.domain.ports.out;

import com.flash_loan.bank.transaction_service.domain.model.AccountInfo;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;

public interface AccountValidationPort {
    Maybe<AccountInfo> getAccount(String accountId);
    Single<Boolean> updateBalance(String accountId, BigDecimal newBalance);
}
