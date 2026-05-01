package com.godswill.matrimony.config;

import com.godswill.matrimony.model.User;
import com.godswill.matrimony.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds (creates) an ADMIN user at application startup if it doesn't exist.
 * If it already exists, it ensures the role is ADMIN.
 *
 * NOTE: Your app currently uses plain-text passwords in UserService.login(),
 * so this initializer also stores the password as plain-text.
 * (Do NOT do this in production; switch to BCrypt later.)
 */
@Component
@RequiredArgsConstructor
public class    AdminUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    // ✅ Change these values if you want different admin credentials
    private static final String ADMIN_FIRST_NAME = "Admin";
    private static final String ADMIN_LAST_NAME = "User";
    private static final String ADMIN_EMAIL = "admin@godswillmatrimony.in";
    private static final String ADMIN_PHONE = "9999999999";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String ADMIN_GENDER = "male";

    @Override
    public void run(String... args) {
        String email = ADMIN_EMAIL.trim().toLowerCase();

        System.out.println("🌱 AdminUserInitializer running... checking admin: " + email);

        userRepository.findByEmail(email).ifPresentOrElse(existing -> {
            boolean changed = false;

            // Ensure ADMIN role
            if (existing.getRole() == null || !"ADMIN".equalsIgnoreCase(existing.getRole())) {
                existing.setRole("ADMIN");
                changed = true;
            }

            // Ensure verified (so admin can login immediately)
            if (!Boolean.TRUE.equals(existing.getEmailVerified())) {
                existing.setEmailVerified(true);
                changed = true;
            }

            // Optional: ensure phone is set (helps if you created admin partially earlier)
            if (existing.getPhone() == null || existing.getPhone().isBlank()) {
                existing.setPhone(ADMIN_PHONE);
                changed = true;
            }

            if (changed) {
                existing.onUpdate();
                userRepository.save(existing);
                System.out.println("✅ Admin user updated/promoted: " + email);
            } else {
                System.out.println("⏭ Admin already exists: " + email);
            }

        }, () -> {
            User admin = new User();
            admin.setFirstName(ADMIN_FIRST_NAME);
            admin.setLastName(ADMIN_LAST_NAME);
            admin.setEmail(email);
            admin.setPhone(ADMIN_PHONE);
            admin.setPassword(ADMIN_PASSWORD); // plain-text (matches your current login())
            admin.setGender(ADMIN_GENDER);

            admin.setEmailVerified(true);
            admin.setRole("ADMIN");

            admin.onCreate();
            userRepository.save(admin);

            System.out.println("✅ Admin user created: " + email);
            System.out.println("🔐 Admin login -> email: " + email + " | password: " + ADMIN_PASSWORD);
        });
    }
}