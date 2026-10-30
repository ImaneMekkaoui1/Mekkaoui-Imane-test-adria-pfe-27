package com.adria.bank.transaction.service;

import com.adria.bank.transaction.domain.AccountClientDTO;
import com.adria.bank.transaction.domain.TransactionException;
import com.adria.bank.transaction.domain.UpdateBalanceClientRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class AccountServiceClientFallback implements AccountServiceClient {

    @Override
    public AccountClientDTO getAccountById(UUID id) {
        log.error("Resilience4J Circuit Breaker Fallback triggered: unable to reach account-service for account {}", id);
        throw new TransactionException("Le service de gestion des comptes (account-service) est actuellement indisponible (Resilience4J Fallback).");
    }

    @Override
    public AccountClientDTO updateBalance(UUID id, UpdateBalanceClientRequest request) {
        log.error("Resilience4J Circuit Breaker Fallback triggered: unable to update balance on account-service for account {}", id);
        throw new TransactionException("Le service de gestion des comptes (account-service) est actuellement indisponible pour la mise à jour du solde.");
    }
}

