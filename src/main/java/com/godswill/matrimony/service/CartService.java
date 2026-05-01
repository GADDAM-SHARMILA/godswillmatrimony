package com.godswill.matrimony.service;

import com.godswill.matrimony.model.Cart;
import com.godswill.matrimony.model.Cart.CartItem;
import com.godswill.matrimony.model.Product;
import com.godswill.matrimony.repository.CartRepository;
import com.godswill.matrimony.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    // ── Resolve cart: prefer userId (logged-in), fallback to guestId ──────────

    /**
     * Get cart for a user or guest by cartType (BRIDE / GROOM).
     * userId takes priority — if provided, always use it.
     * guestId used only when userId is null (not logged in).
     */
    public Cart getCart(String userId, String guestId, String cartType) {
        String type = resolveCartType(cartType);

        if (userId != null && !userId.isBlank()) {
            return cartRepository.findByUserIdAndCartType(userId, type)
                    .orElseGet(() -> {
                        Cart empty = new Cart();
                        empty.setUserId(userId);
                        empty.setCartType(type);
                        return empty;   // don't save empty carts
                    });
        }

        // Guest
        if (guestId != null && !guestId.isBlank()) {
            return cartRepository.findByGuestIdAndCartType(guestId, type)
                    .orElseGet(() -> {
                        Cart empty = new Cart();
                        empty.setGuestId(guestId);
                        empty.setCartType(type);
                        return empty;
                    });
        }

        return new Cart();  // totally anonymous — return empty
    }

    // ── Add to cart ───────────────────────────────────────────────────────────

    public Cart addToCart(String userId, String guestId, String productId,
                          int quantity, String selectedSize, String selectedVariant) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        if (!product.isActive())
            throw new RuntimeException("Product is not available: " + product.getName());

        if (product.getStock() < quantity)
            throw new RuntimeException("Only " + product.getStock() + " items in stock for: " + product.getName());

        // ✅ FIX: Always normalize cartType to UPPERCASE
        // product.getCategory() returns "bride"/"groom" (lowercase) — must uppercase it
        String cartType = product.getCategory() != null
                ? product.getCategory().toUpperCase()
                : "BRIDE";

        // Fetch or create cart
        Cart cart = fetchOrCreateCart(userId, guestId, cartType);

        // Check if same item already in cart
        Optional<CartItem> existing = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId)
                        && safeEquals(item.getSelectedSize(), selectedSize)
                        && safeEquals(item.getSelectedVariant(), selectedVariant))
                .findFirst();

        if (existing.isPresent()) {
            CartItem item = existing.get();
            int newQty    = item.getQuantity() + quantity;
            if (newQty > product.getStock())
                throw new RuntimeException("Cannot exceed available stock (" + product.getStock() + ")");
            item.setQuantity(newQty);
        } else {
            CartItem newItem = new CartItem();
            newItem.setProductId(productId);
            newItem.setProductName(product.getName());
            newItem.setImageUrl(product.getImageUrl());
            newItem.setPrice(product.getPrice());
            newItem.setQuantity(quantity);
            newItem.setSelectedSize(selectedSize);
            newItem.setSelectedVariant(selectedVariant);
            // ✅ FIX: store category as UPPERCASE too so it's consistent
            newItem.setCategory(cartType);
            cart.getItems().add(newItem);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    // ── Update quantity ───────────────────────────────────────────────────────

    public Cart updateQuantity(String userId, String guestId, String cartType,
                               String productId, String selectedSize,
                               String selectedVariant, int quantity) {

        String type = resolveCartType(cartType);
        Cart cart   = resolveCart(userId, guestId, type);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        if (quantity > product.getStock())
            throw new RuntimeException("Only " + product.getStock() + " items available");

        cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId)
                        && safeEquals(item.getSelectedSize(), selectedSize)
                        && safeEquals(item.getSelectedVariant(), selectedVariant))
                .findFirst()
                .ifPresent(item -> {
                    if (quantity <= 0) cart.getItems().remove(item);
                    else item.setQuantity(quantity);
                });

        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    // ── Remove item ───────────────────────────────────────────────────────────

    public Cart removeFromCart(String userId, String guestId, String cartType,
                               String productId, String selectedSize, String selectedVariant) {

        String type = resolveCartType(cartType);
        Cart cart   = resolveCart(userId, guestId, type);

        cart.getItems().removeIf(item ->
                item.getProductId().equals(productId)
                        && safeEquals(item.getSelectedSize(), selectedSize)
                        && safeEquals(item.getSelectedVariant(), selectedVariant));

        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    // ── Clear cart ────────────────────────────────────────────────────────────

    public void clearCart(String userId, String cartType) {
        cartRepository.deleteByUserIdAndCartType(userId, cartType);
    }

    public void clearAllUserCarts(String userId) {
        cartRepository.deleteByUserId(userId);
    }

    // ── On LOGOUT: delete all carts for logged-in user ────────────────────────

    public void clearCartsOnLogout(String userId) {
        if (userId != null && !userId.isBlank()) {
            cartRepository.deleteByUserId(userId);
        }
    }

    // ── On LOGIN: migrate guest cart → user cart ──────────────────────────────

    public void migrateGuestCartToUser(String guestId, String userId) {
        if (guestId == null || guestId.isBlank() || userId == null || userId.isBlank()) return;

        List<Cart> guestCarts = cartRepository.findByGuestIdAndUserIdIsNull(guestId);

        for (Cart guestCart : guestCarts) {
            String cartType = guestCart.getCartType();

            Optional<Cart> userCartOpt = cartRepository.findByUserIdAndCartType(userId, cartType);

            if (userCartOpt.isPresent()) {
                Cart userCart = userCartOpt.get();
                for (CartItem guestItem : guestCart.getItems()) {
                    Optional<CartItem> existing = userCart.getItems().stream()
                            .filter(i -> i.getProductId().equals(guestItem.getProductId())
                                    && safeEquals(i.getSelectedSize(), guestItem.getSelectedSize())
                                    && safeEquals(i.getSelectedVariant(), guestItem.getSelectedVariant()))
                            .findFirst();

                    if (existing.isPresent()) {
                        existing.get().setQuantity(existing.get().getQuantity() + guestItem.getQuantity());
                    } else {
                        userCart.getItems().add(guestItem);
                    }
                }
                userCart.setUpdatedAt(LocalDateTime.now());
                cartRepository.save(userCart);
                cartRepository.delete(guestCart);
            } else {
                guestCart.setUserId(userId);
                guestCart.setGuestId(null);
                guestCart.setUpdatedAt(LocalDateTime.now());
                cartRepository.save(guestCart);
            }
        }

        cartRepository.deleteByGuestId(guestId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Cart fetchOrCreateCart(String userId, String guestId, String cartType) {
        // ✅ FIX: normalize here too so DB lookups always use UPPERCASE
        String type = resolveCartType(cartType);

        if (userId != null && !userId.isBlank()) {
            return cartRepository.findByUserIdAndCartType(userId, type)
                    .orElseGet(() -> {
                        Cart c = new Cart();
                        c.setUserId(userId);
                        c.setCartType(type);
                        return c;
                    });
        }
        return cartRepository.findByGuestIdAndCartType(guestId, type)
                .orElseGet(() -> {
                    Cart c = new Cart();
                    c.setGuestId(guestId);
                    c.setCartType(type);
                    return c;
                });
    }

    private Cart resolveCart(String userId, String guestId, String cartType) {
        if (userId != null && !userId.isBlank()) {
            return cartRepository.findByUserIdAndCartType(userId, cartType)
                    .orElseThrow(() -> new RuntimeException("Cart not found"));
        }
        return cartRepository.findByGuestIdAndCartType(guestId, cartType)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
    }

    private String resolveCartType(String cartType) {
        if (cartType == null || cartType.isBlank()) return "BRIDE";
        // ✅ FIX: always uppercase so "bride", "Bride", "BRIDE" all work
        return cartType.toUpperCase();
    }

    private boolean safeEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}