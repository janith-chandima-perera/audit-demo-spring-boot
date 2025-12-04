package com.chandima.tutor.audit.repository;

import com.chandima.tutor.audit.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Standard Spring Data JPA repository for the AuditLog entity.
 *
 * NOTE FOR LEARNERS:
 * Like the ProductRepository, this is left empty to rely on the powerful defaults
 * provided by Spring Data JPA. You could add methods here to search the audit logs,
 * for instance, to find all changes for a specific entity:
 * `List<AuditLog> findByEntityNameAndEntityIdOrderByTimestampDesc(String entityName, String entityId);`
 */
// @Repository: Marks this interface as a Spring-managed repository bean.
@Repository
// JpaRepository<AuditLog, Long>: Provides generic CRUD operations for the AuditLog entity.
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
