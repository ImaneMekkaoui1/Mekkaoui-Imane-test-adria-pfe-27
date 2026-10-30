package com.adria.bank.account.repository;

import com.adria.bank.account.domain.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<BankAccount, UUID> {
    List<BankAccount> findByCustomerReference(String customerReference);
}

