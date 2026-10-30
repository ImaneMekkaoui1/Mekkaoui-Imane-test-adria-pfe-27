package com.adria.bank.transaction.domain;

import java.util.UUID;

public record AccountClientDTO(
        UUID id,
        String customerReference,
        String status,
        String currency,
        double balance
) {}

