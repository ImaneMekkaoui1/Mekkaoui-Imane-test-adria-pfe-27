package com.adria.bank.audit;

import com.adria.bank.audit.domain.AuditEvent;
import com.adria.bank.audit.repository.AuditEventRepository;
import com.adria.bank.audit.service.KafkaTransactionConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuditServiceTests {

    @Autowired
    private KafkaTransactionConsumer kafkaTransactionConsumer;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void testConsumeTransactionEventAndPersist() {
        UUID accountId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        String jsonPayload = String.format("""
                {
                  "eventId": "%s",
                  "eventType": "TransactionCreated",
                  "transactionId": "%s",
                  "accountId": "%s",
                  "amount": 250.00,
                  "currency": "EUR",
                  "occurredAt": "2026-01-15T10:30:00Z"
                }
                """, eventId, transactionId, accountId);

        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "bank.transactions.created", 0, 1L, accountId.toString(), jsonPayload
        );

        kafkaTransactionConsumer.consumeTransactionEvent(record);

        List<AuditEvent> events = auditEventRepository.findByAccountIdOrderByReceivedAtDesc(accountId);
        assertFalse(events.isEmpty());
        AuditEvent saved = events.get(0);
        assertEquals(eventId, saved.getEventId());
        assertEquals("TransactionCreated", saved.getEventType());
        assertEquals(transactionId, saved.getTransactionId());
        assertEquals(accountId, saved.getAccountId());
    }
}

