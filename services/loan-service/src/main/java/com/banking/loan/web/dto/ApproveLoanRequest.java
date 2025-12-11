package com.banking.loan.web.dto;

import java.time.LocalDate;

public class ApproveLoanRequest {

    private LocalDate firstDueDate;

    public LocalDate getFirstDueDate() {
        return firstDueDate;
    }

    public void setFirstDueDate(LocalDate firstDueDate) {
        this.firstDueDate = firstDueDate;
    }
}

