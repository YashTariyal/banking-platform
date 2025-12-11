package com.banking.payment.web;

import com.banking.payment.domain.Payment;
import com.banking.payment.domain.PaymentStatus;
import com.banking.payment.service.PaymentService;
import com.banking.payment.web.dto.CreatePaymentRequest;
import com.banking.payment.web.dto.FailPaymentRequest;
import com.banking.payment.web.dto.PageResponse;
import com.banking.payment.web.dto.PaymentResponse;
import com.banking.payment.web.dto.ProcessPaymentRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper mapper;

    public PaymentController(PaymentService paymentService, PaymentMapper mapper) {
        this.paymentService = paymentService;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        Payment payment = paymentService.initiatePayment(
                request.referenceId(),
                request.rail(),
                request.direction(),
                request.fromAccountId(),
                request.toAccountId(),
                request.toExternalAccount(),
                request.toExternalRouting(),
                request.toExternalBankName(),
                request.amount(),
                request.currency(),
                request.description()
        );
        return mapper.toResponse(payment);
    }

    @GetMapping("/{id}")
    public PaymentResponse getPayment(@PathVariable UUID id) {
        Payment payment = paymentService.getPayment(id);
        return mapper.toResponse(payment);
    }

    @GetMapping("/reference/{referenceId}")
    public PaymentResponse getPaymentByReferenceId(@PathVariable String referenceId) {
        Payment payment = paymentService.getPaymentByReferenceId(referenceId);
        return mapper.toResponse(payment);
    }

    @GetMapping("/account/{accountId}")
    public PageResponse<PaymentResponse> getPaymentsByAccount(
            @PathVariable UUID accountId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return mapper.toPageResponse(
                paymentService.getPaymentsByAccount(accountId, pageable)
                        .map(mapper::toResponse)
        );
    }

    @GetMapping
    public PageResponse<PaymentResponse> getPayments(
            @RequestParam(required = false) PaymentStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (status != null) {
            return mapper.toPageResponse(
                    paymentService.getPaymentsByStatus(status, pageable)
                            .map(mapper::toResponse)
            );
        }
        return mapper.toPageResponse(
                paymentService.getPaymentsByStatus(PaymentStatus.PENDING, pageable)
                        .map(mapper::toResponse)
        );
    }

    @PutMapping("/{id}/process")
    public PaymentResponse processPayment(
            @PathVariable UUID id,
            @Valid @RequestBody ProcessPaymentRequest request
    ) {
        Payment payment = paymentService.processPayment(id);
        // Auto-complete after processing (simplified - in real system this would be async)
        if (request.externalReference() != null) {
            payment = paymentService.completePayment(id, request.externalReference());
        }
        return mapper.toResponse(payment);
    }

    @PutMapping("/{id}/complete")
    public PaymentResponse completePayment(
            @PathVariable UUID id,
            @RequestParam(required = false) String externalReference
    ) {
        Payment payment = paymentService.completePayment(id, externalReference);
        return mapper.toResponse(payment);
    }

    @PutMapping("/{id}/fail")
    public PaymentResponse failPayment(
            @PathVariable UUID id,
            @Valid @RequestBody FailPaymentRequest request
    ) {
        Payment payment = paymentService.failPayment(id, request.failureReason());
        return mapper.toResponse(payment);
    }

    @PutMapping("/{id}/cancel")
    public PaymentResponse cancelPayment(@PathVariable UUID id) {
        Payment payment = paymentService.cancelPayment(id);
        return mapper.toResponse(payment);
    }
}

