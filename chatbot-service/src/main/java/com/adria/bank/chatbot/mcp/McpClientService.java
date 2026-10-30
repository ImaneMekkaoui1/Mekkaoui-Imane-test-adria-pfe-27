package com.adria.bank.chatbot.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class McpClientService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${chatbot.account-service-url:http://localhost:8081}")
    private String accountServiceUrl;

    @Value("${chatbot.transaction-service-url:http://localhost:8082}")
    private String transactionServiceUrl;

    public List<Map<String, Object>> discoverAllTools() {
        List<Map<String, Object>> allTools = new ArrayList<>();

        // Discover tools from account-service
        try {
            String url = accountServiceUrl + "/api/mcp/tools";
            ResponseEntity<List<Map<String, Object>>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
            if (resp.getBody() != null) {
                resp.getBody().forEach(t -> {
                    Map<String, Object> tool = new HashMap<>(t);
                    tool.put("service", "account-service");
                    allTools.add(tool);
                });
            }
        } catch (Exception e) {
            log.warn("Unable to fetch MCP tools from account-service: {}", e.getMessage());
        }

        // Discover tools from transaction-service
        try {
            String url = transactionServiceUrl + "/api/mcp/tools";
            ResponseEntity<List<Map<String, Object>>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
            if (resp.getBody() != null) {
                resp.getBody().forEach(t -> {
                    Map<String, Object> tool = new HashMap<>(t);
                    tool.put("service", "transaction-service");
                    allTools.add(tool);
                });
            }
        } catch (Exception e) {
            log.warn("Unable to fetch MCP tools from transaction-service: {}", e.getMessage());
        }

        return allTools;
    }

    public Object callMcpTool(String toolName, Map<String, Object> arguments) {
        log.info("MCP Client calling tool [{}] with arguments: {}", toolName, arguments);

        String targetServiceUrl;
        if (toolName.contains("account")) {
            targetServiceUrl = accountServiceUrl;
        } else if (toolName.contains("transaction") || toolName.contains("transfer")) {
            targetServiceUrl = transactionServiceUrl;
        } else {
            // Default to account
            targetServiceUrl = accountServiceUrl;
        }

        String endpoint = targetServiceUrl + "/api/mcp/tools/" + toolName + "/call";
        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(arguments);
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, entity, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("result")) {
                return response.getBody().get("result");
            }
            return response.getBody();
        } catch (Exception e) {
            log.error("Error executing MCP tool [{}] on {}: {}", toolName, endpoint, e.getMessage());
            throw new RuntimeException("Erreur lors de l'appel du tool MCP [" + toolName + "] : " + e.getMessage(), e);
        }
    }
}

