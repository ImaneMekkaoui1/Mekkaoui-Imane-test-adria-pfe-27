package com.adria.bank.transaction.domain;

public record UpdateBalanceClientRequest(
        double amount,
        String operationType
) {}

