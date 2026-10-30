package com.adria.bank.account.service;

import com.adria.bank.account.domain.*;
import com.adria.bank.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    public AccountDTO createAccount(CreateAccountRequest request) {
        log.info("Creating account for customer: {}, currency: {}, initialBalance: {}",
                request.customerReference(), request.currency(), request.initialBalance());

        BankAccount account = BankAccount.builder()
                .id(UUID.randomUUID())
                .customerReference(request.customerReference())
                .currency(request.currency().toUpperCase())
                .balance(request.initialBalance())
                .status(AccountStatus.ACTIVE)
                .build();

        BankAccount saved = accountRepository.save(account);
        return AccountDTO.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO getAccountById(UUID id) {
        BankAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountException("Compte non trouvé avec l'identifiant : " + id));
        return AccountDTO.fromEntity(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountDTO> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(AccountDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountDTO> getAccountsByCustomer(String customerReference) {
        return accountRepository.findByCustomerReference(customerReference).stream()
                .map(AccountDTO::fromEntity)
                .toList();
    }

    @Override
    public AccountDTO updateBalance(UUID id, UpdateBalanceRequest request) {
        BankAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountException("Compte non trouvé avec l'identifiant : " + id));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountException("Opération impossible : le compte " + id + " est " + account.getStatus());
        }

        if (request.amount() <= 0) {
            throw new AccountException("Le montant doit être strictement positif");
        }

        if ("DEBIT".equalsIgnoreCase(request.operationType())) {
            if (account.getBalance() < request.amount()) {
                throw new AccountException("Solde insuffisant (" + account.getBalance() + " " + account.getCurrency()
                        + ") pour effectuer un débit de " + request.amount());
            }
            account.setBalance(account.getBalance() - request.amount());
        } else if ("CREDIT".equalsIgnoreCase(request.operationType())) {
            account.setBalance(account.getBalance() + request.amount());
        } else {
            throw new AccountException("Type d'opération inconnu : " + request.operationType() + ". Utilisez CREDIT ou DEBIT.");
        }

        log.info("Updated balance for account {}: new balance = {}", id, account.getBalance());
        return AccountDTO.fromEntity(account);
    }

    @Override
    public AccountDTO changeStatus(UUID id, AccountStatus status) {
        BankAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountException("Compte non trouvé avec l'identifiant : " + id));
        account.setStatus(status);
        log.info("Account {} status changed to {}", id, status);
        return AccountDTO.fromEntity(account);
    }
}

