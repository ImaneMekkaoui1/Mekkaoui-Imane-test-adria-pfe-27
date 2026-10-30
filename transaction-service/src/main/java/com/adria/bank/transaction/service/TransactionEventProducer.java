package com.adria.bank.transaction.service;

import com.adria.bank.transaction.domain.TransactionCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${bank.kafka.topic.transaction-created:bank.transactions.created}")
    private String topic;

    public void publishTransactionCreated(TransactionCreatedEvent event) {
        String key = event.accountId().toString();
        log.info("Publishing TransactionCreatedEvent to topic [{}] with key [{}]: {}", topic, key, event);
        try {
            kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Kafka: Event [{}] sent successfully to partition [{}] with offset [{}]",
                            event.eventId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Kafka: Failed to send event [{}] to topic [{}]: {}",
                            event.eventId(), topic, ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("Kafka broker unreachable or error sending event [{}]: {}", event.eventId(), e.getMessage());
        }
    }
}

