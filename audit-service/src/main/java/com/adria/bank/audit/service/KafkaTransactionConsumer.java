package com.adria.bank.audit.service;

import com.adria.bank.audit.domain.AuditEvent;
import com.adria.bank.audit.domain.TransactionCreatedEvent;
import com.adria.bank.audit.repository.AuditEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaTransactionConsumer {

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @KafkaListener(
            topics = "${bank.kafka.topic.transaction-created:bank.transactions.created}",
            groupId = "${spring.kafka.consumer.group-id:audit-group}"
    )
    public void consumeTransactionEvent(ConsumerRecord<String, String> record) {
        String key = record.key();
        String payload = record.value();

        log.info("================================================================================");
        log.info("[AUDIT KAFKA CONSUMER] Event received from partition {} with offset {}",
                record.partition(), record.offset());
        log.info("[AUDIT KAFKA CONSUMER] Message Key (accountId): {}", key);
        log.info("[AUDIT KAFKA CONSUMER] Raw Payload: {}", payload);

        try {
            TransactionCreatedEvent event = objectMapper.readValue(payload, TransactionCreatedEvent.class);

            log.info("[AUDIT EVENT LOG] eventId={}, eventType={}, transactionId={}, accountId={}, amount={} {}, occurredAt={}",
                    event.eventId(),
                    event.eventType(),
                    event.transactionId(),
                    event.accountId(),
                    event.amount(),
                    event.currency(),
                    event.occurredAt());

            // Sauvegarde dans la base d'audit sans modifier aucune donnée bancaire métier
            AuditEvent auditEvent = AuditEvent.builder()
                    .id(UUID.randomUUID())
                    .eventId(event.eventId())
                    .eventType(event.eventType())
                    .transactionId(event.transactionId())
                    .accountId(event.accountId())
                    .amount(event.amount())
                    .currency(event.currency())
                    .occurredAt(event.occurredAt())
                    .receivedAt(Instant.now())
                    .build();

            auditEventRepository.save(auditEvent);
            log.info("[AUDIT SAVED] Audit record successfully persisted in audit database.");
            log.info("================================================================================");

        } catch (Exception e) {
            log.error("[AUDIT ERROR] Error deserializing or saving audit event: {}", e.getMessage(), e);
        }
    }
}

