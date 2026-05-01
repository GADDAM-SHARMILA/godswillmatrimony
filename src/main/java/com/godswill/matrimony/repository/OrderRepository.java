package com.godswill.matrimony.repository;

import com.godswill.matrimony.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    // Logged-in user orders
    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);

    // Guest orders
    List<Order> findByGuestIdOrderByCreatedAtDesc(String guestId);

    // Razorpay lookup (used for payment verification)
    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);

    // Filter by status (admin)
    List<Order> findByStatus(Order.OrderStatus status);

    // ✅ All orders sorted newest first (admin)
    List<Order> findAllByOrderByCreatedAtDesc();
}