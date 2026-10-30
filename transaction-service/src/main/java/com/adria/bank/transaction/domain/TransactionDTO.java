package com.adria.bank.transaction.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionDTO(
        UUID id,
        UUID accountId,
        BigDecimal amount,
        String currency,
        String merchantCategory,
        String country,
        Instant createdAt
) {
    public static TransactionDTO fromEntity(BankTransaction entity) {
        return new TransactionDTO(
                entity.getId(),
                entity.getAccountId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getMerchantCategory(),
                entity.getCountry(),
                entity.getCreatedAt()
        );
    }
}

