package com.adria.bank.account.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AccountMcpController {

    private final AccountMcpTools accountMcpTools;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Standard MCP JSON-RPC 2.0 endpoint (tools/list and tools/call)
     */
    @PostMapping(value = {"/mcp", "/mcp/accounts"})
    public ResponseEntity<Map<String, Object>> handleMcpJsonRpc(@RequestBody Map<String, Object> request) {
        String jsonrpc = (String) request.getOrDefault("jsonrpc", "2.0");
        Object id = request.get("id");
        String method = (String) request.get("method");

        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", jsonrpc);
        response.put("id", id);

        try {
            if ("tools/list".equals(method)) {
                Map<String, Object> result = new HashMap<>();
                result.put("tools", accountMcpTools.getToolDefinitions());
                response.put("result", result);
            } else if ("tools/call".equals(method)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> params = (Map<String, Object>) request.get("params");
                String name = (String) params.get("name");
                @SuppressWarnings("unchecked")
                Map<String, Object> arguments = (Map<String, Object>) params.getOrDefault("arguments", Collections.emptyMap());

                Object output = accountMcpTools.executeTool(name, arguments);
                String textResult = objectMapper.writeValueAsString(output);

                Map<String, Object> result = new HashMap<>();
                result.put("content", List.of(Map.of("type", "text", "text", textResult)));
                result.put("isError", false);
                response.put("result", result);
            } else {
                response.put("error", Map.of("code", -32601, "message", "Method not found: " + method));
            }
        } catch (Exception e) {
            log.error("MCP RPC error: {}", e.getMessage());
            response.put("error", Map.of("code", -32000, "message", e.getMessage()));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * REST endpoint to list tools
     */
    @GetMapping("/api/mcp/tools")
    public ResponseEntity<List<Map<String, Object>>> listTools() {
        return ResponseEntity.ok(accountMcpTools.getToolDefinitions());
    }

    /**
     * REST endpoint to call an MCP tool directly
     */
    @PostMapping("/api/mcp/tools/{toolName}/call")
    public ResponseEntity<Map<String, Object>> callTool(
            @PathVariable String toolName,
            @RequestBody(required = false) Map<String, Object> arguments) {
        if (arguments == null) {
            arguments = Collections.emptyMap();
        }
        Object result = accountMcpTools.executeTool(toolName, arguments);
        return ResponseEntity.ok(Map.of("tool", toolName, "result", result));
    }
}

