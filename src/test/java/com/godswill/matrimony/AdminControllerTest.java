package com.godswill.matrimony;

import com.godswill.matrimony.controller.AdminController;
import com.godswill.matrimony.model.Profile;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.repository.ProfileRepository;
import com.godswill.matrimony.service.EmailService;
import com.godswill.matrimony.service.ImageStorageService;
import com.godswill.matrimony.service.ProfileService;
import com.godswill.matrimony.service.UserService;
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

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminController Tests")
class AdminControllerTest {

    @Mock private ProfileRepository profileRepository;
    @Mock private ProfileService profileService;
    @Mock private ImageStorageService imageStorageService;
    @Mock private UserService userService;
    @Mock private EmailService emailService;

    @InjectMocks
    private AdminController adminController;

    private MockMvc mockMvc;
    private MockHttpSession adminSession;
    private MockHttpSession guestSession;
    private User adminUser;
    private Profile mockProfile;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();

        // Admin user
        adminUser = new User();
        adminUser.setId("admin-001");
        adminUser.setFirstName("Admin");
        adminUser.setEmail("admin@test.com");
        adminUser.setRole("ADMIN");

        // Mock profile
        mockProfile = new Profile();
        mockProfile.setId("profile-001");
        mockProfile.setFirstName("John");
        mockProfile.setLastName("Doe");
        mockProfile.setVerified(false);
        mockProfile.setActive(true);

        adminSession = new MockHttpSession();
        adminSession.setAttribute("user", adminUser);

        guestSession = new MockHttpSession();
    }

    // ══════════════════════════════════════════════
    //  GET /admin  — redirect to all profiles
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /admin — admin user redirects to /admin/profiles/all")
    void testAdminHome_AsAdmin_RedirectsToAllProfiles() throws Exception {
        mockMvc.perform(get("/admin").session(adminSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/profiles/all"));
    }

    @Test
    @DisplayName("GET /admin — guest user redirects to /login")
    void testAdminHome_AsGuest_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin").session(guestSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("GET /admin — non-admin user redirects to /login")
    void testAdminHome_AsNonAdmin_RedirectsToLogin() throws Exception {
        User normalUser = new User();
        normalUser.setRole("USER");
        MockHttpSession userSession = new MockHttpSession();
        userSession.setAttribute("user", normalUser);

        mockMvc.perform(get("/admin").session(userSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ══════════════════════════════════════════════
    //  GET /admin/profiles/all
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /admin/profiles/all — returns admin-all-profiles view with model attributes")
    void testAllProfiles_AsAdmin_ReturnsView() throws Exception {
        Profile verified = new Profile();
        verified.setVerified(true);
        verified.setActive(true);

        Profile unverified = new Profile();
        unverified.setVerified(false);
        unverified.setActive(true);

        when(profileRepository.findAll()).thenReturn(List.of(verified, unverified));

        mockMvc.perform(get("/admin/profiles/all").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-all-profiles"))
                .andExpect(model().attributeExists("profiles"))
                .andExpect(model().attribute("totalCount", 2))
                .andExpect(model().attribute("verifiedCount", 1L))
                .andExpect(model().attribute("pendingCount", 1L))
                .andExpect(model().attribute("activePage", "admin"));
    }

    @Test
    @DisplayName("GET /admin/profiles/all — guest redirects to /login")
    void testAllProfiles_AsGuest_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/profiles/all").session(guestSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("GET /admin/profiles/all — empty profile list returns zero counts")
    void testAllProfiles_EmptyList_ZeroCounts() throws Exception {
        when(profileRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/admin/profiles/all").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalCount", 0))
                .andExpect(model().attribute("verifiedCount", 0L))
                .andExpect(model().attribute("pendingCount", 0L));
    }

    // ══════════════════════════════════════════════
    //  GET /admin/profiles/pending
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /admin/profiles/pending — returns pending profiles view")
    void testPendingProfiles_AsAdmin_ReturnsView() throws Exception {
        when(profileRepository.findByVerified(false)).thenReturn(List.of(mockProfile));

        mockMvc.perform(get("/admin/profiles/pending").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-pending-profiles"))
                .andExpect(model().attributeExists("profiles"))
                .andExpect(model().attribute("activePage", "admin"));
    }

    @Test
    @DisplayName("GET /admin/profiles/pending — guest redirects to /login")
    void testPendingProfiles_AsGuest_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/profiles/pending").session(guestSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ══════════════════════════════════════════════
    //  POST /admin/profiles/{id}/approve
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("POST /admin/profiles/{id}/approve — approves profile and redirects to pending")
    void testApprove_AsAdmin_ApprovesAndRedirects() throws Exception {
        when(profileRepository.findById("profile-001")).thenReturn(Optional.of(mockProfile));

        mockMvc.perform(post("/admin/profiles/profile-001/approve").session(adminSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/profiles/pending"));

        verify(profileRepository, times(1)).save(any(Profile.class));
    }

    @Test
    @DisplayName("POST /admin/profiles/{id}/approve — profile not found still redirects")
    void testApprove_ProfileNotFound_StillRedirects() throws Exception {
        when(profileRepository.findById("non-existent")).thenReturn(Optional.empty());

        mockMvc.perform(post("/admin/profiles/non-existent/approve").session(adminSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/profiles/pending"));

        verify(profileRepository, never()).save(any());
    }

    @Test
    @DisplayName("POST /admin/profiles/{id}/approve — guest redirects to /login")
    void testApprove_AsGuest_RedirectsToLogin() throws Exception {
        mockMvc.perform(post("/admin/profiles/profile-001/approve").session(guestSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ══════════════════════════════════════════════
    //  POST /admin/profiles/{id}/reject
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("POST /admin/profiles/{id}/reject — rejects profile and redirects to pending")
    void testReject_AsAdmin_RejectsAndRedirects() throws Exception {
        when(profileRepository.findById("profile-001")).thenReturn(Optional.of(mockProfile));

        mockMvc.perform(post("/admin/profiles/profile-001/reject").session(adminSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/profiles/pending"));

        verify(profileRepository, times(1)).save(any(Profile.class));
    }

    @Test
    @DisplayName("POST /admin/profiles/{id}/reject — guest redirects to /login")
    void testReject_AsGuest_RedirectsToLogin() throws Exception {
        mockMvc.perform(post("/admin/profiles/profile-001/reject").session(guestSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ══════════════════════════════════════════════
    //  POST /admin/profiles/{id}/delete
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("POST /admin/profiles/{id}/delete — deletes profile and redirects to all")
    void testDeleteProfile_AsAdmin_DeletesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/profiles/profile-001/delete").session(adminSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/profiles/all"));

        verify(profileRepository, times(1)).deleteById("profile-001");
    }

    @Test
    @DisplayName("POST /admin/profiles/{id}/delete — guest redirects to /login")
    void testDeleteProfile_AsGuest_RedirectsToLogin() throws Exception {
        mockMvc.perform(post("/admin/profiles/profile-001/delete").session(guestSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ══════════════════════════════════════════════
    //  GET /admin/profiles/create
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /admin/profiles/create — returns admin-create-user view")
    void testShowAdminCreateUser_AsAdmin_ReturnsView() throws Exception {
        mockMvc.perform(get("/admin/profiles/create").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-create-user"))
                .andExpect(model().attributeExists("pageTitle"));
    }

    @Test
    @DisplayName("GET /admin/profiles/create — guest redirects to /login")
    void testShowAdminCreateUser_AsGuest_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/profiles/create").session(guestSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ══════════════════════════════════════════════
    //  POST /admin/users/create
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("POST /admin/users/create — passwords mismatch redirects with error")
    void testAdminCreateUser_PasswordMismatch_RedirectsWithError() throws Exception {
        mockMvc.perform(post("/admin/users/create").session(adminSession)
                        .param("firstName", "Jane")
                        .param("lastName", "Doe")
                        .param("email", "jane@test.com")
                        .param("phone", "9876543210")
                        .param("gender", "female")
                        .param("dateOfBirth", "1995-05-10")
                        .param("password", "pass123")
                        .param("confirmPassword", "different"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/profiles/create"));
    }

    @Test
    @DisplayName("POST /admin/users/create — duplicate email redirects with error")
    void testAdminCreateUser_DuplicateEmail_RedirectsWithError() throws Exception {
        when(userService.existsByEmail("jane@test.com")).thenReturn(true);

        mockMvc.perform(post("/admin/users/create").session(adminSession)
                        .param("firstName", "Jane")
                        .param("lastName", "Doe")
                        .param("email", "jane@test.com")
                        .param("phone", "9876543210")
                        .param("gender", "female")
                        .param("dateOfBirth", "1995-05-10")
                        .param("password", "pass123")
                        .param("confirmPassword", "pass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/profiles/create"));
    }

    @Test
    @DisplayName("POST /admin/users/create — duplicate phone redirects with error")
    void testAdminCreateUser_DuplicatePhone_RedirectsWithError() throws Exception {
        when(userService.existsByEmail(anyString())).thenReturn(false);
        when(userService.existsByPhone("9876543210")).thenReturn(true);

        mockMvc.perform(post("/admin/users/create").session(adminSession)
                        .param("firstName", "Jane")
                        .param("lastName", "Doe")
                        .param("email", "jane@test.com")
                        .param("phone", "9876543210")
                        .param("gender", "female")
                        .param("dateOfBirth", "1995-05-10")
                        .param("password", "pass123")
                        .param("confirmPassword", "pass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/profiles/create"));
    }

    @Test
    @DisplayName("POST /admin/users/create — valid data creates user & profile, redirects to all")
    void testAdminCreateUser_ValidData_CreatesAndRedirects() throws Exception {
        when(userService.existsByEmail(anyString())).thenReturn(false);
        when(userService.existsByPhone(anyString())).thenReturn(false);

        User savedUser = new User();
        savedUser.setId("new-user-001");
        savedUser.setFirstName("Jane");
        savedUser.setEmail("jane@test.com");

        when(userService.registerUser(any(User.class))).thenReturn(savedUser);

        mockMvc.perform(post("/admin/users/create").session(adminSession)
                        .param("firstName", "Jane")
                        .param("lastName", "Doe")
                        .param("email", "jane@test.com")
                        .param("phone", "9876543210")
                        .param("gender", "female")
                        .param("dateOfBirth", "1995-05-10")
                        .param("password", "pass123")
                        .param("confirmPassword", "pass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/profiles/all"));

        verify(userService, times(1)).registerUser(any(User.class));
        verify(profileService, times(1)).saveProfile(any(Profile.class));
    }

    @Test
    @DisplayName("POST /admin/users/create — guest redirects to /login")
    void testAdminCreateUser_AsGuest_RedirectsToLogin() throws Exception {
        mockMvc.perform(post("/admin/users/create").session(guestSession)
                        .param("firstName", "Jane").param("lastName", "Doe")
                        .param("email", "jane@test.com").param("phone", "9876543210")
                        .param("gender", "female").param("dateOfBirth", "1995-05-10")
                        .param("password", "pass123").param("confirmPassword", "pass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ══════════════════════════════════════════════
    //  GET /admin/profiles/{id}/edit
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /admin/profiles/{id}/edit — returns profile-create view with profile")
    void testShowAdminEditProfile_ProfileExists_ReturnsView() throws Exception {
        when(profileRepository.findById("profile-001")).thenReturn(Optional.of(mockProfile));

        mockMvc.perform(get("/admin/profiles/profile-001/edit").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-create"))
                .andExpect(model().attributeExists("profile"))
                .andExpect(model().attribute("isAdmin", true))
                .andExpect(model().attribute("isPremium", true));
    }

    @Test
    @DisplayName("GET /admin/profiles/{id}/edit — profile not found redirects to all")
    void testShowAdminEditProfile_ProfileNotFound_RedirectsToAll() throws Exception {
        when(profileRepository.findById("bad-id")).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/profiles/bad-id/edit").session(adminSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/profiles/all"));
    }

    @Test
    @DisplayName("GET /admin/profiles/{id}/edit — guest redirects to /login")
    void testShowAdminEditProfile_AsGuest_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/profiles/profile-001/edit").session(guestSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ══════════════════════════════════════════════
    //  POST /admin/profiles/{id}/edit
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("POST /admin/profiles/{id}/edit — saves updated profile and redirects to all")
    void testAdminEditProfile_ValidData_SavesAndRedirects() throws Exception {
        when(profileRepository.findById("profile-001")).thenReturn(Optional.of(mockProfile));

        mockMvc.perform(post("/admin/profiles/profile-001/edit").session(adminSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/profiles/all"));

        verify(profileRepository, times(1)).save(any(Profile.class));
    }

    @Test
    @DisplayName("POST /admin/profiles/{id}/edit — profile not found redirects to all")
    void testAdminEditProfile_ProfileNotFound_RedirectsToAll() throws Exception {
        when(profileRepository.findById("bad-id")).thenReturn(Optional.empty());

        mockMvc.perform(post("/admin/profiles/bad-id/edit").session(adminSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/profiles/all"));
    }

    @Test
    @DisplayName("POST /admin/profiles/{id}/edit — guest redirects to /login")
    void testAdminEditProfile_AsGuest_RedirectsToLogin() throws Exception {
        mockMvc.perform(post("/admin/profiles/profile-001/edit").session(guestSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}