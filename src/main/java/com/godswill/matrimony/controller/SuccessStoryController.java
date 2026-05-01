package com.godswill.matrimony.controller;

import com.godswill.matrimony.model.SuccessStory;
import com.godswill.matrimony.service.SuccessStoryService;
import com.godswill.matrimony.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class SuccessStoryController {

    private final SuccessStoryService successStoryService;
    private final ImageStorageService imageStorageService;

    /**
     * Display all success stories (public page).
     */
    @GetMapping("/success-stories")
    public String successStories(Model model) {
        List<SuccessStory> stories = successStoryService.getAllSuccessStories();
        model.addAttribute("successStories", stories);
        model.addAttribute("activePage", "stories");
        model.addAttribute("pageTitle", "Success Stories");
        return "success-stories";
    }

    /**
     * Show form to submit a new success story.
     */
    @GetMapping("/submit-story")
    public String submitStoryPage(Model model) {
        model.addAttribute("successStory", new SuccessStory());
        model.addAttribute("pageTitle", "Submit Story");
        return "submit-story";
    }

    /**
     * ✅ UPDATED: Save success story with S3 image upload
     * No longer stores binary data in MongoDB
     */
    @PostMapping("/submit-story")
    public String saveStory(
            @ModelAttribute("successStory") SuccessStory successStory,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {

        try {
            successStoryService.createSuccessStory(successStory, imageFile);
            return "redirect:/success-stories?message=Story+submitted+successfully";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/submit-story?error=" + e.getMessage();
        }
    }

    /**
     * Get a single success story by ID (returns JSON with S3 URL).
     */
    @GetMapping("/api/success-story/{id}")
    @ResponseBody
    public ResponseEntity<?> getStoryById(@PathVariable String id) {
        SuccessStory story = successStoryService.getStoryById(id);
        if (story == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(story);
    }

    /**
     * ✅ UPDATED: Redirect to S3 URL instead of streaming binary data
     * Browser handles the redirect to S3, where image is cached globally
     */
    @GetMapping("/success-story/image/{id}")
    public String getImage(@PathVariable String id) {
        SuccessStory story = successStoryService.getStoryById(id);

        if (story == null || story.getImageUrl() == null || story.getImageUrl().isBlank()) {
            return "redirect:/images/placeholder.png";
        }

        // ✅ Redirect to S3 URL — browser downloads directly from S3
        return "redirect:" + story.getImageUrl();
    }

    /**
     * Update an existing success story with optional new image.
     */
    @PostMapping("/success-story/{id}/edit")
    public String updateStory(
            @PathVariable String id,
            @ModelAttribute SuccessStory updatedStory,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {

        try {
            successStoryService.updateSuccessStory(id, updatedStory, imageFile);
            return "redirect:/success-stories?message=Story+updated+successfully";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/success-stories?error=" + e.getMessage();
        }
    }

    /**
     * Delete a success story and its image from S3.
     */
    @PostMapping("/success-story/{id}/delete")
    public String deleteStory(@PathVariable String id) {
        try {
            successStoryService.deleteSuccessStory(id);
            return "redirect:/success-stories?message=Story+deleted+successfully";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/success-stories?error=" + e.getMessage();
        }
    }
}