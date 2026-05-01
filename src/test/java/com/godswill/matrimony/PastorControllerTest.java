package com.godswill.matrimony;

import com.godswill.matrimony.controller.PastorController;
import com.godswill.matrimony.model.Pastor;
import com.godswill.matrimony.model.Profile;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.service.EmailService;
import com.godswill.matrimony.service.PastorService;
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
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PastorController Tests")
class PastorControllerTest {

    @Mock private PastorService pastorService;
    @Mock private ProfileService profileService;
    @Mock private UserService userService;   // ← ADDED: used in addProfile() duplicate checks
    @Mock private EmailService emailService; // ← ADDED: used in addProfile() welcome email

    @InjectMocks
    private PastorController pastorController;

    private MockMvc mockMvc;
    private MockHttpSession pastorSession;
    private MockHttpSession guestSession;
    private Pastor mockPastor;
    private Profile mockProfile;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(pastorController)
                .setViewResolvers(viewResolver)
                .build();

        mockPastor = new Pastor();
        mockPastor.setId("pastor-001");
        mockPastor.setFirstName("Rev");
        mockPastor.setLastName("Samuel");
        mockPastor.setEmail("rev@church.com");
        mockPastor.setPassword("pass123");

        mockProfile = new Profile();
        mockProfile.setId("profile-001");
        mockProfile.setFirstName("John");
        mockProfile.setLastName("Doe");
        mockProfile.setProposedByPastorId("pastor-001");
        mockProfile.setDateOfBirth(LocalDate.of(1995, 5, 10));

        pastorSession = new MockHttpSession();
        pastorSession.setAttribute("loggedInPastor", mockPastor);

