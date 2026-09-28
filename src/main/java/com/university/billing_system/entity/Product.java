package com.university.billing_system.entity;

import com.university.billing_system.exception.InsufficientStockException;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @NotBlank(message = "Product name cannot be empty")
    @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Price cannot be null")
    @Positive(message = "Price must be > 0")
    @Column(nullable = false)
    private BigDecimal price;

    @NotNull(message = "Stock quantity cannot be null")
    @PositiveOrZero(message = "Stock cannot be negative")
    @Column(nullable = false)
    private Integer stockQuantity;

    @NotNull(message = "Reorder level cannot be null")
    @PositiveOrZero(message = "Reorder level cannot be negative")
    @Column(nullable = false)
    private Integer reorderLevel;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Product() {
    }

    public Product(String name, String description, BigDecimal price,
                   Integer stockQuantity, Integer reorderLevel) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.reorderLevel = reorderLevel;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ---------- business logic ----------

    public boolean needsReorder() {
        return stockQuantity < reorderLevel;
    }

    public boolean hasEnoughStock(Integer quantity) {
        return stockQuantity >= quantity;
    }

    // Now throws an error instead of silently doing nothing
    public void reduceStock(Integer quantity) {
        if (!hasEnoughStock(quantity)) {
            throw new InsufficientStockException(
                    "Insufficient stock for '" + name + "': requested " + quantity
                            + ", available " + stockQuantity);
        }
        this.stockQuantity -= quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public void increaseStock(Integer quantity) {
        this.stockQuantity += quantity;
        this.updatedAt = LocalDateTime.now();
    }

    // ---------- getters & setters ----------

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public Integer getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(Integer reorderLevel) { this.reorderLevel = reorderLevel; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Product{productId=" + productId + ", name='" + name + "', price=" + price
                + ", stockQuantity=" + stockQuantity + ", reorderLevel=" + reorderLevel + "}";
    }
}