package com.godswill.matrimony.service;

import com.godswill.matrimony.model.Subscription;
import com.godswill.matrimony.model.SubscriptionPlan;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.repository.SubscriptionRepository;
import com.godswill.matrimony.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    /* =====================================================
       CREATE PENDING SUBSCRIPTION
    ===================================================== */

    public Subscription createSubscription(String userId,
                                           SubscriptionPlan plan,
                                           String razorpayOrderId) {

        Subscription subscription = new Subscription();
        subscription.setUserId(userId);
        subscription.setPlan(plan);
        subscription.setAmount(plan.getPrice());
        subscription.setRazorpayOrderId(razorpayOrderId);
        subscription.setPaymentStatus("PENDING");
        subscription.setActive(false);
        subscription.onCreate();

        return subscriptionRepository.save(subscription);
    }

    /* =====================================================
       ACTIVATE SUBSCRIPTION (AFTER PAYMENT SUCCESS)
    ===================================================== */

    @Transactional
    public Subscription activateSubscription(String razorpayOrderId,
                                             String paymentId,
                                             String signature) {

        Subscription subscription = subscriptionRepository
                .findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() ->
                        new RuntimeException("Subscription not found for order: " + razorpayOrderId)
                );

        // 🚨 Prevent duplicate activation
        if ("SUCCESS".equals(subscription.getPaymentStatus())) {
            return subscription;
        }

        // Deactivate existing active subscriptions
        deactivateUserSubscriptions(subscription.getUserId());

        // Activate current subscription
        subscription.setRazorpayPaymentId(paymentId);
        subscription.setRazorpaySignature(signature);
        subscription.setPaymentStatus("SUCCESS");
        subscription.setActive(true);

        LocalDateTime now = LocalDateTime.now();
        subscription.setStartDate(now);

        int months = subscription.getPlan().getDurationMonths();
        subscription.setEndDate(now.plusMonths(months));

        subscription.onUpdate();
        subscriptionRepository.save(subscription);

        updateUserSubscription(subscription);

        return subscription;
    }

    /* =====================================================
       DEACTIVATE OTHER SUBSCRIPTIONS
    ===================================================== */

    private void deactivateUserSubscriptions(String userId) {

        List<Subscription> subscriptions =
                subscriptionRepository.findByUserId(userId);

        for (Subscription sub : subscriptions) {
            if (Boolean.TRUE.equals(sub.getActive())) {
                sub.setActive(false);
                sub.onUpdate();
                subscriptionRepository.save(sub);
            }
        }
    }

    /* =====================================================
       UPDATE USER RECORD
    ===================================================== */

    private void updateUserSubscription(Subscription subscription) {

        userRepository.findById(subscription.getUserId())
                .ifPresent(user -> {
                    user.setActiveSubscriptionId(subscription.getId());
                    user.setCurrentPlan(subscription.getPlan());
                    user.setSubscriptionExpiryDate(subscription.getEndDate());
                    user.onUpdate();
                    userRepository.save(user);
                });
    }

    /* =====================================================
       GET ACTIVE SUBSCRIPTION
    ===================================================== */

    public Optional<Subscription> getActiveSubscription(String userId) {
        return subscriptionRepository.findByUserIdAndActiveTrue(userId);
    }

    /* =====================================================
       GET ALL SUBSCRIPTIONS (HISTORY)
    ===================================================== */

    public List<Subscription> getUserSubscriptions(String userId) {
        return subscriptionRepository.findByUserId(userId);
    }

    /* =====================================================
       GET BY ORDER ID (NEEDED FOR CONTROLLER)
    ===================================================== */

    public Optional<Subscription> getSubscriptionByOrderId(String orderId) {
        return subscriptionRepository.findByRazorpayOrderId(orderId);
    }

    /* =====================================================
       GET BY SUBSCRIPTION ID (FOR RECEIPT DOWNLOAD)
    ===================================================== */

    public Optional<Subscription> getSubscriptionById(String subscriptionId) {
        return subscriptionRepository.findById(subscriptionId);
    }

    /* =====================================================
       CHECK ACTIVE PREMIUM
    ===================================================== */

    public boolean hasActivePremium(String userId) {

        Optional<Subscription> subOpt = getActiveSubscription(userId);

        if (subOpt.isEmpty()) return false;

        Subscription sub = subOpt.get();

        return sub.getActive() && !sub.isExpired();
    }

    /* =====================================================
       MARK PAYMENT FAILED
    ===================================================== */

    public void markPaymentFailed(String razorpayOrderId, String reason) {

        subscriptionRepository.findByRazorpayOrderId(razorpayOrderId)
                .ifPresent(sub -> {
                    sub.setPaymentStatus("FAILED");
                    sub.setActive(false);
                    sub.setFailureReason(reason);
                    sub.onUpdate();
                    subscriptionRepository.save(sub);
                });
    }
}