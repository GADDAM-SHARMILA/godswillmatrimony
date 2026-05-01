package com.godswill.matrimony.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${zoho.zeptomail.api-key}")
    private String apiKey;

    @Value("${zoho.zeptomail.payment-template-key:}")
    private String paymentTemplateKey;

    @Value("${zoho.zeptomail.api-url}")
    private String apiUrl;

    @Value("${zoho.zeptomail.from-email}")
    private String fromEmail;

    @Value("${zoho.zeptomail.from-name}")
    private String fromName;

    @Value("${zoho.zeptomail.otp-template-key:}")
    private String otpTemplateKey;

    @Value("${zoho.zeptomail.welcome-template-key:}")
    private String welcomeTemplateKey;

    @Value("${zoho.zeptomail.admin-welcome-template-key:}")
    private String adminWelcomeTemplateKey;

    @Value("${zoho.zeptomail.reset-template-key:}")
    private String resetTemplateKey;

    private static final int MAX_RETRIES = 1;
    private static final long RETRY_DELAY = 2000;

    /* ================= OTP ================= */

    public void sendOtpEmail(String toEmail, String otp) {
        sendEmailAsync(toEmail, Map.of("otp_code", otp), otpTemplateKey);
    }

    /* ================= WELCOME (normal self-registration) ================= */

    public void sendWelcomeEmail(String toEmail, String firstName) {
        String safeName = (firstName == null || firstName.isBlank())
                ? "User"
                : firstName.trim();

        sendEmailAsync(toEmail, Map.of("first_name", safeName), welcomeTemplateKey);
    }

    /* ================= WELCOME (admin-created account) ================= */

    /**
     * Sent when admin creates an account on behalf of a user.
     * Includes their login credentials so they know how to access the platform.
     *
     * Merge fields used in ZeptoMail template:
     *   {{first_name}}  - User's first name
     *   {{email}}       - Their login email
     *   {{password}}    - Their temporary password (plain text)
     *
     * ⚠️  Add this key to application.properties:
     *     zoho.zeptomail.admin-welcome-template-key=YOUR_TEMPLATE_KEY_HERE
     */
    public void sendAdminCreatedWelcomeEmail(String toEmail,
                                             String firstName,
                                             String plainPassword) {

        String safeName = (firstName == null || firstName.isBlank())
                ? "User"
                : firstName.trim();

        Map<String, String> mergeInfo = new HashMap<>();
        mergeInfo.put("first_name", safeName);
        mergeInfo.put("email",      toEmail);
        mergeInfo.put("password",   plainPassword);

        // Falls back to normal welcome template if admin-specific one isn't configured
        String templateKey = (adminWelcomeTemplateKey != null && !adminWelcomeTemplateKey.isBlank())
                ? adminWelcomeTemplateKey
                : welcomeTemplateKey;

        sendEmailAsync(toEmail, mergeInfo, templateKey);
    }

    /* ================= RESET PASSWORD ================= */

    public void sendPasswordResetEmail(String toEmail, String firstName, String resetLink) {

        String safeName = (firstName == null || firstName.isBlank())
                ? "User"
                : firstName.trim();

        Map<String, String> mergeInfo = new HashMap<>();
        mergeInfo.put("first_name", safeName);
        mergeInfo.put("reset_link", resetLink);

        sendEmailAsync(toEmail, mergeInfo, resetTemplateKey);
    }

    /* ================= PAYMENT SUCCESS ================= */

    public void sendPaymentSuccessEmail(String toEmail,
                                        String firstName,
                                        String planName,
                                        String transactionId,
                                        String amount,
                                        String paymentDate,
                                        String dashboardLink) {

        String safeName = (firstName == null || firstName.isBlank())
                ? "User"
                : firstName.trim();

        Map<String, String> mergeInfo = new HashMap<>();
        mergeInfo.put("first_name",      safeName);
        mergeInfo.put("plan_name",       planName);
        mergeInfo.put("transaction_id",  transactionId);
        mergeInfo.put("amount",          amount);
        mergeInfo.put("payment_date",    paymentDate);
        mergeInfo.put("dashboard_link",  dashboardLink);

        sendEmailAsync(toEmail, mergeInfo, paymentTemplateKey);
    }

    /* ================= ASYNC ================= */

    @Async
    public void sendEmailAsync(String toEmail,
                               Map<String, String> mergeInfo,
                               String templateKey) {

        sendEmailWithRetry(toEmail, mergeInfo, templateKey, 0);
    }

    /* ================= RETRY ================= */

    private void sendEmailWithRetry(String toEmail,
                                    Map<String, String> mergeInfo,
                                    String templateKey,
                                    int attempt) {

        try {
            sendEmail(toEmail, mergeInfo, templateKey);
            System.out.println("✅ Email sent successfully to: " + toEmail);
            return;

        } catch (HttpClientErrorException e) {
            System.err.println("❌ Client Error: " + e.getStatusCode());
            throw new RuntimeException("Email failed: " + e.getResponseBodyAsString());

        } catch (Exception e) {
            if (attempt < MAX_RETRIES) {
                try { Thread.sleep(RETRY_DELAY); } catch (InterruptedException ignored) {}
                sendEmailWithRetry(toEmail, mergeInfo, templateKey, attempt + 1);
            } else {
                throw new RuntimeException("Failed after retries: " + e.getMessage());
            }
        }
    }

    /* ================= MAIN SEND ================= */

    private void sendEmail(String toEmail,
                           Map<String, String> mergeInfo,
                           String templateKey) throws Exception {

        Map<String, Object> payload = buildPayload(toEmail, mergeInfo, templateKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Zoho-enczapikey " + apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        restTemplate.postForObject(apiUrl, request, String.class);
    }

    /* ================= PAYLOAD BUILDER ================= */

    private Map<String, Object> buildPayload(String toEmail,
                                             Map<String, String> mergeInfo,
                                             String templateKey) {

        Map<String, Object> payload = new HashMap<>();

        // FROM
        Map<String, String> from = new HashMap<>();
        from.put("address", fromEmail);
        from.put("name",    fromName);

        // TO
        Map<String, Object> toObj = new HashMap<>();
        Map<String, String> emailAddress = new HashMap<>();
        emailAddress.put("address", toEmail);
        toObj.put("email_address", emailAddress);

        payload.put("from",         from);
        payload.put("to",           new Object[]{toObj});
        payload.put("template_key", templateKey);
        payload.put("merge_info",   mergeInfo);

        return payload;
    }
}