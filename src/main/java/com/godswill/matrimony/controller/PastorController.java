package com.godswill.matrimony.controller;

import com.godswill.matrimony.model.Pastor;
import com.godswill.matrimony.model.Profile;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.service.EmailService;
import com.godswill.matrimony.service.PastorService;
import com.godswill.matrimony.service.ProfileService;
import com.godswill.matrimony.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Controller
@RequestMapping("/pastors")
public class PastorController {

    @Autowired
    private PastorService pastorService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    // ================= REGISTER =================

    @GetMapping("/register")
    public String showForm(Model model) {
        model.addAttribute("pastor", new Pastor());
        return "pastors-register";
    }

    @PostMapping("/register")
    public String savePastor(@ModelAttribute("pastor") Pastor pastor, Model model) {
        if (!pastor.passwordsMatch()) {
            model.addAttribute("errorMessage", "Passwords do not match!");
            return "pastors-register";
        }
        pastorService.savePastor(pastor);
        return "redirect:/login";
    }

    // ================= LOGIN (kept for backward compatibility) =================

    @GetMapping("/login")
    public String showLoginForm() {
        return "redirect:/login";
    }

    @PostMapping("/login")
    public String loginPastor(@RequestParam String email,
                              @RequestParam String password,
                              HttpSession session) {
        return "redirect:/login";
    }

    // ================= DASHBOARD =================

    @GetMapping("/dashboard")
    public String pastorDashboard(Model model, HttpSession session) {
        Pastor pastor = (Pastor) session.getAttribute("loggedInPastor");
        if (pastor == null) return "redirect:/login";

        model.addAttribute("pastor", pastor);
        model.addAttribute("profile", new Profile());

        List<Profile> profiles = profileService.getProfilesByPastor(pastor.getId());

        for (Profile p : profiles) {
            if (p.getDateOfBirth() != null) {
                p.setAge(Period.between(p.getDateOfBirth(), LocalDate.now()).getYears());
            }
        }

        model.addAttribute("profiles", profiles);
        return "pastors-dashboard";
    }

    // ================= ADD PROFILE =================

    @PostMapping("/profiles")
    public String addProfile(@ModelAttribute("profile") Profile profile,
                             HttpSession session,
                             RedirectAttributes ra) {

        Pastor pastor = (Pastor) session.getAttribute("loggedInPastor");
        if (pastor == null) return "redirect:/login";

        // ── Duplicate email check ──
        if (profile.getEmail() != null && userService.existsByEmail(profile.getEmail().trim().toLowerCase())) {
            ra.addFlashAttribute("errorMessage", "❌ A user with this email already exists: " + profile.getEmail());
            return "redirect:/pastors/dashboard";
        }

        // ── Duplicate phone check ──
        if (profile.getPhone() != null && userService.existsByPhone(profile.getPhone().trim())) {
            ra.addFlashAttribute("errorMessage", "❌ A user with this phone number already exists: " + profile.getPhone());
            return "redirect:/pastors/dashboard";
        }

        try {
            // ── 1. Generate a random password ──
            String generatedPassword = generateRandomPassword();

            // ── 2. Create User account ──
            User user = new User();
            user.setFirstName(profile.getFirstName());
            user.setLastName(profile.getLastName());
            user.setEmail(profile.getEmail().trim().toLowerCase());
            user.setPhone(profile.getPhone().trim());
            user.setGender(profile.getGender());
            user.setDateOfBirth(profile.getDateOfBirth());
            user.setRole("USER");
            user.setEmailVerified(true);  // Pastor-created = auto-verified
            user.setPassword(generatedPassword);

            User savedUser = userService.registerUser(user);
            userService.markEmailVerified(savedUser.getId());

            // ── 3. Send welcome email with login credentials ──
            try {
                emailService.sendAdminCreatedWelcomeEmail(
                        savedUser.getEmail(),
                        savedUser.getFirstName(),
                        generatedPassword  // plain-text before encoding
                );
            } catch (Exception mailEx) {
                System.err.println("⚠️ Welcome email failed: " + mailEx.getMessage());
            }

            // ── 4. Save Profile linked to the new user ──
            profile.setUserId(savedUser.getId());
            profile.setProposedByPastorId(pastor.getId());
            profile.setProposedByPastorName(pastor.getFirstName() + " " + pastor.getLastName());
            profile.setVerified(false);  // Still needs admin approval
            profile.setActive(true);

            if (profile.getDateOfBirth() != null) {
                profile.setAge(Period.between(profile.getDateOfBirth(), LocalDate.now()).getYears());
            }

            profileService.saveProfile(profile);
            ra.addFlashAttribute("successMessage", "✅ Profile created and welcome email sent to " + profile.getEmail());

        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("errorMessage", "❌ Error creating profile: " + e.getMessage());
        }

        return "redirect:/pastors/dashboard";
    }

