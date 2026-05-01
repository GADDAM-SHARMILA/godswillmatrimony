package com.godswill.matrimony.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "catering_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CateringItem {

    @Id
    private String id;

    private String name;
    private String category;       // e.g. Biryani, Curry, Snack, Bread, etc.
    private String description;
    private String origin;
    private String imageUrl;
    private boolean active = true;
    private int displayOrder = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}