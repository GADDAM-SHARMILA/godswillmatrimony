//package com.godswill.matrimony;
//
//import com.godswill.matrimony.controller.OrderController;
//import com.godswill.matrimony.model.Order;
//import com.godswill.matrimony.model.User;
//import com.godswill.matrimony.service.OrderService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.mock.web.MockHttpSession;
//import org.springframework.ui.Model;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//class OrderControllerTest {
//
//    @InjectMocks
//    private OrderController controller;
//
//    @Mock private OrderService orderService;
//    @Mock private Model model;
//
//    private MockHttpSession loggedInSession;
//    private MockHttpSession guestSession;
//    private User testUser;
//    private Order mockOrder;
//
//    @BeforeEach
//    void setUp() throws Exception {
//        MockitoAnnotations.openMocks(this);
//
//        testUser = new User();
//        testUser.setId("user-001");
//
//        loggedInSession = new MockHttpSession();
//        loggedInSession.setAttribute("user", testUser);
//
//        guestSession = new MockHttpSession();
//
//        mockOrder = new Order();
//        mockOrder.setId("order-001");
//        mockOrder.setRazorpayOrderId("rzp-001");
//        mockOrder.setFinalAmount(500.0);
//        mockOrder.setTotalAmount(550.0);
//        mockOrder.setDiscountAmount(50.0);
//        mockOrder.setStatus(Order.OrderStatus.PENDING);
//        mockOrder.setPaymentStatus(Order.PaymentStatus.PENDING);
//
//        Order.ShippingAddress addr = new Order.ShippingAddress();
//        addr.setFullName("Test User");
//        addr.setPhone("9999999999");
//        addr.setEmail("test@example.com");
//        mockOrder.setShippingAddress(addr);
//
//        // inject razorpay key via reflection
//        var field = OrderController.class.getDeclaredField("razorpayKeyId");
//        field.setAccessible(true);
//        field.set(controller, "rzp_test_key");
//    }
//
//    // ── adminOrdersPage ───────────────────────────────────────────────────────
//
//    @Test
//    void adminOrdersPage_returnsCorrectView() {
//        String view = controller.adminOrdersPage(model);
//
//        assertThat(view).isEqualTo("admin-orders");
//        verify(model).addAttribute("activePage", "admin");
//    }
//
//    // ── placeOrder ────────────────────────────────────────────────────────────
//
//    @Test
//    void placeOrder_loggedInUser_returnsOrderDetails() throws Exception {
//        when(orderService.placeOrder(eq("user-001"), isNull(), any(), isNull(), eq("BRIDE")))
//                .thenReturn(mockOrder);
//
//        Map<String, Object> request = buildPlaceOrderRequest("BRIDE", null);
//
//        ResponseEntity<?> response = controller.placeOrder(request, loggedInSession);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        @SuppressWarnings("unchecked")
//        Map<String, Object> body = (Map<String, Object>) response.getBody();
//        assertThat(body.get("orderId")).isEqualTo("order-001");
//        assertThat(body.get("razorpayOrderId")).isEqualTo("rzp-001");
//        assertThat(body.get("keyId")).isEqualTo("rzp_test_key");
//    }
//
//    @Test
//    void placeOrder_guestUser_withGuestId_returnsOrderDetails() throws Exception {
//        when(orderService.placeOrder(isNull(), eq("guest-abc"), any(), isNull(), eq("BRIDE")))
//                .thenReturn(mockOrder);
//
//        Map<String, Object> request = buildPlaceOrderRequest("BRIDE", "guest-abc");
//
//        ResponseEntity<?> response = controller.placeOrder(request, guestSession);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//    }
//
//    @Test
//    void placeOrder_noUserNoGuestId_returnsBadRequest() {
//        Map<String, Object> request = buildPlaceOrderRequest("BRIDE", null);
//
//        ResponseEntity<?> response = controller.placeOrder(request, guestSession);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//        @SuppressWarnings("unchecked")
//        Map<String, Object> body = (Map<String, Object>) response.getBody();
//        assertThat(body.get("error").toString()).contains("Session expired");
//    }
//
//    @Test
//    void placeOrder_serviceThrowsRuntimeException_returnsBadRequest() throws Exception {
//        when(orderService.placeOrder(any(), any(), any(), any(), any()))
//                .thenThrow(new RuntimeException("Cart is empty"));
//
//        Map<String, Object> request = buildPlaceOrderRequest("BRIDE", null);
//
//        ResponseEntity<?> response = controller.placeOrder(request, loggedInSession);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//        @SuppressWarnings("unchecked")
//        Map<String, Object> body = (Map<String, Object>) response.getBody();
//        assertThat(body.get("error")).isEqualTo("Cart is empty");
//    }
//
//    @Test
//    void placeOrder_containsDiscountAndFinalAmount() throws Exception {
//        when(orderService.placeOrder(any(), any(), any(), any(), any())).thenReturn(mockOrder);
//
//        Map<String, Object> request = buildPlaceOrderRequest("BRIDE", null);
//
//        ResponseEntity<?> response = controller.placeOrder(request, loggedInSession);
//
//        @SuppressWarnings("unchecked")
//        Map<String, Object> body = (Map<String, Object>) response.getBody();
//        assertThat(body.get("totalAmount")).isEqualTo(550.0);
//        assertThat(body.get("discountAmount")).isEqualTo(50.0);
//        assertThat(body.get("finalAmount")).isEqualTo(500.0);
//    }
//
//    // ── verifyPayment ─────────────────────────────────────────────────────────
//
//    @Test
//    void verifyPayment_validRequest_returnsSuccessMessage() {
//        mockOrder.setStatus(Order.OrderStatus.CONFIRMED);
//        mockOrder.setPaymentStatus(Order.PaymentStatus.PAID);
//        when(orderService.verifyPayment("rzp-001", "pay-001", "sig-001")).thenReturn(mockOrder);
//
//        Map<String, String> request = Map.of(
//                "razorpayOrderId",   "rzp-001",
//                "razorpayPaymentId", "pay-001",
//                "razorpaySignature", "sig-001"
//        );
//
//        ResponseEntity<?> response = controller.verifyPayment(request);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        @SuppressWarnings("unchecked")
//        Map<String, Object> body = (Map<String, Object>) response.getBody();
//        assertThat(body.get("message")).isEqualTo("Payment verified successfully");
//        assertThat(body.get("orderId")).isEqualTo("order-001");
//        assertThat(body.get("status")).isEqualTo("CONFIRMED");
//        assertThat(body.get("paymentStatus")).isEqualTo("PAID");
//    }
//
//    @Test
//    void verifyPayment_serviceThrows_returnsBadRequest() {
//        when(orderService.verifyPayment(any(), any(), any()))
//                .thenThrow(new RuntimeException("Invalid signature"));
//
//        Map<String, String> request = Map.of(
//                "razorpayOrderId",   "rzp-001",
//                "razorpayPaymentId", "pay-001",
//                "razorpaySignature", "bad-sig"
//        );
//
//        ResponseEntity<?> response = controller.verifyPayment(request);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//    }
//
//    // ── getMyOrders ───────────────────────────────────────────────────────────
//
//    @Test
//    void getMyOrders_loggedInUser_returnsUserOrders() {
//        when(orderService.getUserOrders("user-001")).thenReturn(List.of(mockOrder));
//
//        ResponseEntity<?> response = controller.getMyOrders(null, loggedInSession);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        verify(orderService).getUserOrders("user-001");
//    }
//
//    @Test
//    void getMyOrders_guestWithGuestId_returnsGuestOrders() {
//        when(orderService.getGuestOrders("guest-abc")).thenReturn(List.of(mockOrder));
//
//        ResponseEntity<?> response = controller.getMyOrders("guest-abc", guestSession);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        verify(orderService).getGuestOrders("guest-abc");
//    }
//
//    @Test
//    void getMyOrders_noUserNoGuestId_returnsBadRequest() {
//        ResponseEntity<?> response = controller.getMyOrders(null, guestSession);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//    }
//
//    // ── getOrder ──────────────────────────────────────────────────────────────
//
//    @Test
//    void getOrder_validId_returnsOrder() {
//        when(orderService.getOrderById("order-001")).thenReturn(mockOrder);
//
//        ResponseEntity<?> response = controller.getOrder("order-001");
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(response.getBody()).isEqualTo(mockOrder);
//    }
//
//    @Test
//    void getOrder_notFound_returns404() {
//        when(orderService.getOrderById("bad-id")).thenThrow(new RuntimeException("Not found"));
//
//        ResponseEntity<?> response = controller.getOrder("bad-id");
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
//    }
//
//    // ── cancelOrder ───────────────────────────────────────────────────────────
//
//    @Test
//    void cancelOrder_loggedInUser_returnsCancelledStatus() {
//        mockOrder.setStatus(Order.OrderStatus.CANCELLED);
//        when(orderService.cancelOrder("order-001", "user-001", null)).thenReturn(mockOrder);
//
//        ResponseEntity<?> response = controller.cancelOrder(
//                "order-001", new HashMap<>(), loggedInSession);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        @SuppressWarnings("unchecked")
//        Map<String, Object> body = (Map<String, Object>) response.getBody();
//        assertThat(body.get("status")).isEqualTo("CANCELLED");
//    }
//
//    @Test
//    void cancelOrder_serviceThrows_returnsBadRequest() {
//        when(orderService.cancelOrder(any(), any(), any()))
//                .thenThrow(new RuntimeException("Cannot cancel"));
//
//        ResponseEntity<?> response = controller.cancelOrder(
//                "order-001", new HashMap<>(), loggedInSession);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//    }
//
//    // ── updateOrderStatus (admin) ─────────────────────────────────────────────
//
//    @Test
//    void updateOrderStatus_validStatus_returnsUpdatedStatus() {
//        mockOrder.setStatus(Order.OrderStatus.SHIPPED);
//        when(orderService.updateOrderStatus("order-001", Order.OrderStatus.SHIPPED))
//                .thenReturn(mockOrder);
//
//        ResponseEntity<?> response = controller.updateOrderStatus(
//                "order-001", Map.of("status", "SHIPPED"));
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        @SuppressWarnings("unchecked")
//        Map<String, Object> body = (Map<String, Object>) response.getBody();
//        assertThat(body.get("status")).isEqualTo("SHIPPED");
//    }
//
//    @Test
//    void updateOrderStatus_invalidStatus_returnsBadRequest() {
//        ResponseEntity<?> response = controller.updateOrderStatus(
//                "order-001", Map.of("status", "INVALID_STATUS"));
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//    }
//
//    // ── getAllOrders (admin) ───────────────────────────────────────────────────
//
//    @Test
//    void getAllOrders_noFilter_returnsAllOrders() {
//        when(orderService.getAllOrders()).thenReturn(List.of(mockOrder));
//
//        ResponseEntity<?> response = controller.getAllOrders(null);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        verify(orderService).getAllOrders();
//    }
//
//    @Test
//    void getAllOrders_withValidStatus_returnsFilteredOrders() {
//        when(orderService.getOrdersByStatus(Order.OrderStatus.SHIPPED))
//                .thenReturn(List.of(mockOrder));
//
//        ResponseEntity<?> response = controller.getAllOrders("SHIPPED");
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        verify(orderService).getOrdersByStatus(Order.OrderStatus.SHIPPED);
//    }
//
//    @Test
//    void getAllOrders_withInvalidStatus_returnsBadRequest() {
//        ResponseEntity<?> response = controller.getAllOrders("GARBAGE_STATUS");
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//    }
//
//    // ── Helper ────────────────────────────────────────────────────────────────
//
//    private Map<String, Object> buildPlaceOrderRequest(String cartType, String guestId) {
//        Map<String, Object> address = new HashMap<>();
//        address.put("fullName",    "Test User");
//        address.put("phone",       "9999999999");
//        address.put("email",       "test@example.com");
//        address.put("addressLine1","123 Main St");
//        address.put("addressLine2","");
//        address.put("city",        "Hyderabad");
//        address.put("state",       "Telangana");
//        address.put("pincode",     "500001");
//
//        Map<String, Object> request = new HashMap<>();
//        request.put("cartType",       cartType);
//        request.put("shippingAddress", address);
//        if (guestId != null) request.put("guestId", guestId);
//        return request;
//    }
//}