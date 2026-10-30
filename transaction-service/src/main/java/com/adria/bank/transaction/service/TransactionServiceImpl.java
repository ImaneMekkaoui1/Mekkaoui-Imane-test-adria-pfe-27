package com.adria.bank.transaction.service;

import com.adria.bank.transaction.domain.*;
import com.adria.bank.transaction.repository.TransactionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final TransactionEventProducer transactionEventProducer;

    @Value("${bank.transaction.max-amount:1000.0}")
    private double maxAmount;

    @Override
    @CircuitBreaker(name = "accountService", fallbackMethod = "createTransactionFallback")
    public TransactionDTO createTransaction(CreateTransactionRequest request) {
        log.info("Processing transaction: accountId={}, amount={}, currency={}",
                request.accountId(), request.amount(), request.currency());

        // Règle 1 : Montant strictement positif
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Le montant de la transaction doit être strictement positif");
        }

        // Règle 1 (suite) : Respect du plafond configuré
        BigDecimal maxLimit = BigDecimal.valueOf(maxAmount);
        if (request.amount().compareTo(maxLimit) > 0) {
            throw new TransactionException("Le montant (" + request.amount() + " " + request.currency()
                    + ") dépasse le plafond autorisé de " + maxLimit);
        }

        // Règle 2 : Consultation du compte et vérification d'existence & statut actif
        AccountClientDTO account = accountServiceClient.getAccountById(request.accountId());
        if (account == null) {
            throw new TransactionException("Compte introuvable : " + request.accountId());
        }
        if (!"ACTIVE".equalsIgnoreCase(account.status())) {
            throw new TransactionException("Opération rejetée : le compte " + request.accountId() + " est " + account.status());
        }

        // Règle 3 : La devise doit correspondre à celle du compte
        if (!request.currency().equalsIgnoreCase(account.currency())) {
            throw new TransactionException("La devise de la transaction (" + request.currency()
                    + ") ne correspond pas à la devise du compte (" + account.currency() + ")");
        }

        // Règle 4 : Vérification du solde suffisant (pour un débit bancaire)
        double currentBalance = account.balance();
        if (currentBalance < request.amount().doubleValue()) {
            throw new TransactionException("Solde insuffisant (" + currentBalance + " " + account.currency()
                    + ") pour exécuter la transaction de " + request.amount());
        }

        // Persistance de la transaction
        BankTransaction transaction = BankTransaction.builder()
                .id(UUID.randomUUID())
                .accountId(request.accountId())
                .amount(request.amount())
                .currency(request.currency().toUpperCase())
                .merchantCategory(request.merchantCategory())
                .country(request.country())
                .createdAt(Instant.now())
                .build();

        BankTransaction saved = transactionRepository.save(transaction);
        log.info("Transaction persisted successfully with id: {}", saved.getId());

        // Règle 4 (suite) : Mise à jour du solde du compte après enregistrement
        accountServiceClient.updateBalance(
                request.accountId(),
                new UpdateBalanceClientRequest(request.amount().doubleValue(), "DEBIT")
        );

        // Partie F : Publication de l'événement Kafka
        TransactionCreatedEvent event = TransactionCreatedEvent.of(saved);
        transactionEventProducer.publishTransactionCreated(event);

        return TransactionDTO.fromEntity(saved);
    }

    /**
     * Fallback Resilience4J pour createTransaction
     */
    public TransactionDTO createTransactionFallback(CreateTransactionRequest request, Throwable ex) {
        log.error("Resilience4J Fallback triggered for createTransaction: {}", ex.getMessage());
        throw new TransactionException("Impossible d'enregistrer la transaction (Account Service indisponible / Circuit Breaker): " + ex.getMessage());
    }

    @Override
    @CircuitBreaker(name = "accountService", fallbackMethod = "transferFallback")
    public TransferResult transfer(TransferRequest request) {
        log.info("Executing transfer: from {} to {}, amount={} {}",
                request.sourceAccountId(), request.targetAccountId(), request.amount(), request.currency());

        if (request.sourceAccountId().equals(request.targetAccountId())) {
            throw new TransactionException("Le compte source et le compte destination doivent être différents");
        }

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Le montant du virement doit être strictement positif");
        }

        BigDecimal maxLimit = BigDecimal.valueOf(maxAmount);
        if (request.amount().compareTo(maxLimit) > 0) {
            throw new TransactionException("Le montant du virement (" + request.amount()
                    + ") dépasse le plafond autorisé de " + maxLimit);
        }

        // Vérifier compte source
        AccountClientDTO source = accountServiceClient.getAccountById(request.sourceAccountId());
        if (source == null || !"ACTIVE".equalsIgnoreCase(source.status())) {
            throw new TransactionException("Compte source invalide ou inactif : " + request.sourceAccountId());
        }
        if (!request.currency().equalsIgnoreCase(source.currency())) {
            throw new TransactionException("Devise incompatible avec le compte source (" + source.currency() + ")");
        }
        if (source.balance() < request.amount().doubleValue()) {
            throw new TransactionException("Solde insuffisant sur le compte source (" + source.balance() + ")");
        }

        // Vérifier compte cible
        AccountClientDTO target = accountServiceClient.getAccountById(request.targetAccountId());
        if (target == null || !"ACTIVE".equalsIgnoreCase(target.status())) {
            throw new TransactionException("Compte destinataire invalide ou inactif : " + request.targetAccountId());
        }
        if (!request.currency().equalsIgnoreCase(target.currency())) {
            throw new TransactionException("Devise incompatible avec le compte destinataire (" + target.currency() + ")");
        }

        // Débit compte source
        accountServiceClient.updateBalance(
                request.sourceAccountId(),
                new UpdateBalanceClientRequest(request.amount().doubleValue(), "DEBIT")
        );

        // Crédit compte cible
        accountServiceClient.updateBalance(
                request.targetAccountId(),
                new UpdateBalanceClientRequest(request.amount().doubleValue(), "CREDIT")
        );

        // Enregistrer transaction débit source
        BankTransaction txDebit = transactionRepository.save(BankTransaction.builder()
                .id(UUID.randomUUID())
                .accountId(request.sourceAccountId())
                .amount(request.amount())
                .currency(request.currency().toUpperCase())
                .merchantCategory("TRANSFER_DEBIT")
                .country("MA")
                .createdAt(Instant.now())
                .build());

        // Enregistrer transaction crédit destination
        BankTransaction txCredit = transactionRepository.save(BankTransaction.builder()
                .id(UUID.randomUUID())
                .accountId(request.targetAccountId())
                .amount(request.amount())
                .currency(request.currency().toUpperCase())
                .merchantCategory("TRANSFER_CREDIT")
                .country("MA")
                .createdAt(Instant.now())
                .build());

        // Publier les événements Kafka
        transactionEventProducer.publishTransactionCreated(TransactionCreatedEvent.of(txDebit));
        transactionEventProducer.publishTransactionCreated(TransactionCreatedEvent.of(txCredit));

        return new TransferResult(
                txDebit.getId(),
                txCredit.getId(),
                "SUCCESS",
                "Virement de " + request.amount() + " " + request.currency() + " exécuté avec succès"
        );
    }

    public TransferResult transferFallback(TransferRequest request, Throwable ex) {
        log.error("Resilience4J Fallback triggered for transfer: {}", ex.getMessage());
        throw new TransactionException("Impossible d'exécuter le virement (Account Service indisponible): " + ex.getMessage());
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDTO getTransactionById(UUID id) {
        BankTransaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionException("Transaction non trouvée : " + id));
        return TransactionDTO.fromEntity(tx);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDTO> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(TransactionDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsByAccountId(UUID accountId) {
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId).stream()
                .map(TransactionDTO::fromEntity)
                .toList();
    }
}

