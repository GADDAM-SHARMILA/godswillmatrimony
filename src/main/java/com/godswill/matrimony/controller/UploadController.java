package com.godswill.matrimony.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.concurrent.TimeUnit;

/**
 * ✅ UPDATED: UploadController now primarily handles redirects to S3.
 *
 * Since all images are now stored in S3 with public URLs, this controller
 * can be simplified. Images are accessed directly via S3 URLs in templates.
 *
 * This endpoint remains for backward compatibility with any existing URL references.
 */
@Controller
@RequiredArgsConstructor
public class UploadController {

    /**
     * ⚠️ DEPRECATED: This endpoint assumes MongoDB GridFS storage.
     *
     * For S3 images: Use the image URL directly from the database.
     * Example: <img src="https://godswill-images.s3.ap-south-2.amazonaws.com/profiles/uuid.jpg">
     *
     * If you still have legacy GridFS images, they need to be migrated to S3.
     */
    @GetMapping("/uploads/{id}")
    public ResponseEntity<?> getUpload(@PathVariable String id) {
        // This endpoint is deprecated for GridFS
        // Return a 404 as all images should now be accessed via S3 URLs
        return ResponseEntity.notFound().build();
    }

    /**
     * Optional: Redirect old /uploads/{id} requests to S3 bucket
     * (if you want backward compatibility)
     */
    @GetMapping("/uploads/s3/{key}")
    public String redirectToS3(@PathVariable String key) {
        return "redirect:https://godswill-images.s3.ap-south-2.amazonaws.com/" + key;
    }
}