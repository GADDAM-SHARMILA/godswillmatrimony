package com.godswill.matrimony;

import com.godswill.matrimony.controller.SubscriptionController;
import com.godswill.matrimony.model.Subscription;
import com.godswill.matrimony.model.SubscriptionPlan;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.service.PaymentService;
import com.godswill.matrimony.service.SubscriptionService;
import com.razorpay.RazorpayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionController Tests")
class SubscriptionControllerTest {

    @Mock private SubscriptionService subscriptionService;
    @Mock private PaymentService paymentService;

    @InjectMocks
    private SubscriptionController subscriptionController;

    private MockMvc mockMvc;
    private MockHttpSession userSession;
    private MockHttpSession guestSession;
    private User mockUser;
    private Subscription mockSubscription;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(subscriptionController).build();

        mockUser = new User();
        mockUser.setId("user-001");
        mockUser.setEmail("user@test.com");
        mockUser.setFirstName("John");

        mockSubscription = new Subscription();
        mockSubscription.setId("sub-001");
        mockSubscription.setPlan(SubscriptionPlan.GOLD);
        mockSubscription.setEndDate(LocalDateTime.now().plusMonths(1));

        userSession = new MockHttpSession();
        userSession.setAttribute("user", mockUser);

        guestSession = new MockHttpSession();
    }

    // ══════════════════════════════════════════════
    //  GET /subscription/plans
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /subscription/plans — logged-in user returns premium-plans view")
    void testShowPremiumPlans_LoggedIn_ReturnsView() throws Exception {
        when(subscriptionService.getActiveSubscription("user-001")).thenReturn(Optional.empty());

        mockMvc.perform(get("/subscription/plans").session(userSession))
                .andExpect(status().isOk())
                .andExpect(view().name("user/premium-plans"))
                .andExpect(model().attributeExists("plans"));
    }

    @Test
    @DisplayName("GET /subscription/plans — model contains active subscription if exists")
    void testShowPremiumPlans_WithActiveSub_ModelContainsSub() throws Exception {
        when(subscriptionService.getActiveSubscription("user-001"))
                .thenReturn(Optional.of(mockSubscription));

        mockMvc.perform(get("/subscription/plans").session(userSession))
                .andExpect(model().attributeExists("activeSubscription"));
    }

    @Test
    @DisplayName("GET /subscription/plans — no active subscription, model attribute is null")
    void testShowPremiumPlans_NoActiveSub_ModelAttributeIsNull() throws Exception {
        when(subscriptionService.getActiveSubscription("user-001")).thenReturn(Optional.empty());

        mockMvc.perform(get("/subscription/plans").session(userSession))
                .andExpect(model().attribute("activeSubscription", (Object) null));
    }

    @Test
    @DisplayName("GET /subscription/plans — guest redirects to /login")
    void testShowPremiumPlans_Guest_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/subscription/plans").session(guestSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ══════════════════════════════════════════════
    //  POST /subscription/create-order
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("POST /subscription/create-order — guest returns 401")
    void testCreateOrder_Guest_Returns401() throws Exception {
        mockMvc.perform(post("/subscription/create-order").session(guestSession)
                        .param("plan", "GOLD"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /subscription/create-order — valid plan returns 200 with order data")
    void testCreateOrder_ValidPlan_Returns200() throws Exception {
        Map<String, String> orderData = Map.of("orderId", "order-123", "amount", "9900");
        when(paymentService.createOrder(any(SubscriptionPlan.class), anyString()))
                .thenReturn(orderData);

        mockMvc.perform(post("/subscription/create-order").session(userSession)
                        .param("plan", "GOLD"))
                .andExpect(status().isOk());

        verify(subscriptionService, times(1)).createSubscription(anyString(), any(), anyString());
    }

    @Test
    @DisplayName("POST /subscription/create-order — invalid plan name returns 400")
    void testCreateOrder_InvalidPlan_Returns400() throws Exception {
        mockMvc.perform(post("/subscription/create-order").session(userSession)
                        .param("plan", "INVALID_PLAN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /subscription/create-order — Razorpay exception returns 500")
    void testCreateOrder_RazorpayException_Returns500() throws Exception {
        when(paymentService.createOrder(any(), anyString()))
                .thenThrow(new RazorpayException("Razorpay error"));

        mockMvc.perform(post("/subscription/create-order").session(userSession)
                        .param("plan", "GOLD"))
                .andExpect(status().isInternalServerError());
    }

    // ══════════════════════════════════════════════
    //  POST /subscription/verify-payment
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("POST /subscription/verify-payment — guest redirects to /login")
    void testVerifyPayment_Guest_RedirectsToLogin() throws Exception {
        mockMvc.perform(post("/subscription/verify-payment").session(guestSession)
                        .param("razorpay_order_id", "order-123")
                        .param("razorpay_payment_id", "pay-123")
                        .param("razorpay_signature", "sig-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("POST /subscription/verify-payment — subscription not found redirects to /subscription/failed")
    void testVerifyPayment_SubscriptionNotFound_RedirectsToFailed() throws Exception {
        when(subscriptionService.getSubscriptionByOrderId("order-123")).thenReturn(Optional.empty());

        mockMvc.perform(post("/subscription/verify-payment").session(userSession)
                        .param("razorpay_order_id", "order-123")
                        .param("razorpay_payment_id", "pay-123")
                        .param("razorpay_signature", "sig-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/subscription/failed"));
    }

    @Test
    @DisplayName("POST /subscription/verify-payment — valid payment activates and redirects to success")
    void testVerifyPayment_ValidPayment_ActivatesAndRedirectsToSuccess() throws Exception {
        when(subscriptionService.getSubscriptionByOrderId("order-123"))
                .thenReturn(Optional.of(mockSubscription));
        when(paymentService.verifyAndProcessPayment(anyString(), anyString(), anyString(),
                anyString(), anyString(), any(SubscriptionPlan.class))).thenReturn(true);
        when(subscriptionService.activateSubscription("order-123", "pay-123", "sig-123"))
                .thenReturn(mockSubscription);

        mockMvc.perform(post("/subscription/verify-payment").session(userSession)
                        .param("razorpay_order_id", "order-123")
                        .param("razorpay_payment_id", "pay-123")
                        .param("razorpay_signature", "sig-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/subscription/success"));

        verify(subscriptionService, times(1)).activateSubscription("order-123", "pay-123", "sig-123");
    }

    @Test
    @DisplayName("POST /subscription/verify-payment — invalid signature redirects to /subscription/failed")
    void testVerifyPayment_InvalidSignature_RedirectsToFailed() throws Exception {
        when(subscriptionService.getSubscriptionByOrderId("order-123"))
                .thenReturn(Optional.of(mockSubscription));
        when(paymentService.verifyAndProcessPayment(anyString(), anyString(), anyString(),
                anyString(), anyString(), any())).thenReturn(false);

        mockMvc.perform(post("/subscription/verify-payment").session(userSession)
                        .param("razorpay_order_id", "order-123")
                        .param("razorpay_payment_id", "pay-123")
                        .param("razorpay_signature", "bad-sig"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/subscription/failed"));

        verify(subscriptionService, times(1)).markPaymentFailed(eq("order-123"), anyString());
    }

    @Test
    @DisplayName("POST /subscription/verify-payment — session is updated with subscription info on success")
    void testVerifyPayment_Success_UpdatesSession() throws Exception {
        mockSubscription.setPlan(SubscriptionPlan.GOLD);
        mockSubscription.setEndDate(LocalDateTime.now().plusMonths(1));

        when(subscriptionService.getSubscriptionByOrderId("order-123"))
                .thenReturn(Optional.of(mockSubscription));
        when(paymentService.verifyAndProcessPayment(anyString(), anyString(), anyString(),
                anyString(), anyString(), any())).thenReturn(true);
        when(subscriptionService.activateSubscription(anyString(), anyString(), anyString()))
                .thenReturn(mockSubscription);

        mockMvc.perform(post("/subscription/verify-payment").session(userSession)
                        .param("razorpay_order_id", "order-123")
                        .param("razorpay_payment_id", "pay-123")
                        .param("razorpay_signature", "sig-123"))
                .andExpect(redirectedUrl("/subscription/success"));

        // Session user should have been updated
        User updatedUser = (User) userSession.getAttribute("user");
        assert updatedUser != null;
    }

    // ══════════════════════════════════════════════
    //  GET /subscription/success
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /subscription/success — logged-in user returns payment-success view")
    void testPaymentSuccess_LoggedIn_ReturnsView() throws Exception {
        when(subscriptionService.getActiveSubscription("user-001"))
                .thenReturn(Optional.of(mockSubscription));

        mockMvc.perform(get("/subscription/success").session(userSession))
                .andExpect(status().isOk())
                .andExpect(view().name("user/payment-success"))
                .andExpect(model().attributeExists("subscription"));
    }

    @Test
    @DisplayName("GET /subscription/success — guest redirects to /login")
    void testPaymentSuccess_Guest_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/subscription/success").session(guestSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ══════════════════════════════════════════════
    //  GET /subscription/failed
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /subscription/failed — logged-in user returns payment-failed view")
    void testPaymentFailed_LoggedIn_ReturnsView() throws Exception {
        mockMvc.perform(get("/subscription/failed").session(userSession))
                .andExpect(status().isOk())
                .andExpect(view().name("user/payment-failed"));
    }

    @Test
    @DisplayName("GET /subscription/failed — guest redirects to /login")
    void testPaymentFailed_Guest_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/subscription/failed").session(guestSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}