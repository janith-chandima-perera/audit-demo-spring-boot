package com.chandima.tutor.audit.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents an audit log entry for entity changes.
 * This entity will store the "what, when, who, and how" of each change.
 *
 * NOTE FOR LEARNERS:
 * 1.  `changesJson`: Storing changes as a JSON string is a common and flexible approach.
 *     For more advanced querying capabilities, some might choose a proper JSONB type if the
 *     database supports it, or even a separate `AuditChange` entity (a one-to-many relationship)
 *     to store each changed field in its own row. We use a simple String to remain database-agnostic
 *     and avoid extra complexity.
 * 2.  `changedBy`: This is a simple String. In a real application with security, this would likely
 *     be a foreign key to a `User` entity. We've hardcoded it to "system" to avoid needing a
 *     full security implementation for this example.
 * 3.  Indexing: For performance, columns like `entityName`, `entityId`, and `timestamp` would
 *     typically be indexed (`@Index`) to allow for efficient querying of the audit history.
 */
// @Entity: Specifies that this class is a JPA entity, mapped to a database table (e.g., "audit_log").
@Entity
// @Data: Lombok's annotation to auto-generate getters, setters, toString(), etc.
@Data
// @Builder: A Lombok annotation that implements the Builder design pattern for this class.
// This allows for clean and readable object creation, e.g., AuditLog.builder().entityName("...").build();
@Builder
// @NoArgsConstructor: Generates a no-argument constructor, required by JPA and other frameworks.
@NoArgsConstructor
// @AllArgsConstructor: Generates a constructor with all fields as arguments.
@AllArgsConstructor
public class AuditLog {

    // @Id: Marks this field as the primary key.
    @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY): Tells the database to auto-generate this value.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entityName;
    private String entityId;
    private String action; // e.g., "UPDATE", "CREATE"
    private String changedBy; // In a real app, this would come from Spring Security Context
    private LocalDateTime timestamp;

    /**
     * Stores the actual changes as a JSON string.
     * Example: {"price": "100.0 -> 120.0", "name": "OldName -> NewName"}
     */
    private String changesJson;
}
