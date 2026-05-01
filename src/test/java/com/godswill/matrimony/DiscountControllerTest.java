package com.godswill.matrimony;

import com.godswill.matrimony.controller.DiscountController;
import com.godswill.matrimony.model.Profile;
import com.godswill.matrimony.model.SubscriptionPlan;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.repository.ProfileRepository;
import com.godswill.matrimony.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DiscountControllerTest {

    @InjectMocks
    private DiscountController controller;

    @Mock private ProfileRepository profileRepository;
    @Mock private UserRepository userRepository;

    private Profile testProfile;
    private User premiumUser;
    private User freeUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testProfile = new Profile();
        testProfile.setProfileNumber("MAT10001");
        testProfile.setFirstName("Ravi");
        testProfile.setLastName("Kumar");
        testProfile.setUserId("user-001");

        premiumUser = new User();
        premiumUser.setId("user-001");
        // assume isPremium() returns true and subscription is active

        freeUser = new User();
        freeUser.setId("user-002");
    }

    // ── Missing / blank profile number ────────────────────────────────────────

    @Test
    void checkDiscount_nullProfileNumber_returnsBadRequest() {
        ResponseEntity<?> response = controller.checkDiscount(Map.of());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("error")).isEqualTo("Profile number is required");
    }

    @Test
    void checkDiscount_blankProfileNumber_returnsBadRequest() {
        ResponseEntity<?> response = controller.checkDiscount(Map.of("profileNumber", "  "));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── Profile not found ─────────────────────────────────────────────────────

    @Test
    void checkDiscount_profileNotFound_returnsFoundFalse() {
        when(profileRepository.findByProfileNumber("MAT99999"))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.checkDiscount(
                Map.of("profileNumber", "MAT99999"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("found")).isEqualTo(false);
        assertThat(body.get("isPremium")).isEqualTo(false);
        assertThat(body.get("discountPercent")).isEqualTo(0);
    }

    // ── Profile found but no userId ───────────────────────────────────────────

    @Test
    void checkDiscount_profileWithNoUserId_returnsNoUserLinked() {
        testProfile.setUserId(null);
        when(profileRepository.findByProfileNumber("MAT10001"))
                .thenReturn(Optional.of(testProfile));

        ResponseEntity<?> response = controller.checkDiscount(
                Map.of("profileNumber", "MAT10001"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("found")).isEqualTo(true);
        assertThat(body.get("isPremium")).isEqualTo(false);
        assertThat(body.get("profileName")).isEqualTo("Ravi Kumar");
    }

    // ── User not found in DB ──────────────────────────────────────────────────

    @Test
    void checkDiscount_userNotFound_returnsUserNotFound() {
        when(profileRepository.findByProfileNumber("MAT10001"))
                .thenReturn(Optional.of(testProfile));
        when(userRepository.findById("user-001")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.checkDiscount(
                Map.of("profileNumber", "MAT10001"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("found")).isEqualTo(true);
        assertThat(body.get("isPremium")).isEqualTo(false);
    }

    // ── Premium user ──────────────────────────────────────────────────────────

    @Test
    void checkDiscount_premiumUser_returns10PercentDiscount() {
        User mockPremium = mock(User.class);
        when(mockPremium.isPremium()).thenReturn(true);
        when(mockPremium.getCurrentPlan()).thenReturn(SubscriptionPlan.GOLD); // adjust to your actual enum

        when(profileRepository.findByProfileNumber("MAT10001"))
                .thenReturn(Optional.of(testProfile));
        when(userRepository.findById("user-001")).thenReturn(Optional.of(mockPremium));

        ResponseEntity<?> response = controller.checkDiscount(
                Map.of("profileNumber", "MAT10001"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("isPremium")).isEqualTo(true);
        assertThat(body.get("discountPercent")).isEqualTo(10);
        assertThat(body.get("profileName")).isEqualTo("Ravi Kumar");
        assertThat(body.get("message").toString()).contains("10%");
    }

    // ── Free (non-premium) user ───────────────────────────────────────────────

    @Test
    void checkDiscount_freeUser_returns0PercentDiscount() {
        User mockFree = mock(User.class);
        when(mockFree.isPremium()).thenReturn(false);
        when(mockFree.isSubscriptionExpired()).thenReturn(false);

        when(profileRepository.findByProfileNumber("MAT10001"))
                .thenReturn(Optional.of(testProfile));
        when(userRepository.findById("user-001")).thenReturn(Optional.of(mockFree));

        ResponseEntity<?> response = controller.checkDiscount(
                Map.of("profileNumber", "MAT10001"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("isPremium")).isEqualTo(false);
        assertThat(body.get("discountPercent")).isEqualTo(0);
    }

    // ── Expired subscription ──────────────────────────────────────────────────

    @Test
    void checkDiscount_expiredUser_mentionsExpired() {
        User mockExpired = mock(User.class);
        when(mockExpired.isPremium()).thenReturn(false);
        when(mockExpired.isSubscriptionExpired()).thenReturn(true);

        when(profileRepository.findByProfileNumber("MAT10001"))
                .thenReturn(Optional.of(testProfile));
        when(userRepository.findById("user-001")).thenReturn(Optional.of(mockExpired));

        ResponseEntity<?> response = controller.checkDiscount(
                Map.of("profileNumber", "MAT10001"));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("message").toString()).contains("expired");
    }

    // ── Profile number is uppercased before lookup ────────────────────────────

    @Test
    void checkDiscount_lowercaseProfileNumber_isNormalized() {
        when(profileRepository.findByProfileNumber("MAT10001"))
                .thenReturn(Optional.empty());

        controller.checkDiscount(Map.of("profileNumber", "mat10001"));

        verify(profileRepository).findByProfileNumber("MAT10001");
    }
}