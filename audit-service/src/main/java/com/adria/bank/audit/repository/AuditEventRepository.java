package com.adria.bank.audit.repository;

import com.adria.bank.audit.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findByAccountIdOrderByReceivedAtDesc(UUID accountId);
}

