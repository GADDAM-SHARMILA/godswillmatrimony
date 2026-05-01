package com.godswill.matrimony.service;

import com.godswill.matrimony.model.SuccessStory;
import com.godswill.matrimony.repository.SuccessStoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SuccessStoryService {

    private final SuccessStoryRepository successStoryRepository;
    private final ImageStorageService imageStorageService;

    /**
     * Create a new success story with image upload to S3.
     *
     * @param successStory The story data
     * @param imageFile The image file to upload to S3 (optional)
     * @return Saved SuccessStory with S3 image URL
     */
    public SuccessStory createSuccessStory(SuccessStory successStory, MultipartFile imageFile) {
        try {
            // Upload image to S3 if provided
            if (imageFile != null && !imageFile.isEmpty()) {
                String s3Url = imageStorageService.save(imageFile);
                successStory.setImageUrl(s3Url);
                successStory.setImageType(imageFile.getContentType());
            }

            successStory.onCreate();
            return successStoryRepository.save(successStory);

        } catch (Exception e) {
            throw new RuntimeException("❌ Error creating success story: " + e.getMessage(), e);
        }
    }

    /**
     * Get all published success stories (public view).
     */
    public List<SuccessStory> getAllSuccessStories() {
        return successStoryRepository.findAll();
    }

    /**
     * Get a single success story by ID.
     */
    public SuccessStory getStoryById(String id) {
        return successStoryRepository.findById(id).orElse(null);
    }

    /**
     * Update an existing success story with optional new image.
     */
    public SuccessStory updateSuccessStory(String id, SuccessStory updatedStory, MultipartFile newImageFile) {
        Optional<SuccessStory> existingOpt = successStoryRepository.findById(id);

        if (existingOpt.isEmpty()) {
            throw new RuntimeException("❌ Success story not found: " + id);
        }

        try {
            SuccessStory existing = existingOpt.get();

            // Update text fields
            if (updatedStory.getCoupleName() != null) {
                existing.setCoupleName(updatedStory.getCoupleName());
            }
            if (updatedStory.getStory() != null) {
                existing.setStory(updatedStory.getStory());
            }
            if (updatedStory.getMarriageDate() != null) {
                existing.setMarriageDate(updatedStory.getMarriageDate());
            }
            if (updatedStory.getLocation() != null) {
                existing.setLocation(updatedStory.getLocation());
            }

            // Handle image update
            if (newImageFile != null && !newImageFile.isEmpty()) {
                // Delete old image from S3 if exists
                if (existing.getImageUrl() != null && !existing.getImageUrl().isBlank()) {
                    imageStorageService.delete(existing.getImageUrl());
                }

                // Upload new image to S3
                String newS3Url = imageStorageService.save(newImageFile);
                existing.setImageUrl(newS3Url);
                existing.setImageType(newImageFile.getContentType());
            }

            existing.onUpdate();
            return successStoryRepository.save(existing);

        } catch (Exception e) {
            throw new RuntimeException("❌ Error updating success story: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a success story and its image from S3.
     */
    public void deleteSuccessStory(String id) {
        Optional<SuccessStory> storyOpt = successStoryRepository.findById(id);

        if (storyOpt.isPresent()) {
            SuccessStory story = storyOpt.get();

            // Delete image from S3
            if (story.getImageUrl() != null && !story.getImageUrl().isBlank()) {
                imageStorageService.delete(story.getImageUrl());
            }

            // Delete story from MongoDB
            successStoryRepository.deleteById(id);
        }
    }
}