package com.godswill.matrimony.controller;

import com.godswill.matrimony.model.Profile;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.repository.ProfileRepository;
import com.godswill.matrimony.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/discount")
public class DiscountController {

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * POST /api/discount/check
     * Body: { "profileNumber": "MAT10001" }
     *
     * Returns:
     *  - isPremium: true/false
     *  - discountPercent: 10 (if premium) or 0
     *  - message: human-readable status
     *  - profileName: name to show in UI
     */
    @PostMapping("/check")
    public ResponseEntity<?> checkDiscount(@RequestBody Map<String, String> request) {
        String profileNumber = request.get("profileNumber");

        if (profileNumber == null || profileNumber.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Profile number is required"));
        }

        // Step 1: Find profile by profileNumber
        Optional<Profile> profileOpt = profileRepository.findByProfileNumber(profileNumber.trim().toUpperCase());

        if (profileOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "found",           false,
                    "isPremium",       false,
                    "discountPercent", 0,
                    "message",         "Profile number not found. Please check and try again."
            ));
        }

        Profile profile = profileOpt.get();

        // Step 2: Find the user linked to this profile
        if (profile.getUserId() == null) {
            return ResponseEntity.ok(Map.of(
                    "found",           true,
                    "isPremium",       false,
                    "discountPercent", 0,
                    "profileName",     profile.getName(),
                    "message",         "This profile is not linked to a user account."
            ));
        }

        Optional<User> userOpt = userRepository.findById(profile.getUserId());

        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "found",           true,
                    "isPremium",       false,
                    "discountPercent", 0,
                    "profileName",     profile.getName(),
                    "message",         "User account not found for this profile."
            ));
        }

        User user = userOpt.get();

        // Step 3: Check premium status using existing isPremium() method
        boolean isPremium = user.isPremium();

        if (isPremium) {
            return ResponseEntity.ok(Map.of(
                    "found",           true,
                    "isPremium",       true,
                    "discountPercent", 10,
                    "profileName",     profile.getName(),
                    "plan",            user.getCurrentPlan().name(),
                    "message",         "🎉 Premium member! You get 10% off on your order."
            ));
        } else {
            String reason = (user.isSubscriptionExpired())
                    ? "Your premium subscription has expired."
                    : "This profile does not have an active premium plan.";

            return ResponseEntity.ok(Map.of(
                    "found",           true,
                    "isPremium",       false,
                    "discountPercent", 0,
                    "profileName",     profile.getName(),
                    "message",         reason
            ));
        }
    }
}