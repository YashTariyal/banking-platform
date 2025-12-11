package com.banking.loan.web;

import com.banking.loan.domain.LoanPayment;
import com.banking.loan.domain.PaymentScheduleItem;
import com.banking.loan.service.LoanService;
import com.banking.loan.service.PaymentService;
import com.banking.loan.web.dto.LoanPaymentResponse;
import com.banking.loan.web.dto.PaymentRequest;
import com.banking.loan.web.dto.PaymentScheduleItemResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans")
public class PaymentController {

    private final LoanService loanService;
    private final PaymentService paymentService;
    private final LoanMapper mapper;

    public PaymentController(LoanService loanService, PaymentService paymentService, LoanMapper mapper) {
        this.loanService = loanService;
        this.paymentService = paymentService;
        this.mapper = mapper;
    }

    @GetMapping("/{id}/schedule")
    public List<PaymentScheduleItemResponse> schedule(@PathVariable UUID id) {
        List<PaymentScheduleItem> items = loanService.getSchedule(id);
        return items.stream().map(mapper::toResponse).toList();
    }

    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public LoanPaymentResponse pay(@PathVariable UUID id, @Valid @RequestBody PaymentRequest request) {
        LoanPayment payment = paymentService.applyPayment(id, request.getAmount(), request.getPaymentType(), request.getPaymentDate());
        return mapper.toResponse(payment);
    }

    @GetMapping("/{id}/payments")
    public List<LoanPaymentResponse> payments(@PathVariable UUID id) {
        return paymentService.listPayments(id).stream().map(mapper::toResponse).toList();
    }
}

