package com.banking.payment.web;

import com.banking.payment.domain.Payment;
import com.banking.payment.web.dto.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getReferenceId(),
                payment.getRail(),
                payment.getDirection(),
                payment.getStatus(),
                payment.getFromAccountId(),
                payment.getToAccountId(),
                payment.getToExternalAccount(),
                payment.getToExternalRouting(),
                payment.getToExternalBankName(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getDescription(),
                payment.getFailureReason(),
                payment.getExternalReference(),
                payment.getInitiatedAt(),
                payment.getCompletedAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    public <T> com.banking.payment.web.dto.PageResponse<T> toPageResponse(Page<T> page) {
        return new com.banking.payment.web.dto.PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}

