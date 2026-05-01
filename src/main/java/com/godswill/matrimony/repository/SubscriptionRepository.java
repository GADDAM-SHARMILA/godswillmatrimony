package com.godswill.matrimony.repository;

import com.godswill.matrimony.model.Subscription;
import com.godswill.matrimony.model.SubscriptionPlan;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends MongoRepository<Subscription, String> {

    // Find all subscriptions for a user
    List<Subscription> findByUserId(String userId);

    // Find active subscription for a user
    Optional<Subscription> findByUserIdAndActiveTrue(String userId);

    // Find by payment order ID
    Optional<Subscription> findByRazorpayOrderId(String orderId);

    // Find by payment ID
    Optional<Subscription> findByRazorpayPaymentId(String paymentId);

    // Count active subscriptions by plan
    long countByPlanAndActiveTrue(SubscriptionPlan plan);
}