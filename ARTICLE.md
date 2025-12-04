# Who Changed What? A Simple Guide to Auditing with Spring Boot and Hibernate

In any application that handles important data, a critical question often arises: "Who changed this, and when?" Whether for compliance, debugging, or accountability, having a clear audit trail is not a luxury—it's a necessity.

But how do you build one without tangling audit logic all over your business code?

In this article, we'll walk through a clean, decoupled, and surprisingly simple way to create an audit log for your JPA entities using the power of Spring Boot and Hibernate's own event system.

## The Goal: A Non-Intrusive Audit Trail

Our goal is to automatically record any creation or update to our `Product` entity. The final record should look something like this:

*   **What changed?** A `Product` with ID `123`.
*   **What was the action?** `UPDATE`.
*   **Who did it?** `system` (or a real user).
*   **When did it happen?** `2023-10-27T10:30:00`.
*   **What were the exact changes?** The `price` changed from `1200.50` to `1350.75`.

Let's build the three core components to make this happen.

## Component 1: The `AuditLog` Entity - Our Record Book

First, we need a place to store our audit records. We'll create a simple JPA entity called `AuditLog`. This entity will be our universal record book for all changes across the application.

```java
package com.chandima.tutor.audit.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entityName;
    private String entityId;
    private String action;
    private String changedBy;
    private LocalDateTime timestamp;
    private String changesJson;
}
```

**Key Design Choices:**
*   **`entityName` & `entityId`**: These generic fields allow us to audit *any* entity (`Product`, `Customer`, `Order`, etc.) using the same `AuditLog` table.
*   **`changesJson`**: Storing changes as a JSON string is flexible and simple. For more complex needs, you could use a native JSONB database type or even a separate `AuditChange` table.

## Component 2: The Hibernate Event Listener - The Detective

Now for the magic. How do we detect changes automatically? We tap into Hibernate's internal event system. We'll create a listener that Hibernate will notify whenever an entity is inserted or updated.

This listener's only job is to **gather information** and publish a Spring `ApplicationEvent`. It does **not** save the audit log itself. This decoupling is crucial for performance and resilience.

```java
package com.chandima.tutor.audit.listener;

import com.chandima.tutor.audit.event.EntityModifiedEvent;
import org.hibernate.event.spi.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuditHibernateListener implements PostUpdateEventListener, PostInsertEventListener {

    private final ApplicationEventPublisher eventPublisher;

    public AuditHibernateListener(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        // For simplicity, we can add a check to audit only specific entities
        // if (!(event.getEntity() instanceof Product)) { return; }

        int[] dirtyProperties = event.getDirtyProperties();
        if (dirtyProperties == null || dirtyProperties.length == 0) return;

        Map<String, String> changes = new HashMap<>();
        // ... logic to compare oldState and newState ...

        publishEvent(event.getEntity(), (Serializable) event.getId(), "UPDATE", changes);
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        // ... logic to format initial state ...
        Map<String, String> changes = new HashMap<>();

        publishEvent(event.getEntity(), (Serializable) event.getId(), "CREATE", changes);
    }
    
    private void publishEvent(Object entity, Serializable id, String action, Map<String, String> changes) {
        if (!changes.isEmpty()) {
            // Publish our custom event for another part of the app to handle.
            eventPublisher.publishEvent(new EntityModifiedEvent(entity, id, action, changes));
        }
    }
    
    // Other required methods...
}
```

## Component 3: The Audit Service - The Scribe

Our listener has detected a change and published an event. Now, we need a component to listen for that event and write it to the database. This is our `AuditLogService`.

This service has one critical feature: it runs in a **new transaction**.

```java
package com.chandima.tutor.audit.service;

import com.chandima.tutor.audit.event.EntityModifiedEvent;
import com.chandima.tutor.audit.model.AuditLog;
import com.chandima.tutor.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    // Constructor injection...

    /**
     * Listens for EntityModifiedEvent and saves an audit log.
     * @Transactional(propagation = Propagation.REQUIRES_NEW) is crucial.
     * It ensures the audit log is saved in a separate, independent transaction.
     * If the main business transaction fails, this audit log will still be saved.
     */
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleEntityModifiedEvent(EntityModifiedEvent event) {
        AuditLog auditLog = AuditLog.builder()
                .entityName(event.entity().getClass().getSimpleName())
                .entityId(event.entityId().toString())
                .action(event.action())
                .changedBy("system") // In a real app, get from SecurityContext
                .timestamp(LocalDateTime.now())
                .changesJson(objectMapper.writeValueAsString(event.changes()))
                .build();
        auditLogRepository.save(auditLog);
    }
}
```

This pattern is powerful because it's:
*   **Decoupled:** Your business logic knows nothing about auditing.
*   **Resilient:** Audit failures won't break your main application flow.
*   **Scalable:** It can be easily extended to cover more entities and even `DELETE` events.

Happy coding!