package com.godswill.matrimony.controller;

import com.godswill.matrimony.model.Profile;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.repository.ProfileRepository;
import com.godswill.matrimony.service.EmailService;
import com.godswill.matrimony.service.ImageStorageService;
import com.godswill.matrimony.service.ProfileService;
import com.godswill.matrimony.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final ProfileRepository profileRepository;
    private final ProfileService profileService;
    private final ImageStorageService imageStorageService;
    private final UserService userService;
    private final EmailService emailService;

    // ── Auth guard ──
    private boolean isAdmin(HttpSession session) {
        User u = (User) session.getAttribute("user");
        return u != null && u.getRole() != null && "ADMIN".equalsIgnoreCase(u.getRole());
    }

    /**
     * ✅ UPDATED: Convert base64 image to S3 URL
     * Returns full public S3 URL instead of local file path
     */
    private String saveBase64Image(String base64DataUrl) throws Exception {
        if (base64DataUrl == null || base64DataUrl.isBlank()) {
            return "";
        }

        String[] parts = base64DataUrl.split(",", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid base64 image format");
        }

        String meta = parts[0];                                    // e.g. "data:image/jpeg;base64"
        byte[] imageBytes = java.util.Base64.getDecoder().decode(parts[1]);
        String mimeType = meta.split(":")[1].split(";")[0];        // e.g. "image/jpeg"
        String extension = mimeType.split("/")[1];                 // e.g. "jpeg"
        String filename = "profile." + extension;

        // ✅ Returns full S3 URL — e.g. https://godswill-images.s3.ap-south-2.amazonaws.com/profiles/uuid.jpeg
        return imageStorageService.saveBytes(imageBytes, filename, mimeType);
    }

    // ── Root: redirect to all profiles ──
    @GetMapping
    public String adminHome(HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", "Access denied");
            return "redirect:/login";
        }
        return "redirect:/admin/profiles/all";
    }

    // ══════════════════════════════════════════════
    //  ALL PROFILES (no premium restriction)
    // ══════════════════════════════════════════════
    @GetMapping("/profiles/all")
    public String allProfiles(HttpSession session, Model model, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", "Access denied");
            return "redirect:/login";
        }

        List<Profile> all = profileRepository.findAll();

        long verifiedCount = all.stream().filter(p -> Boolean.TRUE.equals(p.getVerified())).count();
        long activeCount = all.stream().filter(p -> Boolean.TRUE.equals(p.getActive())).count();
        long pendingCount = all.size() - verifiedCount;

        model.addAttribute("profiles", all);
        model.addAttribute("totalCount", all.size());
        model.addAttribute("verifiedCount", verifiedCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("pageTitle", "Admin - All Profiles");
        model.addAttribute("activePage", "admin");
        return "admin-all-profiles";
    }

    // ══════════════════════════════════════════════
    //  PENDING PROFILES
    // ══════════════════════════════════════════════
    @GetMapping("/profiles/pending")
    public String pendingProfiles(HttpSession session, Model model, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", "Access denied");
            return "redirect:/login";
        }

        List<Profile> pending = profileRepository.findByVerified(false);
        model.addAttribute("profiles", pending);
        model.addAttribute("pageTitle", "Admin - Pending Profiles");
        model.addAttribute("activePage", "admin");
        return "admin-pending-profiles";
    }

    // ══════════════════════════════════════════════
    //  APPROVE
    // ══════════════════════════════════════════════
    @PostMapping("/profiles/{id}/approve")
    public String approve(@PathVariable String id, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", "Access denied");
            return "redirect:/login";
        }
        profileRepository.findById(id).ifPresent(p -> {
            p.setVerified(true);
            p.onUpdate();
            profileRepository.save(p);
        });
        ra.addFlashAttribute("success", "✅ Profile approved successfully!");
        return "redirect:/admin/profiles/pending";
    }

    // ══════════════════════════════════════════════
    //  REJECT / DELETE
    // ══════════════════════════════════════════════
    @PostMapping("/profiles/{id}/reject")
    public String reject(@PathVariable String id, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", "Access denied");
            return "redirect:/login";
        }
        profileRepository.findById(id).ifPresent(p -> {
            p.setActive(false);
            p.setVerified(false);
            p.onUpdate();
            profileRepository.save(p);
        });
        ra.addFlashAttribute("success", "🚫 Profile rejected.");
        return "redirect:/admin/profiles/pending";
    }

    @PostMapping("/profiles/{id}/delete")
    public String deleteProfile(@PathVariable String id, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", "Access denied");
            return "redirect:/login";
        }

        // ✅ Delete images from S3 before deleting profile
        Optional<Profile> profileOpt = profileRepository.findById(id);
        if (profileOpt.isPresent()) {
            Profile profile = profileOpt.get();
            if (profile.getImageUrl() != null && !profile.getImageUrl().isBlank()) {
                imageStorageService.delete(profile.getImageUrl());
            }
            if (profile.getImageUrl2() != null && !profile.getImageUrl2().isBlank()) {
                imageStorageService.delete(profile.getImageUrl2());
            }
            if (profile.getImageUrl3() != null && !profile.getImageUrl3().isBlank()) {
                imageStorageService.delete(profile.getImageUrl3());
            }
        }

        profileRepository.deleteById(id);
        ra.addFlashAttribute("success", "🗑️ Profile deleted.");
        return "redirect:/admin/profiles/all";
    }

    // ══════════════════════════════════════════════
    //  ADMIN CREATE USER + PROFILE (GET)
    // ══════════════════════════════════════════════
    @GetMapping("/profiles/create")
    public String showAdminCreateUser(HttpSession session, Model model, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", "Access denied");
            return "redirect:/login";
        }
        model.addAttribute("pageTitle", "Admin - Create User & Profile");
        return "admin-create-user";
    }

    // ══════════════════════════════════════════════
    //  ADMIN CREATE USER + PROFILE (POST)
    //  Mirrors the normal registration flow but:
    //  - Skips OTP/CAPTCHA
    //  - Auto-verifies email
    //  - Auto-approves profile
    // ══════════════════════════════════════════════
    @PostMapping("/users/create")
    public String adminCreateUserAndProfile(
            // --- User account fields ---
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam(required = false) String whatsapp,
            @RequestParam String gender,
            @RequestParam String dateOfBirth,
            @RequestParam(required = false) String maritalStatus,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            // --- Profile-only fields ---
            @RequestParam(required = false) Integer height,
            @RequestParam(required = false) String motherTongue,
            @RequestParam(required = false) String religion,
            @RequestParam(required = false) String denomination,
            @RequestParam(required = false) String caste,
            @RequestParam(required = false) String education,
            @RequestParam(required = false) String profession,
            @RequestParam(required = false) String annualIncome,
            @RequestParam(required = false) String employedIn,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String pincode,
            @RequestParam(required = false) String aboutMe,
            HttpSession session,
            RedirectAttributes ra) {

        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", "Access denied");
            return "redirect:/login";
        }

        // ── Validate passwords match ──
        if (!password.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "❌ Passwords do not match!");
            return "redirect:/admin/profiles/create";
        }

        // ── Check email/phone not already taken ──
        if (userService.existsByEmail(email)) {
            ra.addFlashAttribute("error", "❌ Email already registered: " + email);
            return "redirect:/admin/profiles/create";
        }
        if (userService.existsByPhone(phone)) {
            ra.addFlashAttribute("error", "❌ Phone already registered: " + phone);
            return "redirect:/admin/profiles/create";
        }

        try {
            // ══ 1. BUILD & SAVE USER ══
            User user = new User();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email.trim().toLowerCase());
            user.setPhone(phone.trim());
            user.setWhatsapp(whatsapp);
            user.setGender(gender);
            user.setDateOfBirth(LocalDate.parse(dateOfBirth));
            user.setRole("USER");
            user.setEmailVerified(true);   // ← Admin bypasses OTP
            user.setPassword(password);

            User savedUser = userService.registerUser(user);
            userService.markEmailVerified(savedUser.getId());

            // ══ 2. SEND WELCOME EMAIL ══
            try {
                emailService.sendAdminCreatedWelcomeEmail(
                        savedUser.getEmail(),
                        savedUser.getFirstName(),
                        password
                );
            } catch (Exception mailEx) {
                System.err.println("⚠️ Welcome email failed: " + mailEx.getMessage());
            }

            // ══ 3. BUILD & SAVE PROFILE ══
            Profile profile = new Profile();
            profile.setUserId(savedUser.getId());
            profile.setFirstName(firstName);
            profile.setLastName(lastName);
            profile.setEmail(email.trim().toLowerCase());
            profile.setPhone(phone.trim());
            profile.setGender(gender.trim().toLowerCase());
            profile.setDateOfBirth(LocalDate.parse(dateOfBirth));
            profile.setMaritalStatus(maritalStatus);
            profile.setHeight(height);
            profile.setMotherTongue(motherTongue);
            profile.setReligion(religion != null ? religion.trim().toLowerCase() : null);
            profile.setDenomination(denomination != null ? denomination.trim().toLowerCase() : null);
            profile.setCaste(caste);
            profile.setEducation(education);
            profile.setProfession(profession);
            profile.setAnnualIncome(annualIncome);
            profile.setEmployedIn(employedIn);
            profile.setCity(city);
            profile.setState(state);
            profile.setCountry(country != null ? country : "India");
            profile.setPincode(pincode);
            profile.setAboutMe(aboutMe);
            profile.setVerified(true);
            profile.setActive(true);
            profile.setViewCount(0);

            profileService.saveProfile(profile);

            ra.addFlashAttribute("success",
                    "✅ Account & profile created for " + firstName + " " + lastName +
                            ". Welcome email sent to: " + email);
            return "redirect:/admin/profiles/all";

        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("error", "❌ Error creating account: " + e.getMessage());
            return "redirect:/admin/profiles/create";
        }
    }

    // ══════════════════════════════════════════════
    //  ADMIN EDIT PROFILE (GET)
    // ══════════════════════════════════════════════
    @GetMapping("/profiles/{id}/edit")
    public String showAdminEditProfile(@PathVariable String id, HttpSession session,
                                       Model model, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", "Access denied");
            return "redirect:/login";
        }

        Optional<Profile> profileOpt = profileRepository.findById(id);
        if (profileOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Profile not found!");
            return "redirect:/admin/profiles/all";
        }

        model.addAttribute("profile", profileOpt.get());
        model.addAttribute("isPremium", true);
        model.addAttribute("isAdmin", true);
        model.addAttribute("pageTitle", "Admin - Edit Profile");
        return "profile-create";
    }

    // ══════════════════════════════════════════════
    //  ADMIN EDIT PROFILE (POST)
    // ══════════════════════════════════════════════
    @PostMapping("/profiles/{id}/edit")
    public String adminEditProfile(
            @PathVariable String id,
            @ModelAttribute Profile profile,
            @RequestParam(value = "profileImageData", required = false) String profileImageData,
            @RequestParam(value = "profileImageData2", required = false) String profileImageData2,
            @RequestParam(value = "profileImageData3", required = false) String profileImageData3,
            HttpSession session,
            RedirectAttributes ra) throws Exception {

        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", "Access denied");
            return "redirect:/login";
        }

        Optional<Profile> existingOpt = profileRepository.findById(id);
        if (existingOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Profile not found!");
            return "redirect:/admin/profiles/all";
        }

        Profile existing = existingOpt.get();
        profile.setId(id);
        profile.setUserId(existing.getUserId());
        profile.setProfileNumber(existing.getProfileNumber());

        // ✅ Updated: Handle S3 image URLs
        if (profileImageData != null && profileImageData.startsWith("data:image")) {
            // Delete old image from S3
            if (existing.getImageUrl() != null && !existing.getImageUrl().isBlank()) {
                imageStorageService.delete(existing.getImageUrl());
            }
            profile.setImageUrl(saveBase64Image(profileImageData));
        } else {
            profile.setImageUrl(existing.getImageUrl());
        }

        if (profileImageData2 != null && profileImageData2.startsWith("data:image")) {
            if (existing.getImageUrl2() != null && !existing.getImageUrl2().isBlank()) {
                imageStorageService.delete(existing.getImageUrl2());
            }
            profile.setImageUrl2(saveBase64Image(profileImageData2));
        } else {
            profile.setImageUrl2(existing.getImageUrl2());
        }

        if (profileImageData3 != null && profileImageData3.startsWith("data:image")) {
            if (existing.getImageUrl3() != null && !existing.getImageUrl3().isBlank()) {
                imageStorageService.delete(existing.getImageUrl3());
            }
            profile.setImageUrl3(saveBase64Image(profileImageData3));
        } else {
            profile.setImageUrl3(existing.getImageUrl3());
        }

        profile.onUpdate();
        profileRepository.save(profile);

        ra.addFlashAttribute("success", "✅ Profile updated successfully!");
        return "redirect:/admin/profiles/all";
    }
}