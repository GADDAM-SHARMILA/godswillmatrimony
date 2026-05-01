package com.godswill.matrimony;

import com.godswill.matrimony.controller.CartController;
import com.godswill.matrimony.model.Cart;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.service.CartService;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CartControllerTest {

    @InjectMocks
    private CartController controller;

    @Mock private CartService cartService;

    private MockHttpSession loggedInSession;
    private MockHttpSession guestSession;
    private User testUser;
    private Cart mockCart;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId("user-001");

        loggedInSession = new MockHttpSession();
        loggedInSession.setAttribute("user", testUser);

        guestSession = new MockHttpSession();

        mockCart = new Cart();
        mockCart.setCartType("BRIDE");
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    // ── getCart ───────────────────────────────────────────────────────────────

    @Test
    void getCart_loggedInUser_returnsCart() {
        when(cartService.getCart("user-001", null, "BRIDE")).thenReturn(mockCart);

        ResponseEntity<Cart> response = controller.getCart("BRIDE", null, loggedInSession);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockCart);
    }

    @Test
    void getCart_guestUser_returnsCartByGuestId() {
        when(cartService.getCart(null, "guest-abc", "BRIDE")).thenReturn(mockCart);

        ResponseEntity<Cart> response = controller.getCart("BRIDE", "guest-abc", guestSession);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockCart);
    }

    @Test
    void getCart_groomCartType_passesCorrectType() {
        Cart groomCart = new Cart();
        groomCart.setCartType("GROOM");
        when(cartService.getCart("user-001", null, "GROOM")).thenReturn(groomCart);

        ResponseEntity<Cart> response = controller.getCart("GROOM", null, loggedInSession);

        assertThat(response.getBody().getCartType()).isEqualTo("GROOM");
    }

    // ── addToCart ─────────────────────────────────────────────────────────────

    @Test
    void addToCart_loggedInUser_returnsSuccessResponse() {
        Cart.CartItem item = new Cart.CartItem();
        item.setPrice(250);
        item.setQuantity(2);

        mockCart.setItems(List.of(item));

        when(cartService.addToCart(eq("user-001"), isNull(), eq("prod-1"),
                eq(2), isNull(), isNull()))
                .thenReturn(mockCart);

        Map<String, Object> request = new HashMap<>();
        request.put("productId", "prod-1");
        request.put("quantity", "2");

        ResponseEntity<?> response = controller.addToCart(request, loggedInSession);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();

        assertThat(body).containsKey("message");
        assertThat(body.get("loggedIn")).isEqualTo(true);
    }

    @Test
    void addToCart_guestUser_loggedInIsFalse() {
        Cart.CartItem item = new Cart.CartItem();
        item.setPrice(500);
        item.setQuantity(1);

        mockCart.setItems(List.of(item));

        when(cartService.addToCart(isNull(), eq("guest-abc"), eq("prod-1"),
                eq(1), isNull(), isNull()))
                .thenReturn(mockCart);

        Map<String, Object> request = new HashMap<>();
        request.put("productId", "prod-1");
        request.put("quantity", "1");
        request.put("guestId", "guest-abc");

        ResponseEntity<?> response = controller.addToCart(request, guestSession);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();

        assertThat(body.get("loggedIn")).isEqualTo(false);
    }

    @Test
    void updateQuantity_validRequest_returnsUpdatedCart() {
        Cart.CartItem item = new Cart.CartItem();
        item.setPrice(500);
        item.setQuantity(3);

        mockCart.setItems(List.of(item));

        when(cartService.updateQuantity("user-001", null, "BRIDE", "prod-1", null, null, 3))
                .thenReturn(mockCart);

        Map<String, Object> request = new HashMap<>();
        request.put("productId", "prod-1");
        request.put("cartType", "BRIDE");
        request.put("quantity", "3");

        ResponseEntity<?> response = controller.updateQuantity(request, loggedInSession);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();

        assertThat(body.get("totalItems")).isEqualTo(3);
    }

    @Test
    void clearCart_loggedInUser_clearsAndReturnsOk() {
        ResponseEntity<?> response = controller.clearCart("BRIDE", loggedInSession);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(cartService).clearCart("user-001", "BRIDE");
    }
}