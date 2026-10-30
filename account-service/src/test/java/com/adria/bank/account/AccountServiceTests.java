package com.adria.bank.account;

import com.adria.bank.account.domain.AccountDTO;
import com.adria.bank.account.domain.AccountStatus;
import com.adria.bank.account.domain.CreateAccountRequest;
import com.adria.bank.account.domain.UpdateBalanceRequest;
import com.adria.bank.account.mcp.AccountMcpTools;
import com.adria.bank.account.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AccountServiceTests {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountMcpTools accountMcpTools;

    @Test
    void testCreateAndGetAccount() {
        CreateAccountRequest req = new CreateAccountRequest("CUST-TEST-1", "EUR", 1000.0);
        AccountDTO created = accountService.createAccount(req);

        assertNotNull(created.id());
        assertEquals("CUST-TEST-1", created.customerReference());
        assertEquals(AccountStatus.ACTIVE, created.status());
        assertEquals(1000.0, created.balance());

        AccountDTO fetched = accountService.getAccountById(created.id());
        assertEquals(created.id(), fetched.id());
        assertEquals(1000.0, fetched.balance());
    }

    @Test
    void testUpdateBalanceDebitAndCredit() {
        CreateAccountRequest req = new CreateAccountRequest("CUST-TEST-2", "EUR", 500.0);
        AccountDTO acc = accountService.createAccount(req);

        // Debit 200
        AccountDTO debited = accountService.updateBalance(acc.id(), new UpdateBalanceRequest(200.0, "DEBIT"));
        assertEquals(300.0, debited.balance());

        // Credit 150
        AccountDTO credited = accountService.updateBalance(acc.id(), new UpdateBalanceRequest(150.0, "CREDIT"));
        assertEquals(450.0, credited.balance());
    }

    @Test
    void testMcpToolsExecution() {
        // List tools
        var tools = accountMcpTools.getToolDefinitions();
        assertFalse(tools.isEmpty());

        // Call list_all_accounts through MCP tool
        Object listResult = accountMcpTools.executeTool("list_all_accounts", Map.of());
        assertNotNull(listResult);

        // Call create_bank_account through MCP tool
        Object createResult = accountMcpTools.executeTool("create_bank_account", Map.of(
                "customerReference", "CUST-MCP-1",
                "currency", "USD",
                "initialBalance", 800.0
        ));
        assertNotNull(createResult);
    }
}

