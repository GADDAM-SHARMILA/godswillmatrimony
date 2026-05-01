package com.godswill.matrimony.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private String id;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @Email
    @NotBlank
    @Indexed(unique = true)
    private String email;

    @NotBlank
    @Indexed(unique = true)
    private String phone;

    @NotBlank
    private String password;

    private String gender;
    private String whatsapp;


    private LocalDate dateOfBirth;

    private Boolean emailVerified = false;
    private String resetToken;
    private LocalDateTime resetTokenExpiry;

    // ✅ NEW: USER / ADMIN
    private String role = "USER";

    // ✅ NEW: Subscription fields
    private String activeSubscriptionId; // Reference to active subscription
    private SubscriptionPlan currentPlan; // Current plan (null = FREE)
    private LocalDateTime subscriptionExpiryDate; // When subscription expires

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;

    public void onCreate() {
        if (email != null) email = email.trim().toLowerCase();
        if (phone != null) phone = phone.trim();

        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();

        if (emailVerified == null) emailVerified = false;

        // ✅ default role
        if (role == null || role.isBlank()) role = "USER";
    }

    public void onUpdate() {
        if (email != null) email = email.trim().toLowerCase();
        if (phone != null) phone = phone.trim();

        if (role == null || role.isBlank()) role = "USER";

        updatedAt = LocalDateTime.now();
    }

    // ✅ Check if user has active premium subscription
    public boolean isPremium() {
        return currentPlan != null &&
                subscriptionExpiryDate != null &&
                LocalDateTime.now().isBefore(subscriptionExpiryDate);
    }
    public String getWhatsapp() {
        return whatsapp;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }
    // ✅ Check if subscription is expired
    public boolean isSubscriptionExpired() {
        return currentPlan != null &&
                subscriptionExpiryDate != null &&
                LocalDateTime.now().isAfter(subscriptionExpiryDate);
    }
}