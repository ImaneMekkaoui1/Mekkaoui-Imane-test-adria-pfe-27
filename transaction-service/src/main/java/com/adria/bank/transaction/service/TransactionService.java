package com.adria.bank.transaction.service;

import com.adria.bank.transaction.domain.*;

import java.util.List;
import java.util.UUID;

public interface TransactionService {
    TransactionDTO createTransaction(CreateTransactionRequest request);
    TransferResult transfer(TransferRequest request);
    TransactionDTO getTransactionById(UUID id);
    List<TransactionDTO> getAllTransactions();
    List<TransactionDTO> getTransactionsByAccountId(UUID accountId);
}

