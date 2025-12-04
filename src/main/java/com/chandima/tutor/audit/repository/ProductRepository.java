package com.chandima.tutor.audit.repository;

import com.chandima.tutor.audit.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Standard Spring Data JPA repository for the Product entity.
 * Provides CRUD operations out of the box.
 *
 * NOTE FOR LEARNERS:
 * This interface is intentionally left empty. Spring Data JPA automatically implements
 * the basic CRUD methods (save, findById, findAll, delete, etc.) at runtime.
 * In a real application, you might add custom query methods here, for example:
 * `Optional<Product> findByName(String name);`
 */
// @Repository: A Spring annotation that marks this interface as a Repository bean.
// It tells Spring to handle exceptions and enables component scanning to find it.
@Repository
// JpaRepository<Product, Long>: We extend this interface from Spring Data JPA.
// It provides all the standard database operations for the `Product` entity, which has a `Long` primary key.
public interface ProductRepository extends JpaRepository<Product, Long> {
}
