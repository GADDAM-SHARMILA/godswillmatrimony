package com.godswill.matrimony.repository;

import com.godswill.matrimony.model.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends MongoRepository<Cart, String> {

    // ── Logged-in user: one cart per type ─────────────────────────────────────
    Optional<Cart> findByUserIdAndCartType(String userId, String cartType);

    List<Cart> findByUserId(String userId);         // fetch both BRIDE + GROOM carts

    void deleteByUserId(String userId);             // clear ALL carts on logout

    void deleteByUserIdAndCartType(String userId, String cartType);

    // ── Guest: one cart per type ──────────────────────────────────────────────
    Optional<Cart> findByGuestIdAndCartType(String guestId, String cartType);

    List<Cart> findByGuestId(String guestId);       // fetch both carts for guest

    void deleteByGuestId(String guestId);           // clear guest carts

    // ── Migration: find all guest carts to merge on login ────────────────────
    List<Cart> findByGuestIdAndUserIdIsNull(String guestId);
}