package com.adria.bank.transaction.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionCreatedEvent(
        UUID eventId,
        String eventType,
        UUID transactionId,
        UUID accountId,
        BigDecimal amount,
        String currency,
        Instant occurredAt
) {
    public static TransactionCreatedEvent of(BankTransaction tx) {
        return new TransactionCreatedEvent(
                UUID.randomUUID(),
                "TransactionCreated",
                tx.getId(),
                tx.getAccountId(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getCreatedAt() != null ? tx.getCreatedAt() : Instant.now()
        );
    }
}

