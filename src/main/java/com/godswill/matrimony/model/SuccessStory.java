package com.godswill.matrimony.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "success_stories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuccessStory {

    @Id
    private String id;

    private String coupleName;
    private String story;

    // ✅ CHANGED: Store S3 URL instead of binary data
    private String imageUrl;  // e.g. "https://godswill-images.s3.ap-south-2.amazonaws.com/stories/uuid.jpg"
    private String imageType; // e.g. "image/jpeg" — metadata for the URL

    private LocalDate marriageDate;
    private String location;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}