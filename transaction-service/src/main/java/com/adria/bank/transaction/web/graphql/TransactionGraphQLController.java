package com.adria.bank.transaction.web.graphql;

import com.adria.bank.transaction.domain.*;
import com.adria.bank.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TransactionGraphQLController {

    private final TransactionService transactionService;

    @QueryMapping
    public List<TransactionDTO> allTransactions() {
        return transactionService.getAllTransactions();
    }

    @QueryMapping
    public TransactionDTO transactionById(@Argument UUID id) {
        return transactionService.getTransactionById(id);
    }

    @QueryMapping
    public List<TransactionDTO> transactionsByAccount(@Argument UUID accountId) {
        return transactionService.getTransactionsByAccountId(accountId);
    }

    @MutationMapping
    public TransactionDTO createTransaction(@Argument Map<String, Object> input) {
        UUID accountId = UUID.fromString((String) input.get("accountId"));
        BigDecimal amount = BigDecimal.valueOf(((Number) input.get("amount")).doubleValue());
        String currency = (String) input.get("currency");
        String merchantCategory = (String) input.get("merchantCategory");
        String country = (String) input.get("country");

        CreateTransactionRequest request = new CreateTransactionRequest(
                accountId, amount, currency, merchantCategory, country);
        return transactionService.createTransaction(request);
    }

    @MutationMapping
    public TransferResult transferFunds(@Argument Map<String, Object> input) {
        UUID sourceAccountId = UUID.fromString((String) input.get("sourceAccountId"));
        UUID targetAccountId = UUID.fromString((String) input.get("targetAccountId"));
        BigDecimal amount = BigDecimal.valueOf(((Number) input.get("amount")).doubleValue());
        String currency = (String) input.get("currency");

        TransferRequest request = new TransferRequest(sourceAccountId, targetAccountId, amount, currency);
        return transactionService.transfer(request);
    }
}