    // ================= EDIT PROFILE =================

    @GetMapping("/profiles/edit/{id}")
    public String editProfile(@PathVariable String id,
                              Model model,
                              HttpSession session) {

        Pastor pastor = (Pastor) session.getAttribute("loggedInPastor");
        if (pastor == null) return "redirect:/login";

        Optional<Profile> profileOpt = profileService.getProfileById(id);
        if (profileOpt.isEmpty()) return "redirect:/pastors/dashboard";

        Profile profile = profileOpt.get();

        if (!profile.getProposedByPastorId().equals(pastor.getId())) {
            return "redirect:/pastors/dashboard";
        }

        model.addAttribute("profile", profile);
        return "profile-create";
    }

    // ================= UPDATE PROFILE =================

    @PostMapping("/profiles/update")
    public String updateProfile(@ModelAttribute("profile") Profile profile,
                                HttpSession session) {

        Pastor pastor = (Pastor) session.getAttribute("loggedInPastor");
        if (pastor == null) return "redirect:/login";

        if (profile.getDateOfBirth() != null) {
            profile.setAge(Period.between(profile.getDateOfBirth(), LocalDate.now()).getYears());
        }

        profile.setProposedByPastorId(pastor.getId());
        profile.setProposedByPastorName(pastor.getFirstName() + " " + pastor.getLastName());

        profileService.saveProfile(profile);
        return "redirect:/pastors/dashboard";
    }

    // ================= DELETE PROFILE =================

    @GetMapping("/profiles/delete/{id}")
    public String deleteProfile(@PathVariable String id,
                                HttpSession session) {

        Pastor pastor = (Pastor) session.getAttribute("loggedInPastor");
        if (pastor == null) return "redirect:/login";

        Optional<Profile> profileOpt = profileService.getProfileById(id);
        if (profileOpt.isEmpty()) return "redirect:/pastors/dashboard";

        Profile profile = profileOpt.get();

        if (!profile.getProposedByPastorId().equals(pastor.getId())) {
            return "redirect:/pastors/dashboard";
        }

        profileService.deleteProfile(id);
        return "redirect:/pastors/dashboard";
    }

    // ================= VIEW PROFILE =================

    @GetMapping("/profiles/view/{id}")
    public String viewProfile(@PathVariable String id,
                              Model model,
                              HttpSession session) {

        Pastor pastor = (Pastor) session.getAttribute("loggedInPastor");
        if (pastor == null) return "redirect:/login";

        Optional<Profile> profileOpt = profileService.getProfileById(id);
        if (profileOpt.isEmpty()) return "redirect:/pastors/dashboard";

        Profile profile = profileOpt.get();

        if (!profile.getProposedByPastorId().equals(pastor.getId())) {
            return "redirect:/pastors/dashboard";
        }

        model.addAttribute("profile", profile);
        return "profile-details";
    }

    // ================= LOGOUT =================

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // ================= HELPER =================

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$!";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}