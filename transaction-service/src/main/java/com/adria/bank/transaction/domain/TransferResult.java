package com.adria.bank.transaction.domain;

import java.util.UUID;

public record TransferResult(
        UUID sourceTransactionId,
        UUID targetTransactionId,
        String status,
        String message
) {}

