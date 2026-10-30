package com.adria.bank.account.domain;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateBalanceRequest(
        @DecimalMin(value = "0.01", message = "amount must be strictly positive")
        double amount,

        @NotBlank(message = "operationType is required")
        @Pattern(regexp = "CREDIT|DEBIT", message = "operationType must be CREDIT or DEBIT")
        String operationType
) {}

