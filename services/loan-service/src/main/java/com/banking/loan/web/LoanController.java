package com.banking.loan.web;

import com.banking.loan.domain.LoanApplication;
import com.banking.loan.service.LoanService;
import com.banking.loan.web.dto.ApproveLoanRequest;
import com.banking.loan.web.dto.CreateLoanApplicationRequest;
import com.banking.loan.web.dto.LoanApplicationResponse;
import com.banking.loan.web.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;
    private final LoanMapper mapper;

    public LoanController(LoanService loanService, LoanMapper mapper) {
        this.loanService = loanService;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoanApplicationResponse create(@Valid @RequestBody CreateLoanApplicationRequest request) {
        LoanApplication app = new LoanApplication();
        app.setCustomerId(request.getCustomerId());
        app.setPrincipal(request.getPrincipal());
        app.setAnnualInterestRate(request.getAnnualInterestRate());
        app.setTermMonths(request.getTermMonths());
        LoanApplication saved = loanService.create(app);
        return mapper.toResponse(saved);
    }

    @PostMapping("/{id}/approve")
    public LoanApplicationResponse approve(@PathVariable UUID id, @RequestBody(required = false) ApproveLoanRequest request) {
        LoanApplication updated = loanService.approve(id, request != null ? request.getFirstDueDate() : null);
        return mapper.toResponse(updated);
    }

    @PostMapping("/{id}/reject")
    public LoanApplicationResponse reject(@PathVariable UUID id) {
        return mapper.toResponse(loanService.reject(id));
    }

    @GetMapping("/{id}")
    public LoanApplicationResponse get(@PathVariable UUID id) {
        return mapper.toResponse(loanService.get(id));
    }

    @GetMapping
    public PageResponse<LoanApplicationResponse> list(
            @ParameterObject Pageable pageable,
            @RequestParam(value = "customerId", required = false) UUID customerId
    ) {
        Page<LoanApplication> page = loanService.list(normalize(pageable), customerId);
        return mapper.toPageResponse(page, mapper::toResponse);
    }

    private Pageable normalize(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20);
        }
        int size = Math.min(pageable.getPageSize(), 200);
        return PageRequest.of(pageable.getPageNumber(), size, pageable.getSort());
    }
}

