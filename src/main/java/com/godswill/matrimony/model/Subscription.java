package com.godswill.matrimony.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    private String id;

    @Indexed
    private String userId;

    private SubscriptionPlan plan; // BRONZE, SILVER, GOLD, PLATINUM, RUBY

    private Integer amount; // in rupees

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Boolean active = false; // 🔥 default false (safer)

    /* ================= PAYMENT DETAILS ================= */

    @Indexed(unique = true)
    private String razorpayOrderId;

    private String razorpayPaymentId;

    private String razorpaySignature;

    private String paymentStatus; // PENDING, SUCCESS, FAILED

    private String failureReason; // store failure reason if any

    private LocalDateTime paymentCompletedAt;

    /* ================= AUDIT FIELDS ================= */

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /* ================= LIFECYCLE METHODS ================= */

    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;

        if (active == null) active = false;

        if (paymentStatus == null) paymentStatus = "PENDING";
    }

    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /* ================= BUSINESS LOGIC ================= */

    public boolean isExpired() {
        if (endDate == null) return true;
        return LocalDateTime.now().isAfter(endDate);
    }

    public boolean isActiveAndValid() {
        return Boolean.TRUE.equals(active)
                && "SUCCESS".equals(paymentStatus)
                && !isExpired();
    }
}