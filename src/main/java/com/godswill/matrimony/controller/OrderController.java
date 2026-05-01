package com.godswill.matrimony.controller;

import com.razorpay.RazorpayException;
import com.godswill.matrimony.model.Order;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Value("${razorpay.api.key}")
    private String razorpayKeyId;

    // ── Thymeleaf page: Admin orders ──────────────────────────────────────────

    @GetMapping("/admin/orders")
    public String adminOrdersPage(Model model) {
        model.addAttribute("activePage", "admin");
        return "admin-orders";
    }

    // ── REST API ──────────────────────────────────────────────────────────────

    /**
     * POST /api/orders/place
     * Supports both logged-in users and guests.
     */
    @PostMapping("/api/orders/place")
    @ResponseBody
    public ResponseEntity<?> placeOrder(@RequestBody Map<String, Object> request,
                                        HttpSession session) {
        try {
            User sessionUser = (User) session.getAttribute("user");
            String userId    = (sessionUser != null) ? sessionUser.getId() : null;
            String guestId   = (String) request.getOrDefault("guestId", null);

            if (userId == null && (guestId == null || guestId.isBlank())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Session expired. Please refresh and try again."));
            }

            String cartType      = (String) request.getOrDefault("cartType", "BRIDE");
            String profileNumber = (String) request.getOrDefault("profileNumber", null);

            @SuppressWarnings("unchecked")
            Map<String, String> addressMap = (Map<String, String>) request.get("shippingAddress");
            Order.ShippingAddress address  = new Order.ShippingAddress();
            address.setFullName(addressMap.get("fullName"));
            address.setPhone(addressMap.get("phone"));
            address.setEmail(addressMap.getOrDefault("email", ""));
            address.setAddressLine1(addressMap.get("addressLine1"));
            address.setAddressLine2(addressMap.getOrDefault("addressLine2", ""));
            address.setCity(addressMap.get("city"));
            address.setState(addressMap.get("state"));
            address.setPincode(addressMap.get("pincode"));

            Order order = orderService.placeOrder(userId, guestId, address, profileNumber, cartType);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId",        order.getId());
            response.put("razorpayOrderId", order.getRazorpayOrderId());
            response.put("amount",         (int)(order.getFinalAmount() * 100));
            response.put("currency",       "INR");
            response.put("keyId",          razorpayKeyId);
            response.put("customerName",   order.getShippingAddress().getFullName());
            response.put("customerPhone",  order.getShippingAddress().getPhone());
            response.put("customerEmail",  order.getShippingAddress().getEmail() != null
                    ? order.getShippingAddress().getEmail() : "");
            response.put("totalAmount",    order.getTotalAmount());
            response.put("discountAmount", order.getDiscountAmount());
            response.put("finalAmount",    order.getFinalAmount());

            return ResponseEntity.ok(response);

        } catch (RazorpayException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Razorpay error: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/orders/verify-payment
     */
    @PostMapping("/api/orders/verify-payment")
    @ResponseBody
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> request) {
        try {
            Order order = orderService.verifyPayment(
                    request.get("razorpayOrderId"),
                    request.get("razorpayPaymentId"),
                    request.get("razorpaySignature")
            );
            return ResponseEntity.ok(Map.of(
                    "message",       "Payment verified successfully",
                    "orderId",       order.getId(),
                    "status",        order.getStatus().name(),
                    "paymentStatus", order.getPaymentStatus().name()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/orders/my
     * Returns orders for the current logged-in user or guest.
     */
    @GetMapping("/api/orders/my")
    @ResponseBody
    public ResponseEntity<?> getMyOrders(
            @RequestParam(required = false) String guestId,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            return ResponseEntity.ok(orderService.getUserOrders(user.getId()));
        } else if (guestId != null && !guestId.isBlank()) {
            return ResponseEntity.ok(orderService.getGuestOrders(guestId));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Not authenticated"));
    }

    /**
     * GET /api/orders/{orderId}
     */
    @GetMapping("/api/orders/{orderId}")
    @ResponseBody
    public ResponseEntity<?> getOrder(@PathVariable String orderId) {
        try { return ResponseEntity.ok(orderService.getOrderById(orderId)); }
        catch (RuntimeException e) { return ResponseEntity.notFound().build(); }
    }

    /**
     * POST /api/orders/{orderId}/cancel
     */
    @PostMapping("/api/orders/{orderId}/cancel")
    @ResponseBody
    public ResponseEntity<?> cancelOrder(@PathVariable String orderId,
                                         @RequestBody Map<String, String> request,
                                         HttpSession session) {
        try {
            User user    = (User) session.getAttribute("user");
            String userId  = (user != null) ? user.getId() : null;
            String guestId = request.getOrDefault("guestId", null);
            Order order  = orderService.cancelOrder(orderId, userId, guestId);
            return ResponseEntity.ok(Map.of(
                    "message", "Order cancelled",
                    "status",  order.getStatus().name()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Admin REST ────────────────────────────────────────────────────────────

    /**
     * PUT /api/orders/admin/{orderId}/status
     * Body: { status: "SHIPPED" }
     */
    @PutMapping("/api/orders/admin/{orderId}/status")
    @ResponseBody
    public ResponseEntity<?> updateOrderStatus(@PathVariable String orderId,
                                               @RequestBody Map<String, String> request) {
        try {
            Order.OrderStatus status = Order.OrderStatus.valueOf(request.get("status").toUpperCase());
            Order order = orderService.updateOrderStatus(orderId, status);
            return ResponseEntity.ok(Map.of(
                    "message", "Status updated",
                    "status",  order.getStatus().name()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/orders/admin/all?status=SHIPPED (optional filter)
     * Returns ALL orders across all users for admin.
     */
    @GetMapping("/api/orders/admin/all")
    @ResponseBody
    public ResponseEntity<?> getAllOrders(@RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            try {
                Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
                return ResponseEntity.ok(orderService.getOrdersByStatus(orderStatus));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + status));
            }
        }
        // ✅ FIX: was calling getUserOrders("all") which looks up a userId = "all"
        // Now correctly calls getAllOrders()
        return ResponseEntity.ok(orderService.getAllOrders());
    }
}