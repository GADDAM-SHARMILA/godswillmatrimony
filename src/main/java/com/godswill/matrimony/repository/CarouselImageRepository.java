package com.godswill.matrimony.repository;

import com.godswill.matrimony.model.CarouselImage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarouselImageRepository extends MongoRepository<CarouselImage, String> {

    // Best one for homepage carousel
    List<CarouselImage> findByActiveTrueOrderByDisplayOrderAsc();

    // Optional
    List<CarouselImage> findByActiveTrue();

    CarouselImage findByDisplayOrder(int displayOrder);
}
