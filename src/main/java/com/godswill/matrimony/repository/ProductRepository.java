package com.godswill.matrimony.repository;

import com.godswill.matrimony.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    // Case-insensitive category match — handles "bride", "BRIDE", "Bride" etc.
    List<Product> findByCategoryIgnoreCaseAndActiveTrue(String category);

    List<Product> findByCategoryIgnoreCase(String category);

    List<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name);

    List<Product> findByActiveTrue();
}