package com.adria.bank.transaction.domain;

public class TransactionException extends RuntimeException {
    public TransactionException(String message) {
        super(message);
    }
}

