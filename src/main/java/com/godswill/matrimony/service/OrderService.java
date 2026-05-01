package com.godswill.matrimony.service;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.godswill.matrimony.model.Cart;
import com.godswill.matrimony.model.Order;
import com.godswill.matrimony.model.Profile;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.repository.CartRepository;
import com.godswill.matrimony.repository.OrderRepository;
import com.godswill.matrimony.repository.ProfileRepository;
import com.godswill.matrimony.repository.UserRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired private OrderRepository   orderRepository;
    @Autowired private CartRepository    cartRepository;
    @Autowired private ProductService    productService;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private UserRepository    userRepository;

    @Value("${razorpay.api.key}")
    private String razorpayKeyId;

    @Value("${razorpay.api.secret}")
    private String razorpayKeySecret;

    private static final double PREMIUM_DISCOUNT_PERCENT = 10.0;

    // ── Place order ───────────────────────────────────────────────────────────

    public Order placeOrder(String userId,
                            String guestId,
                            Order.ShippingAddress address,
                            String profileNumber,
                            String cartType) throws RazorpayException {

        String type = (cartType != null && !cartType.isBlank()) ? cartType.toUpperCase() : "BRIDE";

        // Fetch the correct cart
        Cart cart;
        if (userId != null && !userId.isBlank()) {
            cart = cartRepository.findByUserIdAndCartType(userId, type)
                    .orElseThrow(() -> new RuntimeException("Cart is empty"));
        } else if (guestId != null && !guestId.isBlank()) {
            cart = cartRepository.findByGuestIdAndCartType(guestId, type)
                    .orElseThrow(() -> new RuntimeException("Cart is empty"));
        } else {
            throw new RuntimeException("No user or guest session found");
        }

        if (cart.getItems().isEmpty())
            throw new RuntimeException("Cart is empty. Add items before placing an order.");

        // Validate stock
        for (Cart.CartItem item : cart.getItems()) {
            if (!productService.isInStock(item.getProductId(), item.getQuantity()))
                throw new RuntimeException("Insufficient stock for: " + item.getProductName());
        }

        double totalAmount    = cart.getTotalAmount();
        double discountAmount = 0;
        double finalAmount    = totalAmount;

        // Discount logic
        if (userId != null) {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent() && userOpt.get().isPremium()) {
                discountAmount = Math.round((totalAmount * PREMIUM_DISCOUNT_PERCENT / 100.0) * 100.0) / 100.0;
                finalAmount    = Math.round((totalAmount - discountAmount) * 100.0) / 100.0;
            } else if (profileNumber != null && !profileNumber.trim().isEmpty()) {
                DiscountResult dr = calculateDiscountByProfile(profileNumber.trim().toUpperCase(), totalAmount);
                discountAmount = dr.discountAmount;
                finalAmount    = dr.finalAmount;
            }
        } else if (profileNumber != null && !profileNumber.trim().isEmpty()) {
            DiscountResult dr = calculateDiscountByProfile(profileNumber.trim().toUpperCase(), totalAmount);
            discountAmount = dr.discountAmount;
            finalAmount    = dr.finalAmount;
        }

        // Create Razorpay order
        RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount",          (int)(finalAmount * 100));
        orderRequest.put("currency",        "INR");
        orderRequest.put("receipt",         "order_" + System.currentTimeMillis());
        orderRequest.put("payment_capture", 1);

        com.razorpay.Order razorpayOrder = razorpay.orders.create(orderRequest);

        // Save order
        Order order = new Order();
        order.setUserId(userId);
        order.setGuestId(guestId);
        order.setItems(cart.getItems());
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setFinalAmount(finalAmount);
        order.setRazorpayOrderId(razorpayOrder.get("id"));
        order.setShippingAddress(address);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentStatus(Order.PaymentStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    // ── Discount via profile number ───────────────────────────────────────────

    private DiscountResult calculateDiscountByProfile(String profileNumber, double totalAmount) {
        Optional<Profile> profileOpt = profileRepository.findByProfileNumber(profileNumber);
        if (profileOpt.isEmpty()) return new DiscountResult(0, totalAmount);

        Profile profile = profileOpt.get();
        if (profile.getUserId() == null) return new DiscountResult(0, totalAmount);

        Optional<User> userOpt = userRepository.findById(profile.getUserId());
        if (userOpt.isEmpty() || !userOpt.get().isPremium()) return new DiscountResult(0, totalAmount);

        double discountAmount = Math.round((totalAmount * PREMIUM_DISCOUNT_PERCENT / 100.0) * 100.0) / 100.0;
        double finalAmount    = Math.round((totalAmount - discountAmount) * 100.0) / 100.0;
        return new DiscountResult(discountAmount, finalAmount);
    }

    private static class DiscountResult {
        double discountAmount, finalAmount;
        DiscountResult(double d, double f) { this.discountAmount = d; this.finalAmount = f; }
    }

    // ── Verify Razorpay payment ───────────────────────────────────────────────

    public Order verifyPayment(String razorpayOrderId,
                               String razorpayPaymentId,
                               String razorpaySignature) {

        Order order = orderRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + razorpayOrderId));

        if (verifySignature(razorpayOrderId, razorpayPaymentId, razorpaySignature)) {
            order.setRazorpayPaymentId(razorpayPaymentId);
            order.setRazorpaySignature(razorpaySignature);
            order.setPaymentStatus(Order.PaymentStatus.PAID);
            order.setStatus(Order.OrderStatus.CONFIRMED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            // Reduce stock
            for (Cart.CartItem item : order.getItems())
                productService.reduceStock(item.getProductId(), item.getQuantity());

            // Clear the ordered cart
            if (!order.getItems().isEmpty()) {
                String cartType = order.getItems().get(0).getCategory();
                if (order.getUserId() != null) {
                    cartRepository.deleteByUserIdAndCartType(order.getUserId(), cartType);
                } else if (order.getGuestId() != null) {
                    cartRepository.deleteByGuestId(order.getGuestId());
                }
            }

        } else {
            order.setPaymentStatus(Order.PaymentStatus.FAILED);
            order.setStatus(Order.OrderStatus.CANCELLED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            throw new RuntimeException("Payment verification failed. Invalid signature.");
        }

        return order;
    }

    // ── Order history ─────────────────────────────────────────────────────────

    public List<Order> getUserOrders(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Order> getGuestOrders(String guestId) {
        return orderRepository.findByGuestIdOrderByCreatedAtDesc(guestId);
    }

    // ✅ NEW: Returns ALL orders across all users — for admin
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    public Order getOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }

    public Order updateOrderStatus(String orderId, Order.OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    public Order cancelOrder(String orderId, String userId, String guestId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        boolean isOwner = (userId != null && userId.equals(order.getUserId()))
                || (guestId != null && guestId.equals(order.getGuestId()));

        if (!isOwner)
            throw new RuntimeException("Unauthorized to cancel this order");

        if (order.getStatus() == Order.OrderStatus.SHIPPED ||
                order.getStatus() == Order.OrderStatus.DELIVERED)
            throw new RuntimeException("Cannot cancel a shipped/delivered order");

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());

        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            for (Cart.CartItem item : order.getItems())
                productService.restoreStock(item.getProductId(), item.getQuantity());
            order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
        }

        return orderRepository.save(order);
    }

    // ── Razorpay signature verification ──────────────────────────────────────

    private boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        try {
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).equals(razorpaySignature);
        } catch (Exception e) {
            throw new RuntimeException("Signature verification error: " + e.getMessage());
        }
    }
}