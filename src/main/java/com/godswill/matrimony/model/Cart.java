package com.godswill.matrimony.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "carts")
@CompoundIndexes({
        // Each user has ONE cart per type (BRIDE / GROOM)
        @CompoundIndex(name = "idx_userId_cartType",  def = "{'userId': 1, 'cartType': 1}",  unique = true, sparse = true),
        @CompoundIndex(name = "idx_guestId_cartType", def = "{'guestId': 1, 'cartType': 1}", unique = true, sparse = true)
})
public class Cart {

    @Id
    private String id;

    // For logged-in users — maps to User.id
    private String userId;

    // For guests — from localStorage ('guest_xxxxx')
    // Null once user logs in and cart is migrated
    private String guestId;

    // BRIDE or GROOM — separate cart per category
    private String cartType;    // "BRIDE" or "GROOM"

    private List<CartItem> items = new ArrayList<>();

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime updatedAt;

    public Cart() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Inner CartItem ─────────────────────────────────────────────────────────
    public static class CartItem {
        private String productId;
        private String productName;
        private String imageUrl;
        private double price;
        private int quantity;
        private String selectedSize;
        private String selectedVariant;
        private String category;   // "BRIDE" or "GROOM"

        public CartItem() {}

        public String getProductId()    { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public String getProductName()  { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public String getImageUrl()     { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public double getPrice()        { return price; }
        public void setPrice(double price) { this.price = price; }

        public int getQuantity()        { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        public String getSelectedSize() { return selectedSize; }
        public void setSelectedSize(String selectedSize) { this.selectedSize = selectedSize; }

        public String getSelectedVariant() { return selectedVariant; }
        public void setSelectedVariant(String selectedVariant) { this.selectedVariant = selectedVariant; }

        public String getCategory()     { return category; }
        public void setCategory(String category) { this.category = category; }

        public double getSubtotal()     { return price * quantity; }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    public double getTotalAmount() {
        return items.stream().mapToDouble(CartItem::getSubtotal).sum();
    }

    public int getTotalItems() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────
    public String getId()           { return id; }
    public void setId(String id)    { this.id = id; }

    public String getUserId()       { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getGuestId()      { return guestId; }
    public void setGuestId(String guestId) { this.guestId = guestId; }

    public String getCartType()     { return cartType; }
    public void setCartType(String cartType) { this.cartType = cartType; }

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}