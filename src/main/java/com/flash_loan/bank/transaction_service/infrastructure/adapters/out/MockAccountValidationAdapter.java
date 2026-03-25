package com.flash_loan.bank.transaction_service.infrastructure.adapters.out;

import com.flash_loan.bank.transaction_service.domain.model.AccountInfo;
import com.flash_loan.bank.transaction_service.domain.model.AccountType;
import com.flash_loan.bank.transaction_service.domain.ports.out.AccountValidationPort;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class MockAccountValidationAdapter implements AccountValidationPort {

    private final Map<String, AccountInfo> db = new HashMap<>();

    public MockAccountValidationAdapter() {
        // Mock data population
        db.put("AC-SAVINGS-1", AccountInfo.builder().id("AC-SAVINGS-1").balance(new BigDecimal("1000.00")).type(AccountType.SAVINGS).maxMonthlyMovements(3).currentMovements(0).build());
        db.put("AC-CHECKING-1", AccountInfo.builder().id("AC-CHECKING-1").balance(new BigDecimal("5000.00")).type(AccountType.CHECKING).build());
        db.put("AC-FIXED-1", AccountInfo.builder().id("AC-FIXED-1").balance(new BigDecimal("10000.00")).type(AccountType.FIXED_TERM).allowedTransactionDay(LocalDateTime.now().getDayOfMonth()).build());
    }

    @Override
    public Maybe<AccountInfo> getAccountById(String accountId) {
        if (db.containsKey(accountId)) {
            return Maybe.just(db.get(accountId));
        }
        return Maybe.empty();
    }

    @Override
    public Single<Boolean> updateAccountBalance(String accountId, BigDecimal newBalance) {
        return Single.defer(() -> {
            if (db.containsKey(accountId)) {
                db.get(accountId).setBalance(newBalance);
                return Single.just(true);
            }
            return Single.just(false);
        });
    }
}
