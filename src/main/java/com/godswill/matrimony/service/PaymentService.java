package com.godswill.matrimony.service;

import com.godswill.matrimony.model.SubscriptionPlan;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final EmailService emailService;

    @Value("${razorpay.api.key}")
    private String apiKey;

    @Value("${razorpay.api.secret}")
    private String apiSecret;

    /* =====================================================
       CREATE ORDER
    ===================================================== */

    public Map<String, String> createOrder(SubscriptionPlan plan, String userId)
            throws RazorpayException {

        RazorpayClient razorpayClient = new RazorpayClient(apiKey, apiSecret);

        String receiptId = generateShortReceiptId(plan);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", plan.getPrice() * 100); // in paise
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", receiptId);

        JSONObject notes = new JSONObject();
        notes.put("userId", userId);
        notes.put("plan", plan.name());
        notes.put("validity", plan.getDisplayDuration());

        orderRequest.put("notes", notes);

        Order order = razorpayClient.orders.create(orderRequest);

        Map<String, String> response = new HashMap<>();
        response.put("orderId", order.get("id"));
        response.put("amount", order.get("amount").toString());
        response.put("currency", order.get("currency"));
        response.put("keyId", apiKey);

        return response;
    }

    /* =====================================================
       VERIFY & PROCESS PAYMENT
    ===================================================== */

    public boolean verifyAndProcessPayment(String orderId,
                                           String paymentId,
                                           String signature,
                                           String userEmail,
                                           String firstName,
                                           SubscriptionPlan plan) {

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            boolean isValid = Utils.verifyPaymentSignature(options, apiSecret);

            if (isValid) {

                // 🔥 SEND PAYMENT SUCCESS EMAIL
                emailService.sendPaymentSuccessEmail(
                        userEmail,
                        firstName,
                        plan.name(),
                        paymentId,
                        String.valueOf(plan.getPrice()),
                        LocalDate.now().toString(),
                        "http://localhost:8080/dashboard"
                );

                System.out.println("✅ Payment verified & success email sent");

                return true;
            }

            System.out.println("❌ Invalid payment signature");
            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* =====================================================
       HELPER - SHORT RECEIPT
    ===================================================== */

    private String generateShortReceiptId(SubscriptionPlan plan) {
        long timestamp = System.currentTimeMillis();
        String receiptId = "SUB_" + plan.name() + "_" + timestamp;

        if (receiptId.length() > 40) {
            receiptId = receiptId.substring(0, 40);
        }

        return receiptId;
    }

    /* =====================================================
       OPTIONAL - FETCH ORDER
    ===================================================== */

    public JSONObject getOrderDetails(String orderId) throws RazorpayException {
        RazorpayClient razorpayClient = new RazorpayClient(apiKey, apiSecret);
        Order order = razorpayClient.orders.fetch(orderId);
        return new JSONObject(order.toString());
    }
}