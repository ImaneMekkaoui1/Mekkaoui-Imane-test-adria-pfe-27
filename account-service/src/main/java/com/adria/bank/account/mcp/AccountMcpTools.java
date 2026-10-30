package com.adria.bank.account.mcp;

import com.adria.bank.account.domain.AccountDTO;
import com.adria.bank.account.domain.AccountStatus;
import com.adria.bank.account.domain.CreateAccountRequest;
import com.adria.bank.account.domain.UpdateBalanceRequest;
import com.adria.bank.account.service.AccountService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountMcpTools {

    private final AccountService accountService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Map<String, Object>> getToolDefinitions() {
        List<Map<String, Object>> tools = new ArrayList<>();

        // Tool 1: get_account_by_id
        tools.add(Map.of(
                "name", "get_account_by_id",
                "description", "Récupère les détails et le solde d'un compte bancaire via son identifiant UUID",
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "id", Map.of("type", "string", "description", "Identifiant UUID du compte bancaire")
                        ),
                        "required", List.of("id")
                )
        ));

        // Tool 2: list_all_accounts
        tools.add(Map.of(
                "name", "list_all_accounts",
                "description", "Liste l'ensemble des comptes bancaires enregistrés dans la banque",
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Collections.emptyMap()
                )
        ));

        // Tool 3: create_bank_account
        tools.add(Map.of(
                "name", "create_bank_account",
                "description", "Crée un nouveau compte bancaire avec une référence client, une devise et un solde initial",
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "customerReference", Map.of("type", "string", "description", "Référence unique du client (ex: CUST-1001)"),
                                "currency", Map.of("type", "string", "description", "Code devise ISO sur 3 lettres (ex: EUR, USD, MAD)"),
                                "initialBalance", Map.of("type", "number", "description", "Solde initial du compte (par défaut 0.0)")
                        ),
                        "required", List.of("customerReference", "currency")
                )
        ));

        // Tool 4: update_account_balance
        tools.add(Map.of(
                "name", "update_account_balance",
                "description", "Crédite ou débite le solde d'un compte bancaire existant",
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "accountId", Map.of("type", "string", "description", "Identifiant UUID du compte"),
                                "amount", Map.of("type", "number", "description", "Montant strictement positif"),
                                "operationType", Map.of("type", "string", "enum", List.of("CREDIT", "DEBIT"), "description", "Type d'opération : CREDIT ou DEBIT")
                        ),
                        "required", List.of("accountId", "amount", "operationType")
                )
        ));

        // Tool 5: change_account_status
        tools.add(Map.of(
                "name", "change_account_status",
                "description", "Modifie le statut d'un compte bancaire (ACTIVE ou BLOCKED)",
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "accountId", Map.of("type", "string", "description", "Identifiant UUID du compte"),
                                "status", Map.of("type", "string", "enum", List.of("ACTIVE", "BLOCKED"), "description", "Nouveau statut : ACTIVE ou BLOCKED")
                        ),
                        "required", List.of("accountId", "status")
                )
        ));

        return tools;
    }

    public Object executeTool(String toolName, Map<String, Object> arguments) {
        log.info("Executing MCP Tool: {} with arguments: {}", toolName, arguments);
        try {
            switch (toolName) {
                case "get_account_by_id" -> {
                    UUID id = UUID.fromString((String) arguments.get("id"));
                    return accountService.getAccountById(id);
                }
                case "list_all_accounts" -> {
                    return accountService.getAllAccounts();
                }
                case "create_bank_account" -> {
                    String customerRef = (String) arguments.get("customerReference");
                    String currency = (String) arguments.get("currency");
                    double initialBalance = arguments.containsKey("initialBalance")
                            ? ((Number) arguments.get("initialBalance")).doubleValue()
                            : 0.0;
                    CreateAccountRequest request = new CreateAccountRequest(customerRef, currency, initialBalance);
                    return accountService.createAccount(request);
                }
                case "update_account_balance" -> {
                    UUID accountId = UUID.fromString((String) arguments.get("accountId"));
                    double amount = ((Number) arguments.get("amount")).doubleValue();
                    String operationType = (String) arguments.get("operationType");
                    UpdateBalanceRequest request = new UpdateBalanceRequest(amount, operationType);
                    return accountService.updateBalance(accountId, request);
                }
                case "change_account_status" -> {
                    UUID accountId = UUID.fromString((String) arguments.get("accountId"));
                    AccountStatus status = AccountStatus.valueOf((String) arguments.get("status"));
                    return accountService.changeStatus(accountId, status);
                }
                default -> throw new IllegalArgumentException("Unknown MCP tool: " + toolName);
            }
        } catch (Exception e) {
            log.error("Error executing MCP tool {}: {}", toolName, e.getMessage());
            throw new RuntimeException("MCP Tool Error [" + toolName + "]: " + e.getMessage(), e);
        }
    }
}

