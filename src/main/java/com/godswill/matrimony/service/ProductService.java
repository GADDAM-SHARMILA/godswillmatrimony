package com.godswill.matrimony.service;

import com.godswill.matrimony.model.Product;
import com.godswill.matrimony.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    // ── Public ────────────────────────────────────────────────────────────────

    public List<Product> getBrideProducts() {
        return productRepository.findByCategoryIgnoreCaseAndActiveTrue("bride");
    }

    public List<Product> getGroomProducts() {
        return productRepository.findByCategoryIgnoreCaseAndActiveTrue("groom");
    }

    public List<Product> getAllActiveProducts() {
        return productRepository.findByActiveTrue();
    }

    public Optional<Product> getProductById(String id) {
        return productRepository.findById(id);
    }

    public List<Product> searchProducts(String keyword) {
        return productRepository.findByNameContainingIgnoreCaseAndActiveTrue(keyword);
    }

    // ── Admin CRUD ────────────────────────────────────────────────────────────

    public Product createProduct(Product product) {
        if (product.getCategory() != null)
            product.setCategory(product.getCategory().toLowerCase());
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        product.setActive(true);
        return productRepository.save(product);
    }

    public Product updateProduct(String id, Product updated) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setOriginalPrice(updated.getOriginalPrice());
        existing.setPrice(updated.getPrice());
        existing.setDiscountedPrice(updated.getDiscountedPrice());
        existing.setPremiumDiscountPercent(updated.getPremiumDiscountPercent());
        existing.setCategory(updated.getCategory() != null
                ? updated.getCategory().toLowerCase() : null);
        existing.setSubCategory(updated.getSubCategory());
        existing.setImageUrl(updated.getImageUrl());
        existing.setImageUrl2(updated.getImageUrl2());
        existing.setStock(updated.getStock());
        existing.setSizes(updated.getSizes());
        existing.setColors(updated.getColors());
        existing.setBrand(updated.getBrand());
        existing.setMaterial(updated.getMaterial());
        existing.setTags(updated.getTags());
        existing.setDeliveryDays(updated.getDeliveryDays());
        existing.setFeatured(updated.isFeatured());
        existing.setActive(updated.isActive());
        existing.setVariants(updated.getVariants());
        existing.setUpdatedAt(LocalDateTime.now());

        return productRepository.save(existing);
    }

    // Hard delete — uses _id field correctly via MongoTemplate
    public void deleteProduct(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        mongoTemplate.remove(query, Product.class);
    }

    // ── Stock management ──────────────────────────────────────────────────────

    public boolean isInStock(String productId, int requiredQty) {
        return productRepository.findById(productId)
                .map(p -> p.getStock() >= requiredQty)
                .orElse(false);
    }

    public void reduceStock(String productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        if (product.getStock() < quantity)
            throw new RuntimeException("Insufficient stock for: " + product.getName());
        product.setStock(product.getStock() - quantity);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
    }

    public void restoreStock(String productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        product.setStock(product.getStock() + quantity);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
    }

    public List<Product> getAllProductsAdmin() {
        return productRepository.findAll();
    }
}