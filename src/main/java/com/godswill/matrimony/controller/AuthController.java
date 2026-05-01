package com.godswill.matrimony.controller;

import com.godswill.matrimony.model.Pastor;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.service.*;
import com.godswill.matrimony.util.OtpUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final ProfileService profileService;
    private final PastorService pastorService; // ✅ NEW
    private final CartService cartService;

    /* ================= REGISTER ================= */

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new User());
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute User user,
                           @RequestParam String confirmPassword,
                           @RequestParam String captcha,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {

        try {
            String sessionCaptcha = (String) session.getAttribute("captcha");
            if (sessionCaptcha == null || !sessionCaptcha.equalsIgnoreCase(captcha)) {
                redirectAttributes.addFlashAttribute("error", "❌ Invalid CAPTCHA!");
                redirectAttributes.addFlashAttribute("user", user);
                return "redirect:/register";
            }

            session.removeAttribute("captcha");

            if (!user.getPassword().equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Passwords do not match!");
                redirectAttributes.addFlashAttribute("user", user);
                return "redirect:/register";
            }

            if (!OtpUtil.isValidEmail(user.getEmail())) {
                redirectAttributes.addFlashAttribute("error", "Invalid email format!");
                redirectAttributes.addFlashAttribute("user", user);
                return "redirect:/register";
            }

            if (!OtpUtil.isValidPhone(user.getPhone())) {
                redirectAttributes.addFlashAttribute("error", "Phone must be 10 digits!");
                redirectAttributes.addFlashAttribute("user", user);
                return "redirect:/register";
            }

            if (userService.existsByEmail(user.getEmail())) {
                redirectAttributes.addFlashAttribute("error", "Email already registered!");
                return "redirect:/register";
            }

            if (userService.existsByPhone(user.getPhone())) {
                redirectAttributes.addFlashAttribute("error", "Phone already registered!");
                return "redirect:/register";
            }

            User savedUser = userService.registerUser(user);
            otpService.sendEmailOtp(savedUser.getEmail());

            session.setAttribute("otpUserId", savedUser.getId());
            session.setAttribute("otpEmail", savedUser.getEmail());

            redirectAttributes.addFlashAttribute("message", "OTP sent to your email!");
            return "redirect:/verify-otp";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Registration failed: " + e.getMessage());
            return "redirect:/register";
        }
    }

    /* ================= VERIFY OTP ================= */

    @GetMapping("/verify-otp")
    public String showVerifyOtp(HttpSession session,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        String userId = (String) session.getAttribute("otpUserId");
        String email  = (String) session.getAttribute("otpEmail");

        if (userId == null || email == null) {
            redirectAttributes.addFlashAttribute("error", "Session expired. Please register again.");
            return "redirect:/register";
        }

        model.addAttribute("userId", userId);
        model.addAttribute("email", email);
        return "verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(
            @RequestParam String emailOtp,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String userId = (String) session.getAttribute("otpUserId");
        String email  = (String) session.getAttribute("otpEmail");

        if (userId == null || email == null) {
            redirectAttributes.addFlashAttribute("error", "Session expired. Please register again.");
            return "redirect:/register";
        }

        try {
            boolean isValid = otpService.verifyEmailOtp(email, emailOtp);

            if (isValid) {
                userService.markEmailVerified(userId);

                try {
                    userService.findById(userId).ifPresent(user -> {
                        profileService.createProfileForUser(user);
                    });
                } catch (Exception profileEx) {
                    System.err.println("⚠️ Auto profile creation failed for userId="
                            + userId + ": " + profileEx.getMessage());
                }

                try {
                    emailService.sendWelcomeEmail(email, null);
                } catch (Exception mailEx) {
                    System.err.println("⚠️ Welcome email failed: " + mailEx.getMessage());
                }

                session.removeAttribute("otpUserId");
                session.removeAttribute("otpEmail");

                redirectAttributes.addFlashAttribute("success", "✅ Account verified! Please login.");
                return "redirect:/login";
            }

            redirectAttributes.addFlashAttribute("error", "❌ Invalid or expired OTP. Please try again.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ " + e.getMessage());
        }

        return "redirect:/verify-otp";
    }

    /* ================= RESEND OTP ================= */

    @PostMapping("/resend-otp")
    @ResponseBody
    public ResponseEntity<Map<String, String>> resendOtp(@RequestParam String email) {
        try {
            otpService.resendEmailOtp(email);
            return ResponseEntity.ok(Map.of("status", "success", "message", "OTP resent to " + email));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /* ================= LOGIN (Unified: User + Pastor) ================= */

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        @RequestParam String captcha,
                        @RequestParam(required = false) String guestId,  // ← NEW: sent from frontend
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {

        try {
            // ── 1. CAPTCHA check ──
            String sessionCaptcha = (String) session.getAttribute("captcha");
            if (sessionCaptcha == null || !sessionCaptcha.equalsIgnoreCase(captcha)) {
                redirectAttributes.addFlashAttribute("error", "Invalid CAPTCHA!");
                return "redirect:/login";
            }
            session.removeAttribute("captcha");

            // ── 2. Try USER / ADMIN login ──
            Optional<User> userOpt = userService.login(email, password);
            if (userOpt.isPresent()) {
                User user = userOpt.get();

                if (!Boolean.TRUE.equals(user.getEmailVerified())) {
                    redirectAttributes.addFlashAttribute("error", "Please verify your email first!");
                    return "redirect:/login";
                }

                session.setAttribute("user", user);

                // ── Migrate guest cart → user cart on login ──
                if (guestId != null && !guestId.isBlank()) {
                    try {
                        cartService.migrateGuestCartToUser(guestId, user.getId());
                    } catch (Exception e) {
                        System.err.println("⚠️ Cart migration failed: " + e.getMessage());
                    }
                }

                if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                    return "redirect:/admin";
                }

                return "redirect:/";
            }

            // ── 3. Try PASTOR login ──
            Pastor pastor = pastorService.findByEmail(email);
            if (pastor != null && pastor.getPassword().equals(password)) {
                session.setAttribute("loggedInPastor", pastor);
                return "redirect:/pastors/dashboard";
            }

            // ── 4. No match ──
            redirectAttributes.addFlashAttribute("error", "Invalid credentials!");
            return "redirect:/login";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Login failed: " + e.getMessage());
            return "redirect:/login";
        }
    }

    /* ================= FORGOT PASSWORD ================= */

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "forgotpassword/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email,
                                        RedirectAttributes redirectAttributes) {

        Optional<User> userOptional = userService.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));

            userService.updateUser(user);

            String baseUrl = ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .build()
                    .toUriString();

            String resetLink = baseUrl + "/reset-password?token=" + token;

            emailService.sendPasswordResetEmail(
                    user.getEmail(),
                    user.getFirstName(),
                    resetLink
            );
        }

        redirectAttributes.addFlashAttribute("message",
                "If the email exists, a reset link has been sent.");

        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPassword(@RequestParam String token,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {

        Optional<User> userOptional = userService.findByResetToken(token);

        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Invalid or expired link.");
            return "redirect:/forgot-password";
        }

        User user = userOptional.get();

        if (user.getResetTokenExpiry() == null ||
                user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {

            redirectAttributes.addFlashAttribute("error", "Reset link expired.");
            return "redirect:/forgot-password";
        }

        model.addAttribute("token", token);
        return "forgotpassword/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String token,
                                       @RequestParam String password,
                                       @RequestParam String confirmPassword,
                                       RedirectAttributes redirectAttributes) {

        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match!");
            return "redirect:/reset-password?token=" + token;
        }

        Optional<User> userOptional = userService.findByResetToken(token);

        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Invalid request.");
            return "redirect:/forgot-password";
        }

        User user = userOptional.get();

        if (user.getResetTokenExpiry() == null ||
                user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {

            redirectAttributes.addFlashAttribute("error", "Reset link expired.");
            return "redirect:/forgot-password";
        }

        user.setPassword(userService.encodePassword(password));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);

        userService.updateUser(user);

        redirectAttributes.addFlashAttribute("success",
                "Password reset successful. Please login.");

        return "redirect:/login";
    }

    /* ================= LOGOUT ================= */

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {

        // ── Clear user's cart on logout ──
        User user = (User) session.getAttribute("user");
        if (user != null) {
            try {
                cartService.clearCartsOnLogout(user.getId());
            } catch (Exception e) {
                System.err.println("⚠️ Cart clear on logout failed: " + e.getMessage());
            }
        }

        session.invalidate();
        redirectAttributes.addFlashAttribute("message", "Logged out successfully!");
        return "redirect:/";
    }
}