package com.adria.bank.transaction.web.rest;

import com.adria.bank.transaction.domain.CreateTransactionRequest;
import com.adria.bank.transaction.domain.TransactionDTO;
import com.adria.bank.transaction.domain.TransferRequest;
import com.adria.bank.transaction.domain.TransferResult;
import com.adria.bank.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionRestController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        TransactionDTO created = transactionService.createTransaction(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResult> transfer(@Valid @RequestBody TransferRequest request) {
        TransferResult result = transactionService.transfer(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDTO> getTransactionById(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    @GetMapping
    public ResponseEntity<List<TransactionDTO>> getTransactions(
            @RequestParam(required = false) UUID accountId) {
        if (accountId != null) {
            return ResponseEntity.ok(transactionService.getTransactionsByAccountId(accountId));
        }
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }
}

