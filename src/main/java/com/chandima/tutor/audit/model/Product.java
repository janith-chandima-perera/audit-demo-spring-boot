package com.chandima.tutor.audit.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a simple Product entity.
 * We will be auditing changes to this entity.
 *
 * NOTE FOR LEARNERS:
 * This entity is kept simple for clarity. In a production system, you would typically see:
 * 1.  @Column annotations: To explicitly define column names (e.g., @Column(name = "product_name")),
 *     length, nullability, etc. This avoids letting Hibernate decide the naming strategy.
 * 2.  Validation Annotations: Such as @NotNull, @Size, or @Min to enforce data integrity at the
 *     application level before it even hits the database.
 * 3.  More Complex Types: You might use BigDecimal for price to handle currency precisely,
 *     avoiding floating-point inaccuracies.
 */
// @Entity: Specifies that this class is a JPA entity and is mapped to a database table.
@Entity
// @Data: A Lombok annotation that generates boilerplate code for getters, setters, toString(), equals(), and hashCode().
@Data
// @NoArgsConstructor: A Lombok annotation that generates a constructor with no arguments. JPA requires this.
@NoArgsConstructor
// @AllArgsConstructor: A Lombok annotation that generates a constructor with arguments for all fields.
@AllArgsConstructor
public class Product {

    // @Id: Marks this field as the primary key of the entity.
    @Id
    // @GeneratedValue: Configures the way the primary key is generated.
    // GenerationType.IDENTITY: Relies on the database's auto-increment column to generate the ID.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private double price;
}
