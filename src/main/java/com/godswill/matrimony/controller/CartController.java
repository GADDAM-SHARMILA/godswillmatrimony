package com.godswill.matrimony.controller;

import com.godswill.matrimony.model.Cart;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    // ── Helper: get userId from session (null if guest) ───────────────────────
    private String getSessionUserId(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return (user != null) ? user.getId() : null;
    }

    /**
     * GET /api/cart?cartType=BRIDE
     * Returns cart for current user (session) or guest (guestId param).
     * cartType = BRIDE or GROOM
     */
    @GetMapping
    public ResponseEntity<Cart> getCart(
            @RequestParam(defaultValue = "BRIDE") String cartType,
            @RequestParam(required = false) String guestId,
            HttpSession session) {

        String userId = getSessionUserId(session);
        return ResponseEntity.ok(cartService.getCart(userId, guestId, cartType));
    }

    /**
     * POST /api/cart/add
     * Body: { productId, quantity, selectedSize?, selectedVariant?, guestId? }
     * cartType is derived automatically from product.category (BRIDE/GROOM)
     */
    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestBody Map<String, Object> request,
                                       HttpSession session) {
        try {
            String userId          = getSessionUserId(session);
            String guestId         = (String) request.getOrDefault("guestId", null);
            String productId       = (String) request.get("productId");
            int quantity           = Integer.parseInt(request.get("quantity").toString());
            String selectedSize    = (String) request.getOrDefault("selectedSize", null);
            String selectedVariant = (String) request.getOrDefault("selectedVariant", null);

            Cart cart = cartService.addToCart(userId, guestId, productId,
                    quantity, selectedSize, selectedVariant);

            return ResponseEntity.ok(Map.of(
                    "message",     "Item added to cart",
                    "cart",        cart,
                    "totalItems",  cart.getTotalItems(),
                    "totalAmount", cart.getTotalAmount(),
                    "cartType",    cart.getCartType(),
                    "loggedIn",    userId != null
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/cart/update
     * Body: { productId, quantity, cartType, selectedSize?, selectedVariant?, guestId? }
     */
    @PutMapping("/update")
    public ResponseEntity<?> updateQuantity(@RequestBody Map<String, Object> request,
                                            HttpSession session) {
        try {
            String userId          = getSessionUserId(session);
            String guestId         = (String) request.getOrDefault("guestId", null);
            String productId       = (String) request.get("productId");
            String cartType        = (String) request.getOrDefault("cartType", "BRIDE");
            int quantity           = Integer.parseInt(request.get("quantity").toString());
            String selectedSize    = (String) request.getOrDefault("selectedSize", null);
            String selectedVariant = (String) request.getOrDefault("selectedVariant", null);

            Cart cart = cartService.updateQuantity(userId, guestId, cartType,
                    productId, selectedSize, selectedVariant, quantity);

            return ResponseEntity.ok(Map.of(
                    "message",     "Cart updated",
                    "cart",        cart,
                    "totalItems",  cart.getTotalItems(),
                    "totalAmount", cart.getTotalAmount()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/cart/remove
     * Body: { productId, cartType, selectedSize?, selectedVariant?, guestId? }
     */
    @DeleteMapping("/remove")
    public ResponseEntity<?> removeFromCart(@RequestBody Map<String, Object> request,
                                            HttpSession session) {
        try {
            String userId          = getSessionUserId(session);
            String guestId         = (String) request.getOrDefault("guestId", null);
            String productId       = (String) request.get("productId");
            String cartType        = (String) request.getOrDefault("cartType", "BRIDE");
            String selectedSize    = (String) request.getOrDefault("selectedSize", null);
            String selectedVariant = (String) request.getOrDefault("selectedVariant", null);

            Cart cart = cartService.removeFromCart(userId, guestId, cartType,
                    productId, selectedSize, selectedVariant);

            return ResponseEntity.ok(Map.of(
                    "message",     "Item removed",
                    "cart",        cart,
                    "totalItems",  cart.getTotalItems(),
                    "totalAmount", cart.getTotalAmount()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/cart/clear?cartType=BRIDE
     * Clears one cart type for logged-in user
     */
    @DeleteMapping("/clear")
    public ResponseEntity<?> clearCart(@RequestParam String cartType,
                                       HttpSession session) {
        String userId = getSessionUserId(session);
        if (userId == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Not logged in"));
        cartService.clearCart(userId, cartType);
        return ResponseEntity.ok(Map.of("message", cartType + " cart cleared"));
    }
}