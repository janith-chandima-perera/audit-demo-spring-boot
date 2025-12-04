# Simple & Decoupled Auditing with Spring Boot and Hibernate

This project is a demonstration of a clean, non-intrusive, and robust way to implement an audit trail for JPA entities in a Spring Boot application. It answers the critical question: "Who changed what, and when?" without cluttering your business logic.

The core idea is to use Hibernate's own event system to detect changes and Spring's Application Events to process them in a decoupled and transactionally-safe manner.

## The Problem

In many applications, there's a need to track changes to important data for reasons like:
- **Accountability:** Knowing who is responsible for a change.
- **Debugging:** Reconstructing a sequence of events that led to a bug.
- **Compliance:** Meeting regulatory requirements (e.g., GDPR, HIPAA).

A naive approach might involve adding audit-saving logic directly into your business services. This leads to tangled code that is hard to maintain and test.

```java
// The "Bad" Way - Mixing concerns
@Service
public class ProductService {
    public void updateProductPrice(Long id, double newPrice) {
        Product product = productRepository.findById(id).orElseThrow();
        // Business logic
        double oldPrice = product.getPrice();
        product.setPrice(newPrice);
        productRepository.save(product);

        // Audit logic mixed in!
        auditLogRepository.save(new AuditLog(
            "Product", id.toString(), "UPDATE", "price", String.valueOf(oldPrice), String.valueOf(newPrice)
        ));
    }
}
```

This project demonstrates a much cleaner solution.

## The Solution: A Decoupled Architecture

We use a three-step, event-driven process that separates the "detection" of a change from the "recording" of the change.

1.  **Detect (The Detective):** A Hibernate `EventListener` (`AuditHibernateListener`) hooks into Hibernate's session. It gets notified automatically *after* an entity is inserted or updated. Its only job is to gather the details of the change and publish a Spring `ApplicationEvent`.

2.  **Decouple (The Messenger):** A custom Spring `ApplicationEvent` (`EntityModifiedEvent`) acts as a message. It's a simple data carrier that holds information about the change. This breaks the direct dependency between the Hibernate listener and the audit-saving logic.

3.  **Record (The Scribe):** A dedicated Spring `@Service` (`AuditLogService`) listens for the `EntityModifiedEvent`. When it receives one, it saves an `AuditLog` entity to the database. Crucially, it does this in a **new, separate transaction** (`@Transactional(propagation = Propagation.REQUIRES_NEW)`).

### Visual Flow

```
[JPA Save/Update] -> [Hibernate Session] -> [AuditHibernateListener] --(publishes)--> [EntityModifiedEvent] --(consumed by)--> [AuditLogService] -> [Database]
       |                                                                                                                      (New Transaction)      |
       |                                                                                                                                             |
       +-----------------------------------------------------(Main Transaction)--------------------------------------------------------------------+
```

### Why is this better?

- **Decoupled:** The `Product` entity and its services know nothing about auditing.
- **Resilient:** Because the audit log is saved in a new transaction, a failure to save the audit record **will not** cause the main business transaction (e.g., updating the product's price) to roll back.
- **Generic:** The listener can be easily adapted to audit any entity, not just `Product`.
- **Clean:** Business logic remains focused on its primary responsibility.

---

## Core Components

### 1. The Models

- **`Product.java`**: A standard JPA entity that we want to audit.
- **`AuditLog.java`**: The JPA entity that represents a single record in our audit trail.

### 2. The Auditing Mechanism

- **`AuditHibernateListener.java`**: The heart of the detection mechanism. It implements Hibernate's `PostUpdateEventListener` and `PostInsertEventListener`.
- **`EntityModifiedEvent.java`**: A simple Java `record` that carries the payload from the listener to the service.
- **`AuditLogService.java`**: The service responsible for persisting the audit log, using `@EventListener` and `@Transactional(propagation = Propagation.REQUIRES_NEW)`.

### 3. Configuration

- **`HibernateListenerConfigurer.java`**: Uses Hibernate's `Integrator` SPI to programmatically register our `AuditHibernateListener` at startup.
- **`resources/META-INF/services/org.hibernate.integrator.spi.Integrator`**: The standard Java Service Provider Interface (SPI) file that tells Hibernate to load our custom configurer.

---

## How to Run the Project

### Prerequisites

- Java 17+
- Maven 3.6+
- Docker and Docker Compose (for the PostgreSQL database)

### 1. Start the Database

A `docker-compose.yml` file is included to easily start a PostgreSQL container.

```bash
docker-compose up -d
```

This will start a PostgreSQL server on port `5433`, which matches the `application.properties` configuration.

### 2. Run the Application

You can run the application using the Spring Boot Maven plugin:

```bash
mvn spring-boot:run
```

The application will start on port `8080`.

### 3. Test the Audit Trail

Use a tool like `curl` or Postman to interact with the `Product` REST controller.

**A. Create a new product:**

```bash
curl -X POST http://localhost:8080/products \
-H "Content-Type: application/json" \
-d '{"name": "Laptop", "price": 1200.50}'
```

**B. Update the product:**

```bash
curl -X PUT http://localhost:8080/products/1 \
-H "Content-Type: application/json" \
-d '{"name": "Gaming Laptop", "price": 1350.75}'
```

### 4. Check the Database

After running the commands, you can connect to the PostgreSQL database (user: `admin`, pass: `password123`, db: `audit_db`) and check the `product` and `audit_log` tables.

- The `product` table will have one entry with the final state (`id=1`, `name='Gaming Laptop'`, `price=1350.75`).
- The `audit_log` table will have **two** entries:
  1.  An entry for the `CREATE` action.
  2.  An entry for the `UPDATE` action, with the `changes_json` field showing the old vs. new values for `name` and `price`.

### 5. Stop the Database

When you are finished, you can stop and remove the database container:

```bash
docker-compose down
```