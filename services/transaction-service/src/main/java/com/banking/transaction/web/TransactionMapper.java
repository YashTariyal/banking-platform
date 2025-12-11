package com.banking.transaction.web;

import com.banking.transaction.domain.Transaction;
import com.banking.transaction.web.dto.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getReferenceId(),
                transaction.getTransactionType(),
                transaction.getStatus(),
                transaction.getCustomerId(),
                transaction.getFromAccountId(),
                transaction.getToAccountId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getDescription(),
                transaction.getFailureReason(),
                transaction.getInitiatedAt(),
                transaction.getCompletedAt(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }

    public <T> com.banking.transaction.web.dto.PageResponse<T> toPageResponse(Page<T> page) {
        return new com.banking.transaction.web.dto.PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}

