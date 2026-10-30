package com.adria.bank.transaction.mcp;

import com.adria.bank.transaction.domain.CreateTransactionRequest;
import com.adria.bank.transaction.domain.TransferRequest;
import com.adria.bank.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionMcpTools {

    private final TransactionService transactionService;

    public List<Map<String, Object>> getToolDefinitions() {
        List<Map<String, Object>> tools = new ArrayList<>();

        // Tool 1: create_transaction
        tools.add(Map.of(
                "name", "create_transaction",
                "description", "Enregistre une nouvelle transaction bancaire (débit de compte) avec vérification de solde, devise et plafond",
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "accountId", Map.of("type", "string", "description", "UUID du compte bancaire débité"),
                                "amount", Map.of("type", "number", "description", "Montant de la transaction (positif, <= plafond)"),
                                "currency", Map.of("type", "string", "description", "Code devise (ex: EUR)"),
                                "merchantCategory", Map.of("type", "string", "description", "Catégorie commerçant (ex: RETAIL, GROCERY, TRAVEL)"),
                                "country", Map.of("type", "string", "description", "Code pays (ex: MA, FR)")
                        ),
                        "required", List.of("accountId", "amount", "currency")
                )
        ));

        // Tool 2: transfer_funds
        tools.add(Map.of(
                "name", "transfer_funds",
                "description", "Effectue un virement bancaire entre deux comptes bancaires",
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "sourceAccountId", Map.of("type", "string", "description", "UUID du compte émetteur"),
                                "targetAccountId", Map.of("type", "string", "description", "UUID du compte bénéficiaire"),
                                "amount", Map.of("type", "number", "description", "Montant du virement"),
                                "currency", Map.of("type", "string", "description", "Devise du virement (ex: EUR)")
                        ),
                        "required", List.of("sourceAccountId", "targetAccountId", "amount", "currency")
                )
        ));

        // Tool 3: get_transaction_by_id
        tools.add(Map.of(
                "name", "get_transaction_by_id",
                "description", "Récupère les détails d'une transaction bancaire via son UUID",
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "id", Map.of("type", "string", "description", "UUID de la transaction")
                        ),
                        "required", List.of("id")
                )
        ));

        // Tool 4: get_transactions_by_account
        tools.add(Map.of(
                "name", "get_transactions_by_account",
                "description", "Consulte l'historique des transactions d'un compte bancaire donné",
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "accountId", Map.of("type", "string", "description", "UUID du compte bancaire")
                        ),
                        "required", List.of("accountId")
                )
        ));

        return tools;
    }

    public Object executeTool(String toolName, Map<String, Object> arguments) {
        log.info("Executing Transaction MCP Tool: {} with arguments: {}", toolName, arguments);
        try {
            switch (toolName) {
                case "create_transaction" -> {
                    UUID accountId = UUID.fromString((String) arguments.get("accountId"));
                    BigDecimal amount = BigDecimal.valueOf(((Number) arguments.get("amount")).doubleValue());
                    String currency = (String) arguments.get("currency");
                    String merchant = (String) arguments.getOrDefault("merchantCategory", "GENERAL");
                    String country = (String) arguments.getOrDefault("country", "MA");

                    CreateTransactionRequest req = new CreateTransactionRequest(accountId, amount, currency, merchant, country);
                    return transactionService.createTransaction(req);
                }
                case "transfer_funds" -> {
                    UUID srcId = UUID.fromString((String) arguments.get("sourceAccountId"));
                    UUID tgtId = UUID.fromString((String) arguments.get("targetAccountId"));
                    BigDecimal amount = BigDecimal.valueOf(((Number) arguments.get("amount")).doubleValue());
                    String currency = (String) arguments.get("currency");

                    TransferRequest req = new TransferRequest(srcId, tgtId, amount, currency);
                    return transactionService.transfer(req);
                }
                case "get_transaction_by_id" -> {
                    UUID id = UUID.fromString((String) arguments.get("id"));
                    return transactionService.getTransactionById(id);
                }
                case "get_transactions_by_account" -> {
                    UUID accId = UUID.fromString((String) arguments.get("accountId"));
                    return transactionService.getTransactionsByAccountId(accId);
                }
                default -> throw new IllegalArgumentException("Unknown Transaction MCP Tool: " + toolName);
            }
        } catch (Exception e) {
            log.error("Error executing Transaction MCP tool {}: {}", toolName, e.getMessage());
            throw new RuntimeException("Transaction MCP Tool Error [" + toolName + "]: " + e.getMessage(), e);
        }
    }
}