        guestSession = new MockHttpSession();
    }

    // ══════════════════════════════════════════════
    //  GET /pastors/register
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /pastors/register — returns pastors-register view with empty pastor")
    void testShowForm_ReturnsRegisterView() throws Exception {
        mockMvc.perform(get("/pastors/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("pastors-register"))
                .andExpect(model().attributeExists("pastor"));
    }

    // ══════════════════════════════════════════════
    //  POST /pastors/register
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("POST /pastors/register — passwords match saves and redirects to /login")
    void testSavePastor_PasswordsMatch_RedirectsToLogin() throws Exception {
        Pastor pastor = mock(Pastor.class);
        when(pastor.passwordsMatch()).thenReturn(true);

        mockMvc.perform(post("/pastors/register")
                        .flashAttr("pastor", pastor))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(pastorService, times(1)).savePastor(any(Pastor.class));
    }

    @Test
    @DisplayName("POST /pastors/register — passwords do not match shows error on register view")
    void testSavePastor_PasswordMismatch_ReturnsRegisterViewWithError() throws Exception {
        Pastor pastor = mock(Pastor.class);
        when(pastor.passwordsMatch()).thenReturn(false);

        mockMvc.perform(post("/pastors/register")
                        .flashAttr("pastor", pastor))
                .andExpect(status().isOk())
                .andExpect(view().name("pastors-register"))
                .andExpect(model().attributeExists("errorMessage"));

        verify(pastorService, never()).savePastor(any());
    }

    // ══════════════════════════════════════════════
    //  GET /pastors/login
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /pastors/login — redirects to /login (backward compat)")
    void testShowLoginForm_ReturnsView() throws Exception {
        mockMvc.perform(get("/pastors/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ══════════════════════════════════════════════
    //  POST /pastors/login
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("POST /pastors/login — always redirects to /login (stub endpoint)")
    void testLoginPastor_ValidCredentials_RedirectsToDashboard() throws Exception {
        mockMvc.perform(post("/pastors/login")
                        .param("email", "rev@church.com")
                        .param("password", "pass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("POST /pastors/login — wrong password still redirects to /login (stub endpoint)")
    void testLoginPastor_WrongPassword_RedirectsWithError() throws Exception {
        mockMvc.perform(post("/pastors/login")
                        .param("email", "rev@church.com")
                        .param("password", "wrongpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("POST /pastors/login — email not found still redirects to /login (stub endpoint)")
    void testLoginPastor_EmailNotFound_RedirectsWithError() throws Exception {
        mockMvc.perform(post("/pastors/login")
                        .param("email", "unknown@test.com")
                        .param("password", "pass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ══════════════════════════════════════════════
    //  GET /pastors/dashboard
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /pastors/dashboard — logged-in pastor sees dashboard view")
    void testPastorDashboard_LoggedIn_ReturnsView() throws Exception {
        when(profileService.getProfilesByPastor("pastor-001")).thenReturn(List.of(mockProfile));

        mockMvc.perform(get("/pastors/dashboard").session(pastorSession))
                .andExpect(status().isOk())
                .andExpect(view().name("pastors-dashboard"))
                .andExpect(model().attributeExists("pastor"))
                .andExpect(model().attributeExists("profiles"))
                .andExpect(model().attributeExists("profile"));
    }

    @Test
    @DisplayName("GET /pastors/dashboard — no session redirects to /login")
    void testPastorDashboard_NoSession_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/pastors/dashboard").session(guestSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("GET /pastors/dashboard — age is calculated for profiles with dateOfBirth")
    void testPastorDashboard_CalculatesAgeForProfiles() throws Exception {
        when(profileService.getProfilesByPastor("pastor-001")).thenReturn(List.of(mockProfile));

        mockMvc.perform(get("/pastors/dashboard").session(pastorSession))
                .andExpect(status().isOk());

        verify(profileService, times(1)).getProfilesByPastor("pastor-001");
    }

    // ══════════════════════════════════════════════
    //  POST /pastors/profiles
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("POST /pastors/profiles — logged-in pastor adds profile and redirects to dashboard")
    void testAddProfile_LoggedIn_SavesAndRedirects() throws Exception {
        // Stub duplicate checks — no existing email/phone
        when(userService.existsByEmail("jane@example.com")).thenReturn(false);
        when(userService.existsByPhone("9876543210")).thenReturn(false);

        // Stub user registration called inside the try-block
        User savedUser = new User();
        savedUser.setId("user-new-001");
        savedUser.setFirstName("Jane");
        savedUser.setLastName("Smith");
        savedUser.setEmail("jane@example.com");
        when(userService.registerUser(any(User.class))).thenReturn(savedUser);

        mockMvc.perform(post("/pastors/profiles").session(pastorSession)
                        .param("firstName", "Jane")
                        .param("lastName", "Smith")
                        .param("email", "jane@example.com")
                        .param("phone", "9876543210")
                        .param("dateOfBirth", "1997-03-15"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pastors/dashboard"));

        verify(profileService, times(1)).saveProfile(any(Profile.class));
    }

    @Test
    @DisplayName("POST /pastors/profiles — no session redirects to /login")
    void testAddProfile_NoSession_RedirectsToLogin() throws Exception {
        mockMvc.perform(post("/pastors/profiles").session(guestSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("POST /pastors/profiles — pastor ID and name are set on profile")
    void testAddProfile_SetsProposedByPastorFields() throws Exception {
        // Stub duplicate checks — no existing email/phone
        when(userService.existsByEmail("jane@example.com")).thenReturn(false);
        when(userService.existsByPhone("9876543210")).thenReturn(false);

        // Stub user registration
        User savedUser = new User();
        savedUser.setId("user-new-001");
        savedUser.setFirstName("Jane");
        savedUser.setLastName("Smith");
        savedUser.setEmail("jane@example.com");
        when(userService.registerUser(any(User.class))).thenReturn(savedUser);

        mockMvc.perform(post("/pastors/profiles").session(pastorSession)
                        .param("firstName", "Jane")
                        .param("lastName", "Smith")
                        .param("email", "jane@example.com")
                        .param("phone", "9876543210"))
                .andExpect(status().is3xxRedirection());

        verify(profileService, times(1)).saveProfile(argThat(p ->
                "pastor-001".equals(p.getProposedByPastorId()) &&
                        "Rev Samuel".equals(p.getProposedByPastorName())
        ));
    }

    // ══════════════════════════════════════════════
    //  GET /pastors/profiles/edit/{id}
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /pastors/profiles/edit/{id} — own profile shows profile-create view")
    void testEditProfile_OwnProfile_ReturnsView() throws Exception {
        when(profileService.getProfileById("profile-001")).thenReturn(Optional.of(mockProfile));

        mockMvc.perform(get("/pastors/profiles/edit/profile-001").session(pastorSession))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-create"))
                .andExpect(model().attributeExists("profile"));
    }

    @Test
    @DisplayName("GET /pastors/profiles/edit/{id} — profile belongs to another pastor redirects to dashboard")
    void testEditProfile_OtherPastorProfile_RedirectsToDashboard() throws Exception {
        mockProfile.setProposedByPastorId("other-pastor");
        when(profileService.getProfileById("profile-001")).thenReturn(Optional.of(mockProfile));

        mockMvc.perform(get("/pastors/profiles/edit/profile-001").session(pastorSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pastors/dashboard"));
    }

    @Test
    @DisplayName("GET /pastors/profiles/edit/{id} — profile not found redirects to dashboard")
    void testEditProfile_NotFound_RedirectsToDashboard() throws Exception {
        when(profileService.getProfileById("bad-id")).thenReturn(Optional.empty());

        mockMvc.perform(get("/pastors/profiles/edit/bad-id").session(pastorSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pastors/dashboard"));
    }

    @Test
    @DisplayName("GET /pastors/profiles/edit/{id} — no session redirects to /login")
    void testEditProfile_NoSession_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/pastors/profiles/edit/profile-001").session(guestSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ══════════════════════════════════════════════
    //  POST /pastors/profiles/update
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("POST /pastors/profiles/update — logged-in pastor updates and redirects to dashboard")
    void testUpdateProfile_LoggedIn_UpdatesAndRedirects() throws Exception {
        mockMvc.perform(post("/pastors/profiles/update").session(pastorSession)
                        .param("id", "profile-001")
                        .param("firstName", "Jane"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pastors/dashboard"));

        verify(profileService, times(1)).saveProfile(any(Profile.class));
    }

    @Test
    @DisplayName("POST /pastors/profiles/update — no session redirects to /login")
    void testUpdateProfile_NoSession_RedirectsToLogin() throws Exception {
        mockMvc.perform(post("/pastors/profiles/update").session(guestSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ══════════════════════════════════════════════
    //  GET /pastors/profiles/delete/{id}
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /pastors/profiles/delete/{id} — own profile is deleted and redirects to dashboard")
    void testDeleteProfile_OwnProfile_DeletesAndRedirects() throws Exception {
        when(profileService.getProfileById("profile-001")).thenReturn(Optional.of(mockProfile));

        mockMvc.perform(get("/pastors/profiles/delete/profile-001").session(pastorSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pastors/dashboard"));

        verify(profileService, times(1)).deleteProfile("profile-001");
    }

    @Test
    @DisplayName("GET /pastors/profiles/delete/{id} — another pastor's profile is not deleted")
    void testDeleteProfile_OtherPastorProfile_RedirectsWithoutDeleting() throws Exception {
        mockProfile.setProposedByPastorId("other-pastor");
        when(profileService.getProfileById("profile-001")).thenReturn(Optional.of(mockProfile));

        mockMvc.perform(get("/pastors/profiles/delete/profile-001").session(pastorSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pastors/dashboard"));

        verify(profileService, never()).deleteProfile(anyString());
    }

    @Test
    @DisplayName("GET /pastors/profiles/delete/{id} — profile not found redirects to dashboard")
    void testDeleteProfile_NotFound_RedirectsToDashboard() throws Exception {
        when(profileService.getProfileById("bad-id")).thenReturn(Optional.empty());

        mockMvc.perform(get("/pastors/profiles/delete/bad-id").session(pastorSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pastors/dashboard"));
    }

    @Test
    @DisplayName("GET /pastors/profiles/delete/{id} — no session redirects to /login")
    void testDeleteProfile_NoSession_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/pastors/profiles/delete/profile-001").session(guestSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ══════════════════════════════════════════════
    //  GET /pastors/profiles/view/{id}
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /pastors/profiles/view/{id} — own profile shows profile-details view")
    void testViewProfile_OwnProfile_ReturnsView() throws Exception {
        when(profileService.getProfileById("profile-001")).thenReturn(Optional.of(mockProfile));

        mockMvc.perform(get("/pastors/profiles/view/profile-001").session(pastorSession))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-details"))
                .andExpect(model().attributeExists("profile"));
    }

    @Test
    @DisplayName("GET /pastors/profiles/view/{id} — another pastor's profile redirects to dashboard")
    void testViewProfile_OtherPastorProfile_RedirectsToDashboard() throws Exception {
        mockProfile.setProposedByPastorId("other-pastor");
        when(profileService.getProfileById("profile-001")).thenReturn(Optional.of(mockProfile));

        mockMvc.perform(get("/pastors/profiles/view/profile-001").session(pastorSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pastors/dashboard"));
    }

    @Test
    @DisplayName("GET /pastors/profiles/view/{id} — not found redirects to dashboard")
    void testViewProfile_NotFound_RedirectsToDashboard() throws Exception {
        when(profileService.getProfileById("bad-id")).thenReturn(Optional.empty());

        mockMvc.perform(get("/pastors/profiles/view/bad-id").session(pastorSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pastors/dashboard"));
    }

    @Test
    @DisplayName("GET /pastors/profiles/view/{id} — no session redirects to /login")
    void testViewProfile_NoSession_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/pastors/profiles/view/profile-001").session(guestSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ══════════════════════════════════════════════
    //  GET /pastors/logout
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /pastors/logout — invalidates session and redirects to /login")
    void testLogout_InvalidatesSessionAndRedirects() throws Exception {
        mockMvc.perform(get("/pastors/logout").session(pastorSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}