package com.godswill.matrimony.controller;

import com.godswill.matrimony.model.Profile;
import com.godswill.matrimony.model.Subscription;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.service.ImageStorageService;
import com.godswill.matrimony.service.ProfileService;
import com.godswill.matrimony.service.SubscriptionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final ImageStorageService imageStorageService;
    private final SubscriptionService subscriptionService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    // ── Helper: convert base64 data-URL to MultipartFile and save ──
    private String saveBase64Image(String base64DataUrl) throws Exception {
        String[] parts = base64DataUrl.split(",", 2);
        String metadata = parts[0];
        byte[] imageBytes = Base64.getDecoder().decode(parts[1]);
        String mimeType = metadata.split(":")[1].split(";")[0];
        String extension = mimeType.split("/")[1];
        String filename = "cropped." + extension;
        MultipartFile mockFile = new MockMultipartFile("file", filename, mimeType, imageBytes);
        return "/uploads/" + imageStorageService.save(mockFile);
    }

    // ── Helper: check if logged-in user is admin ──
    private boolean isAdmin(HttpSession session) {
        User u = (User) session.getAttribute("user");
        return u != null && u.getRole() != null && "ADMIN".equalsIgnoreCase(u.getRole());
    }

    @GetMapping("/profiles")
    public String listProfiles(
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String religion,
            @RequestParam(required = false) String denomination,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) Integer ageFrom,
            @RequestParam(required = false) Integer ageTo,
            @RequestParam(required = false) String maritalStatus,
            @RequestParam(required = false) String education,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model,
            HttpSession session
    ) {
        if (page < 0) page = 0;
        if (size < 1 || size > 50) size = 20;

        List<Profile> allProfiles;

        if (q != null && !q.isBlank()) {
            allProfiles = profileService.searchProfiles(q);
        } else if (gender != null || religion != null || denomination != null || verified != null ||
                ageFrom != null || ageTo != null || maritalStatus != null || education != null || location != null) {
            allProfiles = profileService.filterProfiles(
                    gender, religion, denomination, verified, ageFrom, ageTo, maritalStatus, education, location
            );
        } else {
            allProfiles = profileService.getAllProfiles();
        }

        int totalProfiles = allProfiles.size();
        int totalPages = (totalProfiles == 0) ? 1 : (int) Math.ceil((double) totalProfiles / size);
        if (page >= totalPages) page = totalPages - 1;

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalProfiles);
        List<Profile> pagedProfiles = (fromIndex < totalProfiles)
                ? allProfiles.subList(fromIndex, toIndex)
                : List.of();

        User loggedInUser = (User) session.getAttribute("user");
        boolean isLoggedIn = loggedInUser != null;
        boolean admin = isAdmin(session);
        boolean isPremium = admin || (isLoggedIn && loggedInUser.isPremium());

        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("isPremium", isPremium);
        model.addAttribute("isAdmin", admin);
        model.addAttribute("profiles", pagedProfiles);
        model.addAttribute("activePage", "profiles");
        model.addAttribute("pageTitle", "Browse Profiles - God's Will Matrimony");
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalProfiles", totalProfiles);
        model.addAttribute("pageSize", size);

        return "profile-listing";
    }

    @GetMapping("/profile/{id}")
    public String viewProfile(@PathVariable String id, Model model, HttpSession session) {
        Optional<Profile> profileOpt = profileService.getProfileById(id);
        if (profileOpt.isEmpty()) return "redirect:/profiles";

        Profile profile = profileOpt.get();
        User loggedInUser = (User) session.getAttribute("user");

        boolean isLoggedIn = loggedInUser != null;
        boolean admin = isAdmin(session);
        boolean isPremium = admin || (isLoggedIn && loggedInUser.isPremium());

        model.addAttribute("profile", profile);
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("isPremium", isPremium);
        model.addAttribute("isAdmin", admin);
        model.addAttribute("canViewFullProfile", isPremium);
        model.addAttribute("canViewContact", isPremium);
        model.addAttribute("similarProfiles", List.of());
        model.addAttribute("pageTitle",
                profile.getFirstName() + " " + profile.getLastName() + " - Profile Details");

        return "profile-details";
    }

    @GetMapping("/profile/create")
    public String showCreateProfileForm(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute("error", "Please login first!");
            return "redirect:/login";
        }

        if (isAdmin(session)) {
            return "redirect:/admin/profiles/create";
        }

        model.addAttribute("profile", new Profile());
        model.addAttribute("isPremium", loggedInUser.isPremium());
        model.addAttribute("isAdmin", false);
        model.addAttribute("pageTitle", "Create Profile - God's Will Matrimony");
        return "profile-create";
    }

    @PostMapping("/profile/create")
    public String createProfile(
            @ModelAttribute Profile profile,
            @RequestParam(value = "profileImageData",  required = false) String profileImageData,
            @RequestParam(value = "profileImageData2", required = false) String profileImageData2,
            @RequestParam(value = "profileImageData3", required = false) String profileImageData3,
            HttpSession session,
            RedirectAttributes redirectAttributes) throws Exception {

        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser == null) return "redirect:/login";

        if (isAdmin(session)) {
            return "redirect:/admin/profiles/create";
        }

        profile.setUserId(loggedInUser.getId());

        if (profile.getReligion() != null)     profile.setReligion(profile.getReligion().trim().toLowerCase());
        if (profile.getDenomination() != null) profile.setDenomination(profile.getDenomination().trim().toLowerCase());

        Optional<Profile> existing = profileService.getProfileByUserId(loggedInUser.getId());

        if (profileImageData != null && profileImageData.startsWith("data:image")) {
            profile.setImageUrl(saveBase64Image(profileImageData));
        } else if (existing.isPresent()) {
            profile.setImageUrl(existing.get().getImageUrl());
        }

        boolean isPremium = loggedInUser.isPremium();

        if (isPremium) {
            if (profileImageData2 != null && profileImageData2.startsWith("data:image")) {
                profile.setImageUrl2(saveBase64Image(profileImageData2));
            } else if (existing.isPresent()) {
                profile.setImageUrl2(existing.get().getImageUrl2());
            }

            if (profileImageData3 != null && profileImageData3.startsWith("data:image")) {
                profile.setImageUrl3(saveBase64Image(profileImageData3));
            } else if (existing.isPresent()) {
                profile.setImageUrl3(existing.get().getImageUrl3());
            }
        } else {
            if (existing.isPresent()) {
                profile.setImageUrl2(existing.get().getImageUrl2());
                profile.setImageUrl3(existing.get().getImageUrl3());
            }
        }

        Profile saved = profileService.createProfile(profile);
        redirectAttributes.addFlashAttribute("success", "✅ Profile saved successfully!");
        return "redirect:/profile/" + saved.getId();
    }

    @GetMapping("/profile/my-matrimony-profile")
    public String viewMyMatrimonyProfile(HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser == null) return "redirect:/login";

        Optional<Profile> profileOpt = profileService.getProfileByUserId(loggedInUser.getId());
        if (profileOpt.isEmpty()) return "redirect:/profile/create";

        return "redirect:/profile/" + profileOpt.get().getId();
    }

    @GetMapping("/profile/edit")
    public String editProfile(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute("error", "Please login first!");
            return "redirect:/login";
        }

        Optional<Profile> profileOpt = profileService.getProfileByUserId(loggedInUser.getId());
        if (profileOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Profile not found!");
            return "redirect:/profile/create";
        }

        model.addAttribute("profile", profileOpt.get());
        model.addAttribute("isPremium", loggedInUser.isPremium());
        model.addAttribute("isAdmin", false);
        model.addAttribute("pageTitle", "Edit Profile - God's Will Matrimony");
        return "profile-create";
    }

    @GetMapping("/my-profile")
    public String myProfile(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute("error", "Please login first!");
            return "redirect:/login";
        }

        model.addAttribute("user", loggedInUser);
        model.addAttribute("pageTitle", "My Profile - God's Will Matrimony");

        // Fetch matrimony profile → gives us profileNumber (same as profile-details page)
        Optional<Profile> profileOpt = profileService.getProfileByUserId(loggedInUser.getId());
        model.addAttribute("userProfile", profileOpt.orElse(null));

        // Pass active subscription for receipt download
        Optional<Subscription> activeSub = subscriptionService.getActiveSubscription(loggedInUser.getId());
        model.addAttribute("activeSubscription", activeSub.orElse(null));

        return "user-profile";
    }
}