package com.adria.bank.account.domain;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank(message = "customerReference is required")
        String customerReference,

        @NotBlank(message = "currency is required")
        @Size(min = 3, max = 3, message = "currency must be 3 characters (e.g. EUR, USD)")
        String currency,

        @DecimalMin(value = "0.0", message = "initialBalance must be non-negative")
        double initialBalance
) {}

