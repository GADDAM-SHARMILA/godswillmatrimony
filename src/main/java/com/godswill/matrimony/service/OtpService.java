package com.godswill.matrimony.service;

import com.godswill.matrimony.model.Otp;
import com.godswill.matrimony.repository.OtpRepository;
import com.godswill.matrimony.util.OtpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService;

    private static final int MAX_ATTEMPTS = 5;
    private static final String EMAIL_VERIFICATION = "EMAIL_VERIFICATION";

    public void sendEmailOtp(String email) {
        if (!OtpUtil.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // Delete old OTP for same email+purpose
        otpRepository.deleteByEmailAndPurpose(email, EMAIL_VERIFICATION);

        String otpCode = OtpUtil.generateOtp();

        Otp otp = new Otp();
        otp.setEmail(email.trim().toLowerCase());
        otp.setPurpose(EMAIL_VERIFICATION);
        otp.setOtpCode(otpCode);
        otp.setAttemptCount(0);
        otp.setVerified(false);
        otp.onCreate();

        otpRepository.save(otp);

        emailService.sendOtpEmail(email, otpCode);
        System.out.println("✅ Email OTP sent to: " + OtpUtil.maskEmail(email));
    }

    public boolean verifyEmailOtp(String email, String otpCode) {
        if (!OtpUtil.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (!OtpUtil.isValidOtp(otpCode)) {
            throw new IllegalArgumentException("Invalid OTP format (must be 6 digits)");
        }

        Optional<Otp> otpOptional = otpRepository.findByEmailAndPurpose(email.trim().toLowerCase(), EMAIL_VERIFICATION);

        if (otpOptional.isEmpty()) {
            throw new RuntimeException("OTP not found for this email");
        }

        Otp otp = otpOptional.get();

        if (otp.isExpired()) {
            otpRepository.delete(otp);
            throw new RuntimeException("OTP has expired. Please request a new one.");
        }

        if (otp.getAttemptCount() != null && otp.getAttemptCount() >= MAX_ATTEMPTS) {
            otpRepository.delete(otp);
            throw new RuntimeException("Maximum OTP verification attempts exceeded. Please request a new OTP.");
        }

        if (!otp.getOtpCode().equals(otpCode)) {
            int attempts = (otp.getAttemptCount() == null) ? 0 : otp.getAttemptCount();
            otp.setAttemptCount(attempts + 1);
            otpRepository.save(otp);

            int remaining = MAX_ATTEMPTS - otp.getAttemptCount();
            throw new RuntimeException("Invalid OTP. " + remaining + " attempts remaining.");
        }

        // ✅ Verified: delete OTP (clean) OR keep verified flag.
        otpRepository.delete(otp);

        System.out.println("✅ Email OTP verified for: " + email);
        return true;
    }

    // Optional helper (OTP collection only — not user table)
    public boolean isOtpVerifiedForEmail(String email) {
        Optional<Otp> otp = otpRepository.findByEmailAndPurpose(email.trim().toLowerCase(), EMAIL_VERIFICATION);
        return otp.isPresent() && Boolean.TRUE.equals(otp.get().getVerified());
    }

    public void resendEmailOtp(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must be provided");
        }
        sendEmailOtp(email);
    }
}
