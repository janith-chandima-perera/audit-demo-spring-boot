package com.chandima.tutor.audit.listener;

import com.chandima.tutor.audit.event.EntityModifiedEvent;
import com.chandima.tutor.audit.model.Product;
import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Hibernate event listener to capture entity changes and publish them as Spring events.
 * This is the heart of our audit logging mechanism.
 *
 * NOTE FOR LEARNERS:
 * This listener directly implements Hibernate's `PostUpdateEventListener`. This is a powerful
 * but low-level approach.
 * 1.  Entity-Specific Logic: The `if (!(event.getEntity() instanceof Product))` check makes this
 *     listener specific to `Product`. A more advanced implementation might use a registry or
 *     annotations (e.g., `@Auditable`) to determine which entities to audit, making the
 *     listener more generic.
 * 2.  Decoupling: The listener's only job is to gather data and publish an event. It does NOT
 *     save the audit log itself. This is a crucial design choice. It keeps the listener fast
 *     and ensures that if audit saving fails, it doesn't crash the main transaction.
 */
// @Component: Marks this class as a Spring component, making it eligible for dependency injection.
@Component
public class AuditHibernateListener implements PostUpdateEventListener, PostInsertEventListener {

    // The ApplicationEventPublisher is a Spring utility for publishing events to listeners.
    private final ApplicationEventPublisher eventPublisher;

    // Spring injects the ApplicationEventPublisher via this constructor.
    public AuditHibernateListener(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    // This method is called by Hibernate *after* an entity update occurs.
    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        // We only care about Product entities for now.
        if (!(event.getEntity() instanceof Product)) {
            return;
        }

        // `event.getDirtyProperties()`: Returns an array of indexes for the properties that have changed.
        int[] dirtyProperties = event.getDirtyProperties();
        if (dirtyProperties == null || dirtyProperties.length == 0) {
            return;
        }

        String[] propertyNames = event.getPersister().getPropertyNames();
        Object[] oldState = event.getOldState(); // The entity's state before the update.
        Object[] newState = event.getState();   // The entity's state after the update.

        Map<String, String> changes = new HashMap<>();
        for (int propIndex : dirtyProperties) {
            String propName = propertyNames[propIndex];
            Object oldValue = oldState != null ? oldState[propIndex] : null;
            Object newValue = newState != null ? newState[propIndex] : null;

            // Format the change into a "old -> new" string for readability.
            String changeString = String.format("%s -> %s",
                    oldValue != null ? oldValue.toString() : "null",
                    newValue != null ? newValue.toString() : "null"
            );
            changes.put(propName, changeString);
        }

        if (!changes.isEmpty()) {
            EntityModifiedEvent modifiedEvent = new EntityModifiedEvent(
                    event.getEntity(),
                    (Serializable) event.getId(), // Cast to Serializable is required.
                    "UPDATE",
                    changes
            );
            // `eventPublisher.publishEvent()`: Publishes our custom event to any interested Spring listeners.
            eventPublisher.publishEvent(modifiedEvent);
        }
    }

    // This method is called by Hibernate *after* an entity is first inserted.
    @Override
    public void onPostInsert(PostInsertEvent event) {
        if (!(event.getEntity() instanceof Product)) {
            return;
        }

        String[] propertyNames = event.getPersister().getPropertyNames();
        Object[] state = event.getState(); // For an insert, there is no "old state".
        Map<String, String> changes = new HashMap<>();

        for (int i = 0; i < propertyNames.length; i++) {
            changes.put(propertyNames[i], "null -> " + (state[i] != null ? state[i].toString() : "null"));
        }

        EntityModifiedEvent modifiedEvent = new EntityModifiedEvent(
                event.getEntity(),
                (Serializable) event.getId(),
                "CREATE",
                changes
        );
        eventPublisher.publishEvent(modifiedEvent);
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        // Returning false means this listener can operate during the transaction flush.
        // The actual commit-dependent logic is handled by our @EventListener in a separate transaction.
        return false;
    }
}
