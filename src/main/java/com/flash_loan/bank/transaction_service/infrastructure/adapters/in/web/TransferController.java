package com.flash_loan.bank.transaction_service.infrastructure.adapters.in.web;

import com.flash_loan.bank.transaction_service.application.dto.TransferRequest;
import com.flash_loan.bank.transaction_service.application.dto.TransferResponse;
import com.flash_loan.bank.transaction_service.application.service.TransferService;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for bank transfer operations.
 * All methods return reactive types compatible with Spring WebFlux.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    /**
     * Initiates a bank transfer between two accounts (same owner or third party).
     * Creates two transaction records linked by a common transferId.
     *
     * @param request the transfer command
     * @return 201 CREATED with both legs of the transfer
     */
    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public Single<TransferResponse> transfer(@RequestBody TransferRequest request) {
        return transferService.executeTransfer(request);
    }
}
