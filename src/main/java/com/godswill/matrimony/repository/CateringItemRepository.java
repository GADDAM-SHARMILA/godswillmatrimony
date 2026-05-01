package com.godswill.matrimony.repository;

import com.godswill.matrimony.model.CateringItem;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CateringItemRepository extends MongoRepository<CateringItem, String> {

    List<CateringItem> findByActiveTrue();
    List<CateringItem> findByCategoryIgnoreCase(String category);
    List<CateringItem> findAllByOrderByDisplayOrderAsc();
    List<CateringItem> findByActiveTrueOrderByDisplayOrderAsc();

    // Used by CateringDataSeeder for upsert — match on name + category
    Optional<CateringItem> findByNameAndCategory(String name, String category);
}