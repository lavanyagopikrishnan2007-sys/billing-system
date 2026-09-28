package com.university.billing_system.service;

import com.university.billing_system.entity.Product;
import com.university.billing_system.exception.InvalidBillStateException;
import com.university.billing_system.exception.ResourceNotFoundException;
import com.university.billing_system.repository.BillItemRepository;
import com.university.billing_system.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final BillItemRepository billItemRepository;

    public ProductService(ProductRepository productRepository,
                          BillItemRepository billItemRepository) {
        this.productRepository = productRepository;
        this.billItemRepository = billItemRepository;
    }

    @Transactional
    public Product createProduct(Product product) {
        product.setProductId(null);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long id, Product updated) {
        Product existing = getProduct(id);
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setPrice(updated.getPrice());
        existing.setStockQuantity(updated.getStockQuantity());
        existing.setReorderLevel(updated.getReorderLevel());
        existing.setUpdatedAt(LocalDateTime.now());
        return productRepository.save(existing);
    }

    // A product used on any bill can't be deleted, or old bills would break
    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProduct(id);
        if (billItemRepository.existsByProductId(id)) {
            throw new InvalidBillStateException(
                    "Cannot delete '" + product.getName() + "': it is used in existing bills");
        }
        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Product> searchByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional(readOnly = true)
    public List<Product> getLowStockProducts() {
        return productRepository.findBelowReorderLevel();
    }
}