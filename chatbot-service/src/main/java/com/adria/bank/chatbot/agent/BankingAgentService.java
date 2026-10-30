package com.adria.bank.chatbot.agent;

import com.adria.bank.chatbot.domain.ChatMessageRequest;
import com.adria.bank.chatbot.domain.ChatMessageResponse;
import com.adria.bank.chatbot.mcp.McpClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankingAgentService {

    private final McpClientService mcpClientService;

    // UUID regex pattern
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "([a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12})"
    );
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?)");

    public ChatMessageResponse processUserMessage(ChatMessageRequest request) {
        String message = request.message().trim();
        String sessionId = request.sessionId() != null ? request.sessionId() : UUID.randomUUID().toString();

        log.info("Agent AI processing query from session [{}]: '{}'", sessionId, message);

        String lower = message.toLowerCase();

        // 1. Détection : Consultation de tous les comptes
        if (lower.contains("liste") && (lower.contains("compte") || lower.contains("comptes"))
                || lower.contains("tous les comptes")) {
            try {
                Object result = mcpClientService.callMcpTool("list_all_accounts", Collections.emptyMap());
                return new ChatMessageResponse(
                        sessionId,
                        "Voici la liste des comptes bancaires enregistrés : " + result,
                        "list_all_accounts",
                        result
                );
            } catch (Exception e) {
                return new ChatMessageResponse(sessionId, "Erreur lors de la récupération des comptes : " + e.getMessage(), "list_all_accounts", null);
            }
        }

        // 2. Détection : Virement bancaire (transfer)
        if (lower.contains("virement") || lower.contains("transfér") || lower.contains("transferer")) {
            List<String> uuids = extractUuids(message);
            Double amount = extractAmount(message);

            if (uuids.size() >= 2 && amount != null) {
                String src = uuids.get(0);
                String tgt = uuids.get(1);
                String currency = extractCurrency(message);

                Map<String, Object> args = Map.of(
                        "sourceAccountId", src,
                        "targetAccountId", tgt,
                        "amount", amount,
                        "currency", currency
                );

                try {
                    Object result = mcpClientService.callMcpTool("transfer_funds", args);
                    return new ChatMessageResponse(
                            sessionId,
                            String.format("✅ Virement de %.2f %s exécuté avec succès du compte %s vers le compte %s.",
                                    amount, currency, src, tgt),
                            "transfer_funds",
                            result
                    );
                } catch (Exception e) {
                    return new ChatMessageResponse(sessionId, "❌ Échec du virement : " + e.getMessage(), "transfer_funds", null);
                }
            } else {
                return new ChatMessageResponse(
                        sessionId,
                        "Pour effectuer un virement, veuillez préciser : l'UUID du compte source, l'UUID du compte destination et le montant (ex: 'Effectuer un virement de 100 EUR du compte <UUID1> vers <UUID2>').",
                        "none",
                        null
                );
            }
        }

        // 3. Détection : Consultation de solde / détail d'un compte
        if (lower.contains("solde") || lower.contains("consulter") || lower.contains("détail") || lower.contains("detail")) {
            List<String> uuids = extractUuids(message);
            if (!uuids.isEmpty()) {
                String accountId = uuids.get(0);
                try {
                    Object result = mcpClientService.callMcpTool("get_account_by_id", Map.of("id", accountId));
                    return new ChatMessageResponse(
                            sessionId,
                            "Voici les détails de votre compte : " + result,
                            "get_account_by_id",
                            result
                    );
                } catch (Exception e) {
                    return new ChatMessageResponse(sessionId, "❌ Impossible de récupérer le compte : " + e.getMessage(), "get_account_by_id", null);
                }
            } else {
                return new ChatMessageResponse(
                        sessionId,
                        "Veuillez indiquer l'identifiant UUID du compte bancaire dont vous souhaitez consulter le solde.",
                        "none",
                        null
                );
            }
        }

        // 4. Détection : Création de compte bancaire
        if (lower.contains("créer") || lower.contains("creer") || lower.contains("nouveau compte") || lower.contains("ouvrir")) {
            String customerRef = "CUST-" + (1000 + new Random().nextInt(9000));
            Pattern custPattern = Pattern.compile("(CUST-[A-Za-z0-9]+)");
            Matcher custMatcher = custPattern.matcher(message);
            if (custMatcher.find()) {
                customerRef = custMatcher.group(1);
            }

            Double amount = extractAmount(message);
            double initialBalance = amount != null ? amount : 0.0;
            String currency = extractCurrency(message);

            Map<String, Object> args = Map.of(
                    "customerReference", customerRef,
                    "currency", currency,
                    "initialBalance", initialBalance
            );

            try {
                Object result = mcpClientService.callMcpTool("create_bank_account", args);
                return new ChatMessageResponse(
                        sessionId,
                        String.format("🎉 Compte bancaire créé avec succès pour le client %s avec un solde initial de %.2f %s. Détails : %s",
                                customerRef, initialBalance, currency, result),
                        "create_bank_account",
                        result
                );
            } catch (Exception e) {
                return new ChatMessageResponse(sessionId, "❌ Erreur lors de la création du compte : " + e.getMessage(), "create_bank_account", null);
            }
        }

        // 5. Détection : Enregistrer une transaction / dépense
        if (lower.contains("transaction") || lower.contains("payer") || lower.contains("achat") || lower.contains("débit")) {
            List<String> uuids = extractUuids(message);
            Double amount = extractAmount(message);

            if (!uuids.isEmpty() && amount != null) {
                String accountId = uuids.get(0);
                String currency = extractCurrency(message);

                Map<String, Object> args = Map.of(
                        "accountId", accountId,
                        "amount", amount,
                        "currency", currency,
                        "merchantCategory", "RETAIL",
                        "country", "MA"
                );

                try {
                    Object result = mcpClientService.callMcpTool("create_transaction", args);
                    return new ChatMessageResponse(
                            sessionId,
                            String.format("💳 Transaction de %.2f %s enregistrée avec succès sur le compte %s. Détails : %s",
                                    amount, currency, accountId, result),
                            "create_transaction",
                            result
                    );
                } catch (Exception e) {
                    return new ChatMessageResponse(sessionId, "❌ Erreur transaction : " + e.getMessage(), "create_transaction", null);
                }
            }
        }

        // Réponse générale / aide
        return new ChatMessageResponse(
                sessionId,
                "Bonjour ! Je suis votre Assistant Bancaire Intelligent propulsé par des Microservices et MCP.\n" +
                        "Je peux vous aider à :\n" +
                        "• Consulter le solde d'un compte (ex: 'Solde du compte <UUID>')\n" +
                        "• Créer un nouveau compte bancaire (ex: 'Créer un compte pour CUST-1001 avec 500 EUR')\n" +
                        "• Effectuer un virement (ex: 'Effectuer un virement de 100 EUR de <UUID1> vers <UUID2>')\n" +
                        "• Enregistrer une transaction (ex: 'Payer 50 EUR avec le compte <UUID>')\n" +
                        "• Lister tous les comptes bancaires (ex: 'Lister tous les comptes')",
                "none",
                null
        );
    }

    private List<String> extractUuids(String text) {
        List<String> matches = new ArrayList<>();
        Matcher m = UUID_PATTERN.matcher(text);
        while (m.find()) {
            matches.add(m.group(1));
        }
        return matches;
    }

    private Double extractAmount(String text) {
        Matcher m = AMOUNT_PATTERN.matcher(text);
        while (m.find()) {
            try {
                double val = Double.parseDouble(m.group(1));
                if (val > 0) return val;
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private String extractCurrency(String text) {
        String upper = text.toUpperCase();
        if (upper.contains("USD")) return "USD";
        if (upper.contains("MAD")) return "MAD";
        if (upper.contains("GBP")) return "GBP";
        return "EUR"; // default
    }
}

