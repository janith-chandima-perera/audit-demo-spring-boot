package com.chandima.tutor.audit.config;

import com.chandima.tutor.audit.listener.AuditHibernateListener;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration class to register our custom Hibernate listeners.
 *
 * NOTE FOR LEARNERS:
 * This class is the "glue" that connects our custom listener to Hibernate.
 * 1.  `@PostConstruct`: This annotation is a simple way to run code after the bean has been
 *     constructed and its dependencies injected. It's perfect for initialization tasks like this.
 * 2.  Unwrapping the SessionFactory: We use `entityManagerFactory.unwrap(SessionFactoryImpl.class)`
 *     to get access to Hibernate's native `SessionFactory`. This is a common pattern when you
 *     need to work with Hibernate-specific features that aren't part of the standard JPA API.
 * 3.  Alternative Approaches: While programmatic registration is clear and explicit, other frameworks
 *     (like Spring Boot itself) sometimes provide properties in `application.properties` to register
 *     listeners, though this approach gives us more fine-grained control.
 */
// @Configuration: Marks this class as a source of bean definitions for the Spring application context.
@Configuration
public class HibernateListenerConfig {

    private final EntityManagerFactory entityManagerFactory;
    private final AuditHibernateListener auditHibernateListener;

    // Spring injects the required beans via this constructor.
    public HibernateListenerConfig(EntityManagerFactory entityManagerFactory, AuditHibernateListener auditHibernateListener) {
        this.entityManagerFactory = entityManagerFactory;
        this.auditHibernateListener = auditHibernateListener;
    }

    /**
     * The @PostConstruct annotation ensures this method is called after dependency injection is done.
     * Here, we're accessing Hibernate's internal SessionFactory to get to the EventListenerRegistry.
     */
    // @PostConstruct: A JSR-250 annotation that ensures this method runs once after the bean's dependencies are set.
    @PostConstruct
    public void registerListeners() {
        // Get the Hibernate SessionFactory from the JPA EntityManagerFactory.
        SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
        // Get the central registry for all Hibernate event listeners.
        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);

        // Append our custom listener to the list of listeners for the POST_UPDATE event type.
        registry.getEventListenerGroup(EventType.POST_UPDATE).appendListener(auditHibernateListener);
        // Also append it for the POST_INSERT event type.
        registry.getEventListenerGroup(EventType.POST_INSERT).appendListener(auditHibernateListener);
    }
}
