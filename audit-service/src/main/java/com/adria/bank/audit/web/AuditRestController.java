package com.adria.bank.audit.web;

import com.adria.bank.audit.domain.AuditEvent;
import com.adria.bank.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditRestController {

    private final AuditEventRepository auditEventRepository;

    @GetMapping("/events")
    public ResponseEntity<List<AuditEvent>> getAllEvents() {
        return ResponseEntity.ok(auditEventRepository.findAll());
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<AuditEvent> getEventById(@PathVariable UUID id) {
        return auditEventRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/events/by-account/{accountId}")
    public ResponseEntity<List<AuditEvent>> getEventsByAccount(@PathVariable UUID accountId) {
        return ResponseEntity.ok(auditEventRepository.findByAccountIdOrderByReceivedAtDesc(accountId));
    }
}

