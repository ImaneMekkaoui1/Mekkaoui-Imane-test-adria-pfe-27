package com.adria.bank.account.domain;

import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(
        @NotNull(message = "status is required")
        AccountStatus status
) {}

