package com.adria.bank.transaction.service;

import com.adria.bank.transaction.domain.AccountClientDTO;
import com.adria.bank.transaction.domain.UpdateBalanceClientRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "account-service", url = "${account-service.url:}", fallback = AccountServiceClientFallback.class)
public interface AccountServiceClient {

    @GetMapping("/api/accounts/{id}")
    AccountClientDTO getAccountById(@PathVariable("id") UUID id);

    @PutMapping("/api/accounts/{id}/balance")
    AccountClientDTO updateBalance(@PathVariable("id") UUID id, @RequestBody UpdateBalanceClientRequest request);
}

