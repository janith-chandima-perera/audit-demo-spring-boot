package com.chandima.tutor.audit.service;

import com.chandima.tutor.audit.event.EntityModifiedEvent;
import com.chandima.tutor.audit.model.AuditLog;
import com.chandima.tutor.audit.repository.AuditLogRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JsonProcessingException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

/**
 * This service consumes the entity modification events.
 *
 * NOTE FOR LEARNERS:
 * This class is the final piece that makes our system "event-driven" and resilient.
 * 1.  `@EventListener`: This is a standard Spring annotation that subscribes this method to
 *     `EntityModifiedEvent`s. It's simpler than the previous `@TransactionalEventListener`.
 * 2.  `@Transactional(propagation = Propagation.REQUIRES_NEW)`: This is a critical concept. It
 *     ensures that the audit log is saved in a completely separate transaction from the one that
 *     triggered it (e.g., the product update). This means if saving the audit log fails, it will
 *     NOT cause the main product update to roll back. This is a key pattern for decoupling
 *     non-critical tasks like logging.
 * 3.  Error Handling & Logging: The `catch` block currently just prints to the console (`System.err`).
 *     In a real application, this is where you would use a dedicated logging framework like SLF4J
 *     with Logback or Log4j2 to write a structured error log. This is crucial for monitoring and
 *     debugging in a production environment.
 */
// @Service: Marks this class as a Spring Service bean, indicating it contains business logic.
@Service
public class AuditConsumerService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper; // For converting the changes map to a JSON string

    public AuditConsumerService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Listens for EntityModifiedEvent events.
     *
     * @param event The event containing details of the entity modification.
     */
    // @EventListener: A Spring annotation that marks this method as a listener for application events.
    // It will be invoked automatically when an EntityModifiedEvent is published.
    @EventListener
    // @Transactional: Makes the method run within a database transaction.
    // propagation = Propagation.REQUIRES_NEW: This is the key. It ensures this method runs
    // in a new, independent transaction, decoupling it from the original business transaction.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleEntityChangeEvent(EntityModifiedEvent event) {
        try {
            // Use the injected ObjectMapper to serialize the 'changes' map into a JSON string.
            String changesJson = objectMapper.writeValueAsString(event.changes());

            // Use the @Builder pattern from Lombok to create a new AuditLog instance.
            AuditLog auditLog = AuditLog.builder()
                    .entityName(event.entity().getClass().getSimpleName())
                    .entityId(event.entityId().toString())
                    .action(event.action())
                    .changesJson(changesJson)
                    .timestamp(LocalDateTime.now())
                    .changedBy("system") // Hardcoded for this example.
                    .build();

            auditLogRepository.save(auditLog);

        } catch (JsonProcessingException e) {
            // In a real app, use a proper logger (e.g., SLF4J) here.
            System.err.println("Failed to serialize audit changes to JSON: " + e.getMessage());
        }
    }
}
