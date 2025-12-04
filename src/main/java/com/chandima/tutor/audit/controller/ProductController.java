package com.chandima.tutor.audit.controller;

import com.chandima.tutor.audit.model.Product;
import com.chandima.tutor.audit.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * A simple REST controller to manage products.
 * This will be used to trigger the audit logging flow.
 *
 * NOTE FOR LEARNERS:
 * This controller is simplified for clarity to focus on the auditing mechanism.
 * In a production application, you would typically:
 * 1.  Use a dedicated Service Layer: Business logic (like updating the product) would be in a
 *     `ProductService` class, not directly in the controller. This follows the layered architecture principle.
 * 2.  Use PATCH for updates: For partial updates, `PATCH` is more semantically correct than `POST`.
 *     We use `POST` here for simplicity in testing with basic tools.
 * 3.  Implement Custom Exception Handling: Instead of returning a generic `ResponseEntity.notFound()`,
 *     you'd use a custom `@ControllerAdvice` to handle exceptions like `ProductNotFoundException`
 *     and return standardized error responses.
 *
 * We have omitted these patterns to keep the code focused on the main topic: event-driven auditing.
 */
// @RestController: A Spring annotation that combines @Controller and @ResponseBody.
// It marks this class as a request handler and ensures return values are written directly to the HTTP response body.
@RestController
// @RequestMapping("/products"): Maps all requests starting with "/products" to this controller.
@RequestMapping("/products")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Endpoint to create a new product. This will trigger the PostInsertEventListener.
     * @param product The product to create.
     * @return The saved product.
     */
    // @PostMapping: Maps HTTP POST requests to this method.
    @PostMapping
    // @RequestBody: Tells Spring to deserialize the JSON from the request body into the Product object.
    public Product createProduct(@RequestBody Product product) {
        return productRepository.save(product);
    }

    /**
     * Endpoint to update a product's price. This will trigger the PostUpdateEventListener.
     * @param id The ID of the product to update.
     * @param price The new price.
     * @return A response entity indicating the result.
     */
    @PostMapping("/update-price/{id}")
    // @Transactional: This is crucial. It ensures the find and update operations occur in the same
    // transaction, which allows Hibernate's "dirty checking" mechanism to automatically detect changes.
    @Transactional
    public ResponseEntity<Product> updateProductPrice(
            // @PathVariable: Binds the {id} from the URL to this method parameter.
            @PathVariable Long id,
            // @RequestParam: Binds the 'price' from the URL query parameter to this method parameter.
            @RequestParam double price) {
        Optional<Product> productOptional = productRepository.findById(id);
        if (productOptional.isPresent()) {
            Product product = productOptional.get();
            product.setPrice(price);
            // NOTE: We don't call save(). Because of @Transactional, Hibernate detects the change
            // to the managed 'product' entity and automatically triggers an UPDATE statement.
            return ResponseEntity.ok(product);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Endpoint to update a product's name. This also triggers the PostUpdateEventListener.
     * @param id The ID of the product to update.
     * @param name The new name.
     * @return A response entity indicating the result.
     */
    @PostMapping("/update-name/{id}")
    @Transactional
    public ResponseEntity<Product> updateProductName(@PathVariable Long id, @RequestParam String name) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setName(name);
                    return ResponseEntity.ok(product);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
