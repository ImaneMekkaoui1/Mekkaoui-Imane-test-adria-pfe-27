package com.adria.bank.account.web.rest;

import com.adria.bank.account.domain.AccountDTO;
import com.adria.bank.account.domain.AccountStatus;
import com.adria.bank.account.domain.ChangeStatusRequest;
import com.adria.bank.account.domain.CreateAccountRequest;
import com.adria.bank.account.domain.UpdateBalanceRequest;
import com.adria.bank.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountRestController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountDTO> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountDTO created = accountService.createAccount(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDTO> getAccountById(@PathVariable UUID id) {
        return ResponseEntity.ok(accountService.getAccountById(id));
    }

    @GetMapping
    public ResponseEntity<List<AccountDTO>> getAllAccounts(
            @RequestParam(required = false) String customerReference) {
        if (customerReference != null && !customerReference.isBlank()) {
            return ResponseEntity.ok(accountService.getAccountsByCustomer(customerReference));
        }
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @PutMapping("/{id}/balance")
    public ResponseEntity<AccountDTO> updateBalance(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBalanceRequest request) {
        return ResponseEntity.ok(accountService.updateBalance(id, request));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<AccountDTO> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeStatusRequest request) {
        return ResponseEntity.ok(accountService.changeStatus(id, request.status()));
    }
}

