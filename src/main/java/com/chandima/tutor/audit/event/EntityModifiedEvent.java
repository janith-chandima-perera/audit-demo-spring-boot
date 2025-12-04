package com.chandima.tutor.audit.event;

import java.io.Serializable;
import java.util.Map;

/**
 * A Java Record to represent the event that an entity has been modified.
 *
 * NOTE FOR LEARNERS:
 * We use a generic `Object` for the entity and `Map<String, String>` for the changes
 * to keep this event reusable for different entity types. This is a practical choice.
 * In very complex systems, you might create a more specific hierarchy of events
 * (e.g., `ProductModifiedEvent`, `OrderModifiedEvent`) for stricter type safety,
 * but that often adds unnecessary complexity.
 *
 * @param entity      The actual entity object that was changed.
 * @param entityId    The ID of the entity.
 * @param action      The action performed (e.g., "UPDATE", "CREATE").
 * @param changes     A map detailing what changed, e.g., {"price": "100.0 -> 120.0"}.
 */
// `public record`: This is a modern Java feature (since Java 16) for creating immutable data carriers.
// The compiler automatically generates a constructor, private final fields, getters,
// and implementations for equals(), hashCode(), and toString(). It's perfect for a DTO or event class.
public record EntityModifiedEvent(
        Object entity,
        Serializable entityId,
        String action,
        Map<String, String> changes
) {
}
