package com.godswill.matrimony.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageStorageService {

    private final AmazonS3 amazonS3;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.base-url}")
    private String s3BaseUrl;

    // ✅ NEW: CloudFront configuration
    @Value("${cloudfront.domain:}")
    private String cloudfrontDomain;

    @Value("${cloudfront.enabled:false}")
    private boolean cloudfrontEnabled;

    @Value("${cloudfront.cache-ttl:86400}")
    private long cacheTtl;

    /**
     * Uploads a MultipartFile to S3 and returns the full public URL.
     * ✅ NEW: Returns CloudFront URL if enabled, otherwise S3 URL
     */
    public String save(MultipartFile file) throws Exception {
        String key = buildKey(file.getOriginalFilename());

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        try (InputStream in = file.getInputStream()) {
            PutObjectRequest request = new PutObjectRequest(bucket, key, in, metadata);
            amazonS3.putObject(request);
        }

        // ✅ NEW: Return CloudFront URL or S3 URL
        return getPublicUrl(key);
    }

    /**
     * Saves raw bytes to S3 (used by base64 image uploads).
     * ✅ NEW: Returns CloudFront URL if enabled
     */
    public String saveBytes(byte[] bytes, String originalFilename, String contentType) throws Exception {
        String key = buildKey(originalFilename);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(bytes.length);

        try (InputStream in = new java.io.ByteArrayInputStream(bytes)) {
            PutObjectRequest request = new PutObjectRequest(bucket, key, in, metadata);
            amazonS3.putObject(request);
        }

        // ✅ NEW: Return CloudFront URL or S3 URL
        return getPublicUrl(key);
    }

    /**
     * ✅ NEW: Get public URL - CloudFront if enabled, S3 otherwise
     */
    private String getPublicUrl(String key) {
        if (cloudfrontEnabled && cloudfrontDomain != null && !cloudfrontDomain.isBlank()) {
            String url = "https://" + cloudfrontDomain + "/" + key;
            System.out.println("✅ Image uploaded to CloudFront CDN: " + url);
            return url;
        }

        String url = s3BaseUrl + "/" + key;
        System.out.println("✅ Image uploaded to S3: " + url);
        return url;
    }

    /**
     * Deletes an image from S3.
     * CloudFront will automatically cache invalidate after TTL
     */
    public void delete(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;

        try {
            // Extract key from CloudFront or S3 URL
            String key;
            if (cloudfrontEnabled && imageUrl.contains(cloudfrontDomain)) {
                key = imageUrl.replace("https://" + cloudfrontDomain + "/", "");
            } else {
                key = imageUrl.replace(s3BaseUrl + "/", "");
            }

            amazonS3.deleteObject(bucket, key);
            System.out.println("✅ Image deleted from S3: " + key);

        } catch (Exception e) {
            System.err.println("⚠️ S3 delete failed: " + e.getMessage());
        }
    }

    /**
     * Builds a unique S3 key with folder prefix and UUID.
     */
    private String buildKey(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "profiles/" + UUID.randomUUID() + extension;
    }

    /**
     * ✅ NEW: Get CloudFront URL for an existing S3 key
     * Useful for migrating old S3 URLs to CloudFront
     */
    public String convertToCloudFrontUrl(String s3Url) {
        if (!cloudfrontEnabled || cloudfrontDomain == null || cloudfrontDomain.isBlank()) {
            return s3Url; // Return S3 URL if CDN disabled
        }

        // Extract key from S3 URL
        String key = s3Url.replace(s3BaseUrl + "/", "");
        return "https://" + cloudfrontDomain + "/" + key;
    }

    /**
     * ✅ NEW: Check CDN status
     */
    public String getStatus() {
        return cloudfrontEnabled
                ? "✅ CloudFront CDN ENABLED - Domain: " + cloudfrontDomain
                : "⚠️ CloudFront CDN DISABLED - Using S3 direct";
    }
}