package com.adria.bank.account;

import com.adria.bank.account.domain.AccountStatus;
import com.adria.bank.account.domain.BankAccount;
import com.adria.bank.account.repository.AccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

import java.util.UUID;

@SpringBootApplication
@EnableDiscoveryClient
@Slf4j
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner initData(AccountRepository accountRepository) {
        return args -> {
            // Seed a few demo accounts if none exist
            if (accountRepository.count() == 0) {
                // Fixed known UUIDs for predictable testing
                UUID acc1Id = UUID.fromString("11111111-1111-1111-1111-111111111111");
                UUID acc2Id = UUID.fromString("22222222-2222-2222-2222-222222222222");
                UUID accBlockedId = UUID.fromString("99999999-9999-9999-9999-999999999999");

                accountRepository.save(BankAccount.builder()
                        .id(acc1Id)
                        .customerReference("CUST-1001")
                        .status(AccountStatus.ACTIVE)
                        .currency("EUR")
                        .balance(5000.00)
                        .build());

                accountRepository.save(BankAccount.builder()
                        .id(acc2Id)
                        .customerReference("CUST-1002")
                        .status(AccountStatus.ACTIVE)
                        .currency("EUR")
                        .balance(1500.00)
                        .build());

                accountRepository.save(BankAccount.builder()
                        .id(accBlockedId)
                        .customerReference("CUST-1003")
                        .status(AccountStatus.BLOCKED)
                        .currency("EUR")
                        .balance(200.00)
                        .build());

                log.info("Initialized demo accounts: {}, {}, {}", acc1Id, acc2Id, accBlockedId);
            }
        };
    }
}

