package com.adria.bank.account.domain;

import java.util.UUID;

public record AccountDTO(
        UUID id,
        String customerReference,
        AccountStatus status,
        String currency,
        double balance
) {
    public static AccountDTO fromEntity(BankAccount account) {
        return new AccountDTO(
                account.getId(),
                account.getCustomerReference(),
                account.getStatus(),
                account.getCurrency(),
                account.getBalance()
        );
    }
}

