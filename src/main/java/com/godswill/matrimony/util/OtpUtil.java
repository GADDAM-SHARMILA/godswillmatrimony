package com.godswill.matrimony.util;

import java.security.SecureRandom;

public class OtpUtil {

    private static final SecureRandom random = new SecureRandom();

    // Generate 6-digit OTP
    public static String generateOtp() {
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    // Validate OTP format
    public static boolean isValidOtp(String otp) {
        return otp != null && otp.matches("\\d{6}");
    }

    // Validate email format (simple + safe)
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        String e = email.trim();
        // basic email validation
        return e.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    // Validate phone format (10 digits)
    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        return phone.trim().matches("\\d{10}");
    }

    // Mask phone for display (e.g., 98****10)
    public static String maskPhone(String phone) {
        if (phone == null) return null;
        String p = phone.trim();
        if (p.length() < 4) return p;
        return p.substring(0, 2) + "****" + p.substring(p.length() - 2);
    }

    // Mask email for display (e.g., a****@gmail.com)
    public static String maskEmail(String email) {
        if (email == null) return null;

        String e = email.trim();
        int at = e.indexOf('@');
        if (at <= 0 || at == e.length() - 1) return e;

        String local = e.substring(0, at);
        String domain = e.substring(at + 1);

        if (local.length() <= 2) {
            return local.charAt(0) + "****@" + domain;
        }

        return local.charAt(0) + "****" + local.charAt(local.length() - 1) + "@" + domain;
    }
}
