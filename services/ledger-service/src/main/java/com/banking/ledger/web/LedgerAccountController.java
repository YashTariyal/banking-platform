package com.banking.ledger.web;

import com.banking.ledger.domain.LedgerAccount;
import com.banking.ledger.service.LedgerAccountService;
import com.banking.ledger.web.dto.CreateLedgerAccountRequest;
import com.banking.ledger.web.dto.LedgerAccountResponse;
import com.banking.ledger.web.dto.PageResponse;
import com.banking.ledger.web.dto.UpdateAccountStatusRequest;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ledger/accounts")
public class LedgerAccountController {

    private final LedgerAccountService ledgerAccountService;
    private final LedgerMapper mapper;

    public LedgerAccountController(LedgerAccountService ledgerAccountService, LedgerMapper mapper) {
        this.ledgerAccountService = ledgerAccountService;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LedgerAccountResponse create(@Valid @RequestBody CreateLedgerAccountRequest request) {
        LedgerAccount account = new LedgerAccount();
        account.setName(request.getName());
        account.setType(request.getType());
        account.setCurrency(request.getCurrency());
        account.setExternalAccountId(request.getExternalAccountId());
        LedgerAccount created = ledgerAccountService.create(account);
        return mapper.toResponse(created);
    }

    @GetMapping("/{id}")
    public LedgerAccountResponse get(@PathVariable UUID id) {
        return mapper.toResponse(ledgerAccountService.get(id));
    }

    @GetMapping
    public PageResponse<LedgerAccountResponse> list(@ParameterObject Pageable pageable) {
        Page<LedgerAccount> page = ledgerAccountService.list(normalize(pageable));
        return mapper.toPageResponse(page, mapper::toResponse);
    }

    @PutMapping("/{id}/status")
    public LedgerAccountResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAccountStatusRequest request
    ) {
        return mapper.toResponse(ledgerAccountService.updateStatus(id, request.getStatus()));
    }

    private Pageable normalize(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20);
        }
        int size = Math.min(pageable.getPageSize(), 200);
        return PageRequest.of(pageable.getPageNumber(), size, pageable.getSort());
    }
}

