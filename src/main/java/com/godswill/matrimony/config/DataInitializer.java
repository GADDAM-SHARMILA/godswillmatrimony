package com.godswill.matrimony.config;

import com.godswill.matrimony.model.Profile;
import com.godswill.matrimony.model.SuccessStory;
import com.godswill.matrimony.repository.ProfileRepository;
import com.godswill.matrimony.repository.SuccessStoryRepository;
import com.godswill.matrimony.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProfileRepository profileRepository;
    private final SuccessStoryRepository successStoryRepository;
    private final ImageStorageService imageStorageService;

    @Override
    public void run(String... args) {
        System.out.println("🌱 Checking DB before seeding...");

        if (profileRepository.count() == 0) {
            createSampleProfiles();
            System.out.println("✅ Sample Profiles Added");
        } else {
            System.out.println("⏭ Profiles already exist. Skipping...");
        }

        if (successStoryRepository.count() == 0) {
            createSampleSuccessStories();
            System.out.println("✅ Sample Success Stories Added");
        } else {
            System.out.println("⏭ Success Stories already exist. Skipping...");
        }

        System.out.println("📊 Total Profiles: " + profileRepository.count());
        System.out.println("💑 Total Success Stories: " + successStoryRepository.count());
    }

    private void createSampleProfiles() {
        Profile profile1 = new Profile();
        profile1.setFirstName("Elisa");
        profile1.setLastName("Hope");
        profile1.setGender("female");
        profile1.setDateOfBirth(LocalDate.of(1995, 5, 15));
        profile1.setAge(28);
        profile1.setMaritalStatus("Never Married");
        profile1.setHeight(165);
        profile1.setMotherTongue("Hindi");
        profile1.setReligion("christian");
        profile1.setDenomination("csi");
        profile1.setCountry("India");
        profile1.setState("Maharashtra");
        profile1.setCity("Mumbai");
        profile1.setEducation("Master's Degree");
        profile1.setProfession("Software Engineer");
        profile1.setAnnualIncome("₹10-15 Lakhs");
        profile1.setEmployedIn("Private Sector");
        profile1.setAboutMe("Simple and family-oriented.");
        profile1.setEmail("priya.sharma@example.com");
        profile1.setPhone("+919876543210");

        // ✅ FIXED: Try to upload image, fallback to placeholder if fails
        profile1.setImageUrl(uploadImageFromResources("girl_1.jpg"));

        profile1.setVerified(true);
        profile1.onCreate();

        Profile profile2 = new Profile();
        profile2.setFirstName("Mathew");
        profile2.setLastName("Hai");
        profile2.setGender("male");
        profile2.setDateOfBirth(LocalDate.of(1992, 8, 20));
        profile2.setAge(31);
        profile2.setMaritalStatus("Never Married");
        profile2.setHeight(175);
        profile2.setMotherTongue("Gujarati");
        profile2.setReligion("converted_christian");
        profile2.setDenomination("baptist");
        profile2.setCountry("India");
        profile2.setState("Gujarat");
        profile2.setCity("Ahmedabad");
        profile2.setEducation("Bachelor's Degree");
        profile2.setProfession("Business Owner");
        profile2.setAnnualIncome("₹15+ Lakhs");
        profile2.setEmployedIn("Business");
        profile2.setAboutMe("Family-oriented person.");
        profile2.setEmail("rahul.patel@example.com");
        profile2.setPhone("+919876543211");

        // ✅ FIXED: Try to upload image, fallback to placeholder if fails
        profile2.setImageUrl(uploadImageFromResources("boy_1.jpg"));

        profile2.setVerified(true);
        profile2.onCreate();

        profileRepository.saveAll(Arrays.asList(profile1, profile2));
    }

    /**
     * ✅ FIXED: Upload image from resources to S3
     * Returns S3 URL on success, placeholder on failure
     * @param filename - image filename to load from resources
     * @return S3 URL or placeholder
     */
    private String uploadImageFromResources(String filename) {
        try {
            ClassPathResource imgFile = new ClassPathResource("static/images/" + filename);

            // Check if file exists
            if (!imgFile.exists()) {
                System.out.println("⚠️ Image not found: static/images/" + filename);
                return getPlaceholderUrl(filename);
            }

            // Read image bytes
            byte[] imageBytes = Files.readAllBytes(imgFile.getFile().toPath());
            String contentType = getContentType(filename);

            // Upload to S3
            String s3Url = imageStorageService.saveBytes(imageBytes, filename, contentType);
            System.out.println("✅ Image uploaded to S3: " + s3Url);
            return s3Url;

        } catch (IOException e) {
            System.out.println("⚠️ Error uploading image: " + e.getMessage());
            return getPlaceholderUrl(filename);
        } catch (Exception e) {
            System.out.println("⚠️ Unexpected error: " + e.getMessage());
            return getPlaceholderUrl(filename);
        }
    }

    /**
     * ✅ FIXED: Get correct MIME type based on filename
     */
    private String getContentType(String filename) {
        if (filename == null) return "image/jpeg";

        if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (filename.toLowerCase().endsWith(".png")) {
            return "image/png";
        } else if (filename.toLowerCase().endsWith(".gif")) {
            return "image/gif";
        } else {
            return "image/jpeg"; // Default
        }
    }

    /**
     * ✅ FIXED: Return placeholder URL if image doesn't exist or fails to upload
     */
    private String getPlaceholderUrl(String filename) {
        // Option 1: Use a public placeholder service
        if (filename.contains("girl") || filename.contains("female")) {
            return "https://via.placeholder.com/200?text=Female+Profile";
        } else if (filename.contains("boy") || filename.contains("male")) {
            return "https://via.placeholder.com/200?text=Male+Profile";
        } else {
            return "https://via.placeholder.com/200?text=Couple";
        }
    }

    /**
     * ✅ FIXED: Create sample success stories with S3 images
     */
    private void createSampleSuccessStories() {
        SuccessStory story1 = new SuccessStory();
        story1.setCoupleName("Rahul & Priya");
        story1.setStory("We found each other here! It's been a wonderful journey together. We're grateful to this platform for bringing us together.");
        story1.setMarriageDate(LocalDate.of(2024, 1, 15));
        story1.setLocation("Mumbai, India");

        // ✅ FIXED: Upload image to S3 with error handling
        String s3ImageUrl = uploadImageFromResources("cpl_1.jpg");
        story1.setImageUrl(s3ImageUrl);
        story1.setImageType("image/jpeg");

        story1.onCreate();
        successStoryRepository.save(story1);
    }
}