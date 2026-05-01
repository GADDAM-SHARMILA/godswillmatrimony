package com.godswill.matrimony.service;

import com.godswill.matrimony.model.CarouselImage;
import com.godswill.matrimony.repository.CarouselImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CarouselImageService {

    private final CarouselImageRepository carouselImageRepository;

    public List<CarouselImage> getAllActiveCarouselImages() {
        return carouselImageRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    public List<CarouselImage> getAllCarouselImages() {
        return carouselImageRepository.findAll();
    }

    public CarouselImage getCarouselImageById(String id) {
        return carouselImageRepository.findById(id).orElse(null);
    }

    public CarouselImage saveCarouselImage(CarouselImage image) {
        if (image.getCreatedAt() == null) {
            image.setCreatedAt(LocalDateTime.now());
        }
        image.setUpdatedAt(LocalDateTime.now());
        return carouselImageRepository.save(image);
    }

    public CarouselImage updateCarouselImage(String id, CarouselImage imageDetails) {
        Optional<CarouselImage> existingImageOpt = carouselImageRepository.findById(id);
        if (existingImageOpt.isEmpty()) return null;

        CarouselImage image = existingImageOpt.get();

        if (imageDetails.getImageUrl() != null) image.setImageUrl(imageDetails.getImageUrl());
        if (imageDetails.getTitle() != null) image.setTitle(imageDetails.getTitle());
        if (imageDetails.getDescription() != null) image.setDescription(imageDetails.getDescription());

        // Keep order only if valid
        if (imageDetails.getDisplayOrder() > 0) image.setDisplayOrder(imageDetails.getDisplayOrder());

        // ✅ Update objectPosition too (important for your carousel centering)
        if (imageDetails.getObjectPosition() != null) image.setObjectPosition(imageDetails.getObjectPosition());

        image.setActive(imageDetails.isActive());
        image.setUpdatedAt(LocalDateTime.now());

        return carouselImageRepository.save(image);
    }

    public void deleteCarouselImage(String id) {
        carouselImageRepository.deleteById(id);
    }

    // ✅ Return updated image (better for controller)
    public CarouselImage toggleCarouselImageActive(String id) {
        Optional<CarouselImage> imageOpt = carouselImageRepository.findById(id);
        if (imageOpt.isEmpty()) return null;

        CarouselImage carouselImage = imageOpt.get();
        carouselImage.setActive(!carouselImage.isActive());
        carouselImage.setUpdatedAt(LocalDateTime.now());

        return carouselImageRepository.save(carouselImage);
    }
}
