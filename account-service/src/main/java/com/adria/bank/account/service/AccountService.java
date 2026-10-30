package com.adria.bank.account.service;

import com.adria.bank.account.domain.AccountDTO;
import com.adria.bank.account.domain.AccountStatus;
import com.adria.bank.account.domain.CreateAccountRequest;
import com.adria.bank.account.domain.UpdateBalanceRequest;

import java.util.List;
import java.util.UUID;

public interface AccountService {
    AccountDTO createAccount(CreateAccountRequest request);
    AccountDTO getAccountById(UUID id);
    List<AccountDTO> getAllAccounts();
    List<AccountDTO> getAccountsByCustomer(String customerReference);
    AccountDTO updateBalance(UUID id, UpdateBalanceRequest request);
    AccountDTO changeStatus(UUID id, AccountStatus status);
}

