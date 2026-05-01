package com.godswill.matrimony;

import com.godswill.matrimony.controller.ProfileController;
import com.godswill.matrimony.model.Profile;
import com.godswill.matrimony.model.Subscription;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.service.ImageStorageService;
import com.godswill.matrimony.service.ProfileService;
import com.godswill.matrimony.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileController Tests")
class ProfileControllerTest {

    @Mock private ProfileService profileService;
    @Mock private ImageStorageService imageStorageService;
    @Mock private SubscriptionService subscriptionService; // ← ADDED

    @InjectMocks
    private ProfileController profileController;

    private MockMvc mockMvc;
    private MockHttpSession userSession;
    private MockHttpSession adminSession;
    private MockHttpSession guestSession;
    private User normalUser;
    private User adminUser;
    private Profile mockProfile;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(profileController).build();
        ReflectionTestUtils.setField(profileController, "uploadDir", "/tmp/uploads");

        normalUser = new User();
        normalUser.setId("user-001");
        normalUser.setRole("USER");
        normalUser.setCurrentPlan(null); // not premium

        adminUser = new User();
        adminUser.setId("admin-001");
        adminUser.setRole("ADMIN");

        mockProfile = new Profile();
        mockProfile.setId("profile-001");
        mockProfile.setFirstName("John");
        mockProfile.setLastName("Doe");
        mockProfile.setUserId("user-001");

        userSession = new MockHttpSession();
        userSession.setAttribute("user", normalUser);

        adminSession = new MockHttpSession();
        adminSession.setAttribute("user", adminUser);

