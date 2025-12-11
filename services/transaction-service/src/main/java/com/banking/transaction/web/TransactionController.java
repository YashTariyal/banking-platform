package com.banking.transaction.web;

import com.banking.transaction.domain.Transaction;
import com.banking.transaction.domain.TransactionStatus;
import com.banking.transaction.domain.TransactionType;
import com.banking.transaction.service.TransactionService;
import com.banking.transaction.web.dto.FailTransactionRequest;
import com.banking.transaction.web.dto.PageResponse;
import com.banking.transaction.web.dto.ReverseTransactionRequest;
import com.banking.transaction.web.dto.TransactionResponse;
import com.banking.transaction.web.dto.CreateTransactionRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionMapper mapper;

    public TransactionController(TransactionService transactionService, TransactionMapper mapper) {
        this.transactionService = transactionService;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        Transaction transaction = transactionService.createTransaction(
                request.referenceId(),
                request.transactionType(),
                request.customerId(),
                request.fromAccountId(),
                request.toAccountId(),
                request.amount(),
                request.currency(),
                request.description()
        );
        return mapper.toResponse(transaction);
    }

    @GetMapping("/{id}")
    public TransactionResponse getTransaction(@PathVariable UUID id) {
        Transaction transaction = transactionService.getTransaction(id);
        return mapper.toResponse(transaction);
    }

    @GetMapping("/reference/{referenceId}")
    public TransactionResponse getTransactionByReferenceId(@PathVariable String referenceId) {
        Transaction transaction = transactionService.getTransactionByReferenceId(referenceId);
        return mapper.toResponse(transaction);
    }

    @GetMapping
    public PageResponse<TransactionResponse> getTransactions(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID accountId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (status != null) {
            return mapper.toPageResponse(
                    transactionService.getTransactionsByStatus(status, pageable)
                            .map(mapper::toResponse)
            );
        }
        if (transactionType != null) {
            return mapper.toPageResponse(
                    transactionService.getTransactionsByType(transactionType, pageable)
                            .map(mapper::toResponse)
            );
        }
        if (customerId != null) {
            return mapper.toPageResponse(
                    transactionService.getTransactionsByCustomer(customerId, pageable)
                            .map(mapper::toResponse)
            );
        }
        if (accountId != null) {
            return mapper.toPageResponse(
                    transactionService.getTransactionsByAccount(accountId, pageable)
                            .map(mapper::toResponse)
            );
        }
        // Default: return empty page (could be enhanced to return all transactions)
        return mapper.toPageResponse(
                org.springframework.data.domain.Page.empty(pageable)
        );
    }

    @PutMapping("/{id}/process")
    public TransactionResponse processTransaction(@PathVariable UUID id) {
        Transaction transaction = transactionService.processTransaction(id);
        return mapper.toResponse(transaction);
    }

    @PutMapping("/{id}/complete")
    public TransactionResponse completeTransaction(@PathVariable UUID id) {
        Transaction transaction = transactionService.completeTransaction(id);
        return mapper.toResponse(transaction);
    }

    @PutMapping("/{id}/fail")
    public TransactionResponse failTransaction(
            @PathVariable UUID id,
            @Valid @RequestBody FailTransactionRequest request
    ) {
        Transaction transaction = transactionService.failTransaction(id, request.failureReason());
        return mapper.toResponse(transaction);
    }

    @PutMapping("/{id}/cancel")
    public TransactionResponse cancelTransaction(@PathVariable UUID id) {
        Transaction transaction = transactionService.cancelTransaction(id);
        return mapper.toResponse(transaction);
    }

    @PutMapping("/{id}/reverse")
    public TransactionResponse reverseTransaction(
            @PathVariable UUID id,
            @Valid @RequestBody ReverseTransactionRequest request
    ) {
        Transaction transaction = transactionService.reverseTransaction(id, request.reason());
        return mapper.toResponse(transaction);
    }
}

