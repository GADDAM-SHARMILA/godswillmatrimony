package com.godswill.matrimony.controller;

import com.godswill.matrimony.model.CarouselImage;
import com.godswill.matrimony.service.CarouselImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carousel-images")
@RequiredArgsConstructor
public class CarouselImageController {

    private final CarouselImageService carouselImageService;

    @GetMapping
    public ResponseEntity<List<CarouselImage>> getAllCarouselImages() {
        return ResponseEntity.ok(carouselImageService.getAllCarouselImages());
    }

    @GetMapping("/active")
    public ResponseEntity<List<CarouselImage>> getActiveCarouselImages() {
        return ResponseEntity.ok(carouselImageService.getAllActiveCarouselImages());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CarouselImage> getCarouselImageById(@PathVariable String id) {
        CarouselImage image = carouselImageService.getCarouselImageById(id);
        return (image != null) ? ResponseEntity.ok(image) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<CarouselImage> createCarouselImage(@RequestBody CarouselImage image) {
        CarouselImage savedImage = carouselImageService.saveCarouselImage(image);
        return ResponseEntity.ok(savedImage);
        // If you want perfect REST: return ResponseEntity.status(201).body(savedImage);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CarouselImage> updateCarouselImage(@PathVariable String id,
                                                             @RequestBody CarouselImage imageDetails) {
        CarouselImage updatedImage = carouselImageService.updateCarouselImage(id, imageDetails);
        return (updatedImage != null) ? ResponseEntity.ok(updatedImage) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCarouselImage(@PathVariable String id) {
        carouselImageService.deleteCarouselImage(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<CarouselImage> toggleCarouselImageActive(@PathVariable String id) {
        CarouselImage updated = carouselImageService.toggleCarouselImageActive(id);
        return (updated != null) ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

}