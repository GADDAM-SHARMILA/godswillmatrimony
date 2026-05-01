package com.godswill.matrimony.service;

import com.godswill.matrimony.model.User;
import com.godswill.matrimony.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // 🔐 Injected from SecurityConfig @Bean — no need to instantiate manually
    private final PasswordEncoder passwordEncoder;

    /* ================= REGISTER ================= */

    public User registerUser(User user) {
        user.onCreate();

        // 🔐 BCrypt-encode password before saving it
        user.setPassword(encodePassword(user.getPassword()));

        return userRepository.save(user);
    }

    /* ================= UPDATE ================= */

    public User updateUser(User user) {
        user.onUpdate();
        return userRepository.save(user);
    }

    /* ================= MARK EMAIL VERIFIED ================= */

    public void markEmailVerified(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setEmailVerified(true);
            user.onUpdate();
            userRepository.save(user);
        });
    }

    /* ================= LOGIN ================= */

    public Optional<User> login(String emailOrPhone, String password) {

        if (emailOrPhone == null || emailOrPhone.isBlank() || password == null) {
            return Optional.empty();
        }

        String key = emailOrPhone.trim();
        Optional<User> userOpt =
                userRepository.findByEmailOrPhone(key.toLowerCase(), key);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // 🔐 BCrypt compare
            if (matchesPassword(password, user.getPassword())) {
                user.setLastLogin(LocalDateTime.now());
                user.onUpdate();
                userRepository.save(user);
                return Optional.of(user);
            }
        }

        return Optional.empty();
    }

    /* ================= PASSWORD RESET SUPPORT ================= */

    public Optional<User> findByResetToken(String token) {
        return userRepository.findByResetToken(token);
    }

    // 🔐 BCrypt encode
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    // 🔐 BCrypt match
    public boolean matchesPassword(String rawPassword, String storedPassword) {
        return passwordEncoder.matches(rawPassword, storedPassword);
    }

    /* ================= LOOKUPS ================= */

    public boolean existsByEmail(String email) {
        if (email == null) return false;
        return userRepository.existsByEmail(email.trim().toLowerCase());
    }

    public boolean existsByPhone(String phone) {
        if (phone == null) return false;
        return userRepository.existsByPhone(phone.trim());
    }

    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return userRepository.findByEmail(email.trim().toLowerCase());
    }
}