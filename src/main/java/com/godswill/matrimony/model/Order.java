package com.godswill.matrimony.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    private String userId;      // null for guest orders
    private String guestId;     // set for guest orders (from localStorage)

    private List<Cart.CartItem> items;

    private double totalAmount;
    private double discountAmount;
    private double finalAmount;

    // Razorpay
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;

    private OrderStatus status;
    private PaymentStatus paymentStatus;

    private ShippingAddress shippingAddress;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdAt;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime updatedAt;

    public enum OrderStatus {
        PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    }

    public enum PaymentStatus {
        PENDING, PAID, FAILED, REFUNDED
    }

    // ── Inner: ShippingAddress ─────────────────────────────────────────────────
    public static class ShippingAddress {
        private String fullName;
        private String phone;
        private String email;           // captured for guest orders
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String pincode;

        public String getFullName()  { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getPhone()     { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getEmail()     { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getAddressLine1() { return addressLine1; }
        public void setAddressLine1(String a) { this.addressLine1 = a; }

        public String getAddressLine2() { return addressLine2; }
        public void setAddressLine2(String a) { this.addressLine2 = a; }

        public String getCity()  { return city; }
        public void setCity(String city) { this.city = city; }

        public String getState() { return state; }
        public void setState(String state) { this.state = state; }

        public String getPincode() { return pincode; }
        public void setPincode(String pincode) { this.pincode = pincode; }
    }

    public Order() {
        this.status = OrderStatus.PENDING;
        this.paymentStatus = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────
    public String getId()    { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId()  { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getGuestId() { return guestId; }
    public void setGuestId(String guestId) { this.guestId = guestId; }

    public List<Cart.CartItem> getItems() { return items; }
    public void setItems(List<Cart.CartItem> items) { this.items = items; }

    public double getTotalAmount()   { return totalAmount; }
    public void setTotalAmount(double t) { this.totalAmount = t; }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double d) { this.discountAmount = d; }

    public double getFinalAmount()   { return finalAmount; }
    public void setFinalAmount(double f) { this.finalAmount = f; }

    public String getRazorpayOrderId()   { return razorpayOrderId; }
    public void setRazorpayOrderId(String r) { this.razorpayOrderId = r; }

    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String r) { this.razorpayPaymentId = r; }

    public String getRazorpaySignature() { return razorpaySignature; }
    public void setRazorpaySignature(String r) { this.razorpaySignature = r; }

    public OrderStatus getStatus()       { return status; }
    public void setStatus(OrderStatus s) { this.status = s; }

    public PaymentStatus getPaymentStatus()         { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus p)   { this.paymentStatus = p; }

    public ShippingAddress getShippingAddress()          { return shippingAddress; }
    public void setShippingAddress(ShippingAddress s)    { this.shippingAddress = s; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime c) { this.createdAt = c; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime u) { this.updatedAt = u; }
}