package com.godswill.matrimony.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

@Document(collection = "profiles")
@CompoundIndexes({
        @CompoundIndex(name = "idx_gender_religion",      def = "{'gender': 1, 'religion': 1}"),
        @CompoundIndex(name = "idx_gender_denomination",  def = "{'gender': 1, 'denomination': 1}"),
        @CompoundIndex(name = "idx_gender_maritalStatus", def = "{'gender': 1, 'maritalStatus': 1}"),
        @CompoundIndex(name = "idx_gender_education",     def = "{'gender': 1, 'education': 1}"),
        @CompoundIndex(name = "idx_verified_active",      def = "{'verified': 1, 'active': 1}"),
        @CompoundIndex(name = "idx_city_state",           def = "{'city': 1, 'state': 1}"),
        @CompoundIndex(name = "idx_active_createdAt",     def = "{'active': 1, 'createdAt': -1}")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Profile {

    @Id
    private String id;

    // Optional: Auto-increment profile public ID like MAT10001 (uses DatabaseSequence)
    @Indexed(unique = true, sparse = true)
    private String profileNumber;

    @Indexed(unique = true, sparse = true)
    private String userId;

    @Indexed
    private String proposedByPastorId;

    // ✅ New field
    private String proposedByPastorName;

    // Personal Information
    private String firstName;
    private String lastName;

    @Indexed
    private String gender;

    private LocalDate dateOfBirth;

    // You can keep age as stored field OR ignore and always use calculatedAge
    private Integer age;

    @Indexed
    private String maritalStatus;

    private Integer height;
    private String motherTongue;

    // Religious Information
    @Indexed
    private String religion; // christian / converted_christian

    // ✅ NEW FIELD
    @Indexed
    private String denomination; // hebron/lutheran/roman/baptist/csi/catholic

    private String caste;

    // Location Details
    private String country;
    private String state;

    @Indexed
    private String city;

    private String pincode;

    // Professional Information
    @Indexed
    private String education;

    @Indexed
    private String profession;

    private String annualIncome;
    private String employedIn;

    // About
    private String aboutMe;

    // Contact Information
    @Indexed
    private String email;

    @Indexed
    private String phone;

    // Profile Image
    private String imageUrl;
    private String imageUrl2;
    private String imageUrl3;

    // Status
    @Indexed
    private Boolean verified = false;

    private Boolean active = true;

    private Boolean shortlisted = false;

    // Statistics
    private Integer viewCount = 0;
    private String lastActive = "Today";

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- Helpers used by Thymeleaf templates ---

    public String getName() {
        String fn = (firstName == null) ? "" : firstName.trim();
        String ln = (lastName == null) ? "" : lastName.trim();
        String full = (fn + " " + ln).trim();
        return full.isEmpty() ? "User" : full;
    }

    public String getLocation() {
        String c = (city == null) ? "" : city.trim();
        String s = (state == null) ? "" : state.trim();

        if (!c.isEmpty() && !s.isEmpty()) return c + ", " + s;
        if (!c.isEmpty()) return c;
        if (!s.isEmpty()) return s;
        return "";
    }

    public Boolean getShortlisted() {
        return shortlisted != null ? shortlisted : false;
    }

    public Integer getCalculatedAge() {
        if (dateOfBirth == null) return null;
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    // --- Lifecycle hooks (call manually from service before save) ---

    public void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();

        if (viewCount == null) viewCount = 0;
        if (verified == null) verified = false;
        if (active == null) active = true;
        if (shortlisted == null) shortlisted = false;

        // Normalize religion/denomination to match frontend filters
        if (religion != null) religion = religion.trim().toLowerCase();
        if (denomination != null) denomination = denomination.trim().toLowerCase();
    }

    public void onUpdate() {
        updatedAt = LocalDateTime.now();

        if (religion != null) religion = religion.trim().toLowerCase();
        if (denomination != null) denomination = denomination.trim().toLowerCase();
    }
}