        guestSession = new MockHttpSession();
    }

    // ══════════════════════════════════════════════
    //  GET /profiles
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /profiles — returns profile-listing view with all profiles")
    void testListProfiles_NoFilter_ReturnsAllProfiles() throws Exception {
        when(profileService.getAllProfiles()).thenReturn(List.of(mockProfile));

        mockMvc.perform(get("/profiles"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-listing"))
                .andExpect(model().attributeExists("profiles"))
                .andExpect(model().attribute("activePage", "profiles"));
    }

    @Test
    @DisplayName("GET /profiles — search by query calls searchProfiles")
    void testListProfiles_WithQuery_CallsSearch() throws Exception {
        when(profileService.searchProfiles("John")).thenReturn(List.of(mockProfile));

        mockMvc.perform(get("/profiles").param("q", "John"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-listing"));

        verify(profileService, times(1)).searchProfiles("John");
    }

    @Test
    @DisplayName("GET /profiles — filter by gender calls filterProfiles")
    void testListProfiles_WithGenderFilter_CallsFilter() throws Exception {
        when(profileService.filterProfiles("male", null, null, null, null, null, null, null, null))
                .thenReturn(List.of(mockProfile));

        mockMvc.perform(get("/profiles").param("gender", "male"))
                .andExpect(status().isOk());

        verify(profileService, times(1)).filterProfiles(eq("male"), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("GET /profiles — negative page is clamped to 0")
    void testListProfiles_NegativePage_ClampedToZero() throws Exception {
        when(profileService.getAllProfiles()).thenReturn(List.of(mockProfile));

        mockMvc.perform(get("/profiles").param("page", "-5"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentPage", 0));
    }

    @Test
    @DisplayName("GET /profiles — invalid size is clamped to 20")
    void testListProfiles_InvalidSize_ClampedTo20() throws Exception {
        when(profileService.getAllProfiles()).thenReturn(List.of(mockProfile));

        mockMvc.perform(get("/profiles").param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pageSize", 20));
    }

    @Test
    @DisplayName("GET /profiles — guest is not logged in, isPremium is false")
    void testListProfiles_GuestUser_IsNotPremium() throws Exception {
        when(profileService.getAllProfiles()).thenReturn(List.of());

        mockMvc.perform(get("/profiles").session(guestSession))
                .andExpect(model().attribute("isLoggedIn", false))
                .andExpect(model().attribute("isPremium", false));
    }

    @Test
    @DisplayName("GET /profiles — admin user has isPremium true")
    void testListProfiles_AdminUser_IsPremium() throws Exception {
        when(profileService.getAllProfiles()).thenReturn(List.of());

        mockMvc.perform(get("/profiles").session(adminSession))
                .andExpect(model().attribute("isAdmin", true))
                .andExpect(model().attribute("isPremium", true));
    }

    // ══════════════════════════════════════════════
    //  GET /profile/{id}
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /profile/{id} — found profile returns profile-details view")
    void testViewProfile_Found_ReturnsView() throws Exception {
        when(profileService.getProfileById("profile-001")).thenReturn(Optional.of(mockProfile));

        mockMvc.perform(get("/profile/profile-001"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-details"))
                .andExpect(model().attributeExists("profile"));
    }

    @Test
    @DisplayName("GET /profile/{id} — not found redirects to /profiles")
    void testViewProfile_NotFound_RedirectsToProfiles() throws Exception {
        when(profileService.getProfileById("bad-id")).thenReturn(Optional.empty());

        mockMvc.perform(get("/profile/bad-id"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profiles"));
    }

    @Test
    @DisplayName("GET /profile/{id} — admin can view full profile and contact")
    void testViewProfile_AdminUser_CanViewFullProfile() throws Exception {
        when(profileService.getProfileById("profile-001")).thenReturn(Optional.of(mockProfile));

        mockMvc.perform(get("/profile/profile-001").session(adminSession))
                .andExpect(model().attribute("canViewFullProfile", true))
                .andExpect(model().attribute("canViewContact", true));
    }

    @Test
    @DisplayName("GET /profile/{id} — guest cannot view full profile or contact")
    void testViewProfile_GuestUser_CannotViewFullProfile() throws Exception {
        when(profileService.getProfileById("profile-001")).thenReturn(Optional.of(mockProfile));

        mockMvc.perform(get("/profile/profile-001").session(guestSession))
                .andExpect(model().attribute("canViewFullProfile", false))
                .andExpect(model().attribute("canViewContact", false));
    }

    // ══════════════════════════════════════════════
    //  GET /profile/create
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /profile/create — logged-in user returns profile-create view")
    void testShowCreateProfileForm_LoggedIn_ReturnsView() throws Exception {
        mockMvc.perform(get("/profile/create").session(userSession))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-create"))
                .andExpect(model().attributeExists("profile"));
    }

    @Test
    @DisplayName("GET /profile/create — guest redirects to /login")
    void testShowCreateProfileForm_Guest_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/profile/create").session(guestSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("GET /profile/create — admin redirects to /admin/profiles/create")
    void testShowCreateProfileForm_Admin_RedirectsToAdminCreate() throws Exception {
        mockMvc.perform(get("/profile/create").session(adminSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/profiles/create"));
    }

    // ══════════════════════════════════════════════
    //  POST /profile/create
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("POST /profile/create — logged-in user creates profile and redirects to profile page")
    void testCreateProfile_LoggedIn_SavesAndRedirects() throws Exception {
        mockProfile.setId("profile-001");
        when(profileService.createProfile(any(Profile.class))).thenReturn(mockProfile);

        mockMvc.perform(post("/profile/create").session(userSession)
                        .param("firstName", "John")
                        .param("lastName", "Doe"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/profile-001"));

        verify(profileService, times(1)).createProfile(any(Profile.class));
    }

    @Test
    @DisplayName("POST /profile/create — guest redirects to /login")
    void testCreateProfile_Guest_RedirectsToLogin() throws Exception {
        mockMvc.perform(post("/profile/create").session(guestSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("POST /profile/create — admin redirects to admin create page")
    void testCreateProfile_Admin_RedirectsToAdminCreate() throws Exception {
        mockMvc.perform(post("/profile/create").session(adminSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/profiles/create"));
    }

    // ══════════════════════════════════════════════
    //  GET /profile/my-matrimony-profile
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /profile/my-matrimony-profile — user has profile redirects to profile/{id}")
    void testViewMyMatrimonyProfile_HasProfile_RedirectsToProfile() throws Exception {
        when(profileService.getProfileByUserId("user-001")).thenReturn(Optional.of(mockProfile));

        mockMvc.perform(get("/profile/my-matrimony-profile").session(userSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/profile-001"));
    }

    @Test
    @DisplayName("GET /profile/my-matrimony-profile — no profile redirects to /profile/create")
    void testViewMyMatrimonyProfile_NoProfile_RedirectsToCreate() throws Exception {
        when(profileService.getProfileByUserId("user-001")).thenReturn(Optional.empty());

        mockMvc.perform(get("/profile/my-matrimony-profile").session(userSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/create"));
    }

    @Test
    @DisplayName("GET /profile/my-matrimony-profile — guest redirects to /login")
    void testViewMyMatrimonyProfile_Guest_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/profile/my-matrimony-profile").session(guestSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ══════════════════════════════════════════════
    //  GET /profile/edit
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /profile/edit — user has profile returns profile-create view")
    void testEditProfile_HasProfile_ReturnsView() throws Exception {
        when(profileService.getProfileByUserId("user-001")).thenReturn(Optional.of(mockProfile));

        mockMvc.perform(get("/profile/edit").session(userSession))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-create"))
                .andExpect(model().attributeExists("profile"));
    }

    @Test
    @DisplayName("GET /profile/edit — no profile redirects to /profile/create")
    void testEditProfile_NoProfile_RedirectsToCreate() throws Exception {
        when(profileService.getProfileByUserId("user-001")).thenReturn(Optional.empty());

        mockMvc.perform(get("/profile/edit").session(userSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/create"));
    }

    @Test
    @DisplayName("GET /profile/edit — guest redirects to /login")
    void testEditProfile_Guest_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/profile/edit").session(guestSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ══════════════════════════════════════════════
    //  GET /my-profile
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /my-profile — logged-in user returns user-profile view")
    void testMyProfile_LoggedIn_ReturnsView() throws Exception {
        when(profileService.getProfileByUserId("user-001")).thenReturn(Optional.empty());
        when(subscriptionService.getActiveSubscription("user-001")).thenReturn(Optional.empty()); // ← ADDED stub

        mockMvc.perform(get("/my-profile").session(userSession))
                .andExpect(status().isOk())
                .andExpect(view().name("user-profile"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    @DisplayName("GET /my-profile — guest redirects to /login")
    void testMyProfile_Guest_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/my-profile").session(guestSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}