package com.godswill.matrimony;

import com.godswill.matrimony.controller.ShortlistController;
import com.godswill.matrimony.controller.ShortlistPageController;
import com.godswill.matrimony.model.Profile;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.service.ShortlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ================================================================
//  ShortlistControllerTest  (REST — /api/shortlist)
// ================================================================
@ExtendWith(MockitoExtension.class)
@DisplayName("ShortlistController Tests")
class ShortlistControllerTest {

    @Mock private ShortlistService shortlistService;

    @InjectMocks
    private ShortlistController shortlistController;

    private MockMvc mockMvc;
    private MockHttpSession userSession;
    private MockHttpSession guestSession;
    private User mockUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(shortlistController).build();

        mockUser = new User();
        mockUser.setId("user-001");
        mockUser.setEmail("user@test.com");

        userSession = new MockHttpSession();
        userSession.setAttribute("user", mockUser);

        guestSession = new MockHttpSession();
    }

    // ══════════════════════════════════════════════
    //  POST /api/shortlist/toggle/{profileId}
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("POST /api/shortlist/toggle/{id} — unauthenticated returns 401")
    void testToggleShortlist_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(post("/api/shortlist/toggle/profile-001").session(guestSession))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/shortlist/toggle/{id} — adds to shortlist returns shortlisted=true")
    void testToggleShortlist_AddsToShortlist_ReturnsTrue() throws Exception {
        when(shortlistService.toggleShortlist("user@test.com", "profile-001")).thenReturn(true);

        mockMvc.perform(post("/api/shortlist/toggle/profile-001").session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.shortlisted").value(true));
    }

    @Test
    @DisplayName("POST /api/shortlist/toggle/{id} — removes from shortlist returns shortlisted=false")
    void testToggleShortlist_RemovesFromShortlist_ReturnsFalse() throws Exception {
        when(shortlistService.toggleShortlist("user@test.com", "profile-001")).thenReturn(false);

        mockMvc.perform(post("/api/shortlist/toggle/profile-001").session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortlisted").value(false));
    }

    @Test
    @DisplayName("POST /api/shortlist/toggle/{id} — service throws exception returns 500")
    void testToggleShortlist_ServiceThrows_Returns500() throws Exception {
        when(shortlistService.toggleShortlist(anyString(), anyString()))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/api/shortlist/toggle/profile-001").session(userSession))
                .andExpect(status().isInternalServerError());
    }

    // ══════════════════════════════════════════════
    //  GET /api/shortlist/check/{profileId}
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/shortlist/check/{id} — unauthenticated returns shortlisted=false")
    void testCheckShortlistStatus_Unauthenticated_ReturnsFalse() throws Exception {
        mockMvc.perform(get("/api/shortlist/check/profile-001").session(guestSession))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.shortlisted").value(false));
    }

    @Test
    @DisplayName("GET /api/shortlist/check/{id} — profile is shortlisted returns true")
    void testCheckShortlistStatus_IsShortlisted_ReturnsTrue() throws Exception {
        when(shortlistService.isShortlisted("user@test.com", "profile-001")).thenReturn(true);

        mockMvc.perform(get("/api/shortlist/check/profile-001").session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortlisted").value(true));
    }

    @Test
    @DisplayName("GET /api/shortlist/check/{id} — profile is not shortlisted returns false")
    void testCheckShortlistStatus_NotShortlisted_ReturnsFalse() throws Exception {
        when(shortlistService.isShortlisted("user@test.com", "profile-001")).thenReturn(false);

        mockMvc.perform(get("/api/shortlist/check/profile-001").session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortlisted").value(false));
    }

    @Test
    @DisplayName("GET /api/shortlist/check/{id} — service throws exception returns 500")
    void testCheckShortlistStatus_ServiceThrows_Returns500() throws Exception {
        when(shortlistService.isShortlisted(anyString(), anyString()))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/shortlist/check/profile-001").session(userSession))
                .andExpect(status().isInternalServerError());
    }
}


