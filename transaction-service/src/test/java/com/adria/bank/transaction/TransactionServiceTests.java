package com.adria.bank.transaction;

import com.adria.bank.transaction.domain.*;
import com.adria.bank.transaction.mcp.TransactionMcpTools;
import com.adria.bank.transaction.service.AccountServiceClient;
import com.adria.bank.transaction.service.TransactionEventProducer;
import com.adria.bank.transaction.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
class TransactionServiceTests {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionMcpTools transactionMcpTools;

    @MockBean
    private AccountServiceClient accountServiceClient;

    @MockBean
    private TransactionEventProducer transactionEventProducer;

    private UUID activeAccountId;
    private UUID blockedAccountId;

    @BeforeEach
    void setUp() {
        activeAccountId = UUID.randomUUID();
        blockedAccountId = UUID.randomUUID();

        when(accountServiceClient.getAccountById(activeAccountId))
                .thenReturn(new AccountClientDTO(activeAccountId, "CUST-1", "ACTIVE", "EUR", 2000.0));

        when(accountServiceClient.getAccountById(blockedAccountId))
                .thenReturn(new AccountClientDTO(blockedAccountId, "CUST-2", "BLOCKED", "EUR", 2000.0));

        when(accountServiceClient.updateBalance(any(), any()))
                .thenReturn(new AccountClientDTO(activeAccountId, "CUST-1", "ACTIVE", "EUR", 1800.0));
    }

    @Test
    void testCreateValidTransaction() {
        CreateTransactionRequest req = new CreateTransactionRequest(
                activeAccountId,
                BigDecimal.valueOf(200.00),
                "EUR",
                "RETAIL",
                "FR"
        );

        TransactionDTO result = transactionService.createTransaction(req);

        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals(activeAccountId, result.accountId());
        assertEquals(BigDecimal.valueOf(200.00), result.amount());
        assertEquals("EUR", result.currency());

        // Verify balance updated and event published
        verify(accountServiceClient, times(1)).updateBalance(eq(activeAccountId), any());
        verify(transactionEventProducer, times(1)).publishTransactionCreated(any());
    }

    @Test
    void testExceedMaxAmountShouldFail() {
        CreateTransactionRequest req = new CreateTransactionRequest(
                activeAccountId,
                BigDecimal.valueOf(1500.00), // Exceeds default limit of 1000
                "EUR",
                "LUXURY",
                "FR"
        );

        assertThrows(TransactionException.class, () -> transactionService.createTransaction(req));
    }

    @Test
    void testBlockedAccountShouldFail() {
        CreateTransactionRequest req = new CreateTransactionRequest(
                blockedAccountId,
                BigDecimal.valueOf(100.00),
                "EUR",
                "RETAIL",
                "FR"
        );

        assertThrows(TransactionException.class, () -> transactionService.createTransaction(req));
    }

    @Test
    void testCurrencyMismatchShouldFail() {
        CreateTransactionRequest req = new CreateTransactionRequest(
                activeAccountId,
                BigDecimal.valueOf(100.00),
                "USD", // Mismatch with EUR
                "RETAIL",
                "US"
        );

        assertThrows(TransactionException.class, () -> transactionService.createTransaction(req));
    }

    @Test
    void testMcpToolsExecution() {
        var tools = transactionMcpTools.getToolDefinitions();
        assertFalse(tools.isEmpty());

        Object result = transactionMcpTools.executeTool("create_transaction", Map.of(
                "accountId", activeAccountId.toString(),
                "amount", 50.0,
                "currency", "EUR",
                "merchantCategory", "CAFE",
                "country", "MA"
        ));

        assertNotNull(result);
    }
}

