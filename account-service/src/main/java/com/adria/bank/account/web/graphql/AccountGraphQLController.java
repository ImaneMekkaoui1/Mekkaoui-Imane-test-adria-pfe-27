package com.adria.bank.account.web.graphql;

import com.adria.bank.account.domain.AccountDTO;
import com.adria.bank.account.domain.AccountStatus;
import com.adria.bank.account.domain.CreateAccountRequest;
import com.adria.bank.account.domain.UpdateBalanceRequest;
import com.adria.bank.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AccountGraphQLController {

    private final AccountService accountService;

    @QueryMapping
    public List<AccountDTO> allAccounts() {
        return accountService.getAllAccounts();
    }

    @QueryMapping
    public AccountDTO accountById(@Argument UUID id) {
        return accountService.getAccountById(id);
    }

    @QueryMapping
    public List<AccountDTO> accountsByCustomer(@Argument String customerReference) {
        return accountService.getAccountsByCustomer(customerReference);
    }

    @MutationMapping
    public AccountDTO createAccount(@Argument Map<String, Object> input) {
        String customerRef = (String) input.get("customerReference");
        String currency = (String) input.get("currency");
        double initialBalance = ((Number) input.get("initialBalance")).doubleValue();

        CreateAccountRequest request = new CreateAccountRequest(customerRef, currency, initialBalance);
        return accountService.createAccount(request);
    }

    @MutationMapping
    public AccountDTO updateBalance(@Argument Map<String, Object> input) {
        UUID accountId = UUID.fromString((String) input.get("accountId"));
        double amount = ((Number) input.get("amount")).doubleValue();
        String operationType = (String) input.get("operationType");

        UpdateBalanceRequest request = new UpdateBalanceRequest(amount, operationType);
        return accountService.updateBalance(accountId, request);
    }

    @MutationMapping
    public AccountDTO blockAccount(@Argument UUID id) {
        return accountService.changeStatus(id, AccountStatus.BLOCKED);
    }

    @MutationMapping
    public AccountDTO activateAccount(@Argument UUID id) {
        return accountService.changeStatus(id, AccountStatus.ACTIVE);
    }
}

