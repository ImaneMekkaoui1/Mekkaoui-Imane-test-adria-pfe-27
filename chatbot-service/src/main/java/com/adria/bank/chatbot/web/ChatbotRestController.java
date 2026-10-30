package com.adria.bank.chatbot.web;

import com.adria.bank.chatbot.agent.BankingAgentService;
import com.adria.bank.chatbot.domain.ChatMessageRequest;
import com.adria.bank.chatbot.domain.ChatMessageResponse;
import com.adria.bank.chatbot.mcp.McpClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatbotRestController {

    private final BankingAgentService bankingAgentService;
    private final McpClientService mcpClientService;

    @PostMapping
    public ResponseEntity<ChatMessageResponse> chat(@RequestBody ChatMessageRequest request) {
        ChatMessageResponse response = bankingAgentService.processUserMessage(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tools")
    public ResponseEntity<List<Map<String, Object>>> getTools() {
        return ResponseEntity.ok(mcpClientService.discoverAllTools());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "chatbot-service",
                "role", "Agentic Banking Chatbot with MCP & Spring AI"
        ));
    }
}

