package com.adria.bank.chatbot;

import com.adria.bank.chatbot.agent.BankingAgentService;
import com.adria.bank.chatbot.domain.ChatMessageRequest;
import com.adria.bank.chatbot.domain.ChatMessageResponse;
import com.adria.bank.chatbot.mcp.McpClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
class ChatbotServiceTests {

    @Autowired
    private BankingAgentService bankingAgentService;

    @MockBean
    private McpClientService mcpClientService;

    @Test
    void testHelpMessage() {
        ChatMessageRequest request = new ChatMessageRequest("bonjour, que peux-tu faire ?", "sess-1");
        ChatMessageResponse resp = bankingAgentService.processUserMessage(request);

        assertNotNull(resp);
        assertEquals("sess-1", resp.sessionId());
        assertTrue(resp.reply().contains("Assistant Bancaire Intelligent"));
    }

    @Test
    void testConsultBalanceIntent() {
        UUID testAccountId = UUID.randomUUID();
        when(mcpClientService.callMcpTool(eq("get_account_by_id"), any()))
                .thenReturn(Map.of("id", testAccountId.toString(), "balance", 1250.0, "currency", "EUR"));

        ChatMessageRequest request = new ChatMessageRequest("Quel est le solde du compte " + testAccountId + " ?", "sess-2");
        ChatMessageResponse resp = bankingAgentService.processUserMessage(request);

        assertNotNull(resp);
        assertEquals("get_account_by_id", resp.executedTool());
        assertTrue(resp.reply().contains("détails de votre compte"));
    }

    @Test
    void testTransferMoneyIntent() {
        UUID srcId = UUID.randomUUID();
        UUID tgtId = UUID.randomUUID();

        when(mcpClientService.callMcpTool(eq("transfer_funds"), any()))
                .thenReturn(Map.of("status", "SUCCESS"));

        String msg = String.format("Virement de 150 EUR depuis le compte %s vers le compte %s", srcId, tgtId);
        ChatMessageRequest request = new ChatMessageRequest(msg, "sess-3");
        ChatMessageResponse resp = bankingAgentService.processUserMessage(request);

        assertNotNull(resp);
        assertEquals("transfer_funds", resp.executedTool());
        assertTrue(resp.reply().contains("Virement de 150.00 EUR exécuté"));
    }
}

