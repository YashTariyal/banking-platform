package com.banking.loan.web;

import com.banking.loan.domain.LoanApplication;
import com.banking.loan.domain.LoanPayment;
import com.banking.loan.domain.PaymentScheduleItem;
import com.banking.loan.web.dto.LoanApplicationResponse;
import com.banking.loan.web.dto.LoanPaymentResponse;
import com.banking.loan.web.dto.PageResponse;
import com.banking.loan.web.dto.PaymentScheduleItemResponse;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class LoanMapper {

    public LoanApplicationResponse toResponse(LoanApplication loan) {
        LoanApplicationResponse response = new LoanApplicationResponse();
        response.setId(loan.getId());
        response.setCustomerId(loan.getCustomerId());
        response.setPrincipal(loan.getPrincipal());
        response.setAnnualInterestRate(loan.getAnnualInterestRate());
        response.setTermMonths(loan.getTermMonths());
        response.setStatus(loan.getStatus());
        response.setApprovedAt(loan.getApprovedAt());
        response.setRejectedAt(loan.getRejectedAt());
        response.setDisbursedAt(loan.getDisbursedAt());
        response.setCreatedAt(loan.getCreatedAt());
        response.setUpdatedAt(loan.getUpdatedAt());
        response.setFirstDueDate(loan.getFirstDueDate());
        return response;
    }

    public PaymentScheduleItemResponse toResponse(PaymentScheduleItem item) {
        PaymentScheduleItemResponse response = new PaymentScheduleItemResponse();
        response.setId(item.getId());
        response.setInstallmentNumber(item.getInstallmentNumber());
        response.setDueDate(item.getDueDate());
        response.setPrincipalDue(item.getPrincipalDue());
        response.setInterestDue(item.getInterestDue());
        response.setPrincipalPaid(item.getPrincipalPaid());
        response.setInterestPaid(item.getInterestPaid());
        response.setStatus(item.getStatus());
        return response;
    }

    public LoanPaymentResponse toResponse(LoanPayment payment) {
        LoanPaymentResponse response = new LoanPaymentResponse();
        response.setId(payment.getId());
        response.setLoanApplicationId(payment.getLoanApplicationId());
        response.setPaymentType(payment.getPaymentType());
        response.setAmount(payment.getAmount());
        response.setPrincipalComponent(payment.getPrincipalComponent());
        response.setInterestComponent(payment.getInterestComponent());
        response.setPaymentDate(payment.getPaymentDate());
        response.setCreatedAt(payment.getCreatedAt());
        return response;
    }

    public <T, R> PageResponse<R> toPageResponse(Page<T> page, Function<T, R> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}

