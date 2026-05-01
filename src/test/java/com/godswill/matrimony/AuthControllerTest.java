package com.godswill.matrimony;

import com.godswill.matrimony.controller.AuthController;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.service.EmailService;
import com.godswill.matrimony.service.OtpService;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Mock private UserService userService;
    @Mock private OtpService otpService;
    @Mock private EmailService emailService;
    @Mock private ProfileService profileService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private MockHttpSession session;
    private User mockUser;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setViewResolvers(viewResolver)
                .build();

        mockUser = new User();
        mockUser.setId("user-001");
        mockUser.setFirstName("John");
        mockUser.setEmail("john@test.com");
        mockUser.setPhone("9876543210");
        mockUser.setPassword("encoded-pass");
        mockUser.setEmailVerified(true);
        mockUser.setRole("USER");

        session = new MockHttpSession();
    }

    // ══════════════════════════════════════════════
    //  GET /register
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /register — returns register view with empty user model")
    void testShowRegisterForm_ReturnsView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("user"));
    }

    // ══════════════════════════════════════════════
    //  POST /register
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("POST /register — invalid CAPTCHA redirects with error")
    void testRegister_InvalidCaptcha_RedirectsWithError() throws Exception {
        session.setAttribute("captcha", "ABCD");

        mockMvc.perform(post("/register").session(session)
                        .param("email", "test@test.com")
                        .param("phone", "9876543210")
                        .param("password", "pass123")
                        .param("confirmPassword", "pass123")
                        .param("captcha", "WRONG"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"));
    }

    @Test
    @DisplayName("POST /register — CAPTCHA case-insensitive match succeeds")
    void testRegister_CaptchaCaseInsensitive_Passes() throws Exception {
        session.setAttribute("captcha", "abcd");

        when(userService.existsByEmail(anyString())).thenReturn(false);
        when(userService.existsByPhone(anyString())).thenReturn(false);
        when(userService.registerUser(any(User.class))).thenReturn(mockUser);

        mockMvc.perform(post("/register").session(session)
                        .param("firstName", "John")
                        .param("lastName", "Doe")
                        .param("email", "john@test.com")
                        .param("phone", "9876543210")
                        .param("password", "pass123")
                        .param("confirmPassword", "pass123")
                        .param("captcha", "ABCD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/verify-otp"));
    }

    @Test
    @DisplayName("POST /register — passwords do not match redirects with error")
    void testRegister_PasswordMismatch_RedirectsWithError() throws Exception {
        session.setAttribute("captcha", "ABCD");

        mockMvc.perform(post("/register").session(session)
                        .param("email", "john@test.com")
                        .param("phone", "9876543210")
                        .param("password", "pass123")
                        .param("confirmPassword", "different")
                        .param("captcha", "ABCD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"));
    }

    @Test
    @DisplayName("POST /register — duplicate email redirects with error")
    void testRegister_DuplicateEmail_RedirectsWithError() throws Exception {
        session.setAttribute("captcha", "ABCD");

        when(userService.existsByEmail("john@test.com")).thenReturn(true);

        mockMvc.perform(post("/register").session(session)
                        .param("email", "john@test.com")
                        .param("phone", "9876543210")
                        .param("password", "pass123")
                        .param("confirmPassword", "pass123")
                        .param("captcha", "ABCD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"));
    }

    @Test
    @DisplayName("POST /register — duplicate phone redirects with error")
    void testRegister_DuplicatePhone_RedirectsWithError() throws Exception {
        session.setAttribute("captcha", "ABCD");

        when(userService.existsByEmail(anyString())).thenReturn(false);
        when(userService.existsByPhone("9876543210")).thenReturn(true);

        mockMvc.perform(post("/register").session(session)
                        .param("email", "john@test.com")
                        .param("phone", "9876543210")
                        .param("password", "pass123")
                        .param("confirmPassword", "pass123")
                        .param("captcha", "ABCD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"));
    }

    @Test
    @DisplayName("POST /register — valid data saves user and redirects to /verify-otp")
    void testRegister_ValidData_RedirectsToVerifyOtp() throws Exception {
        session.setAttribute("captcha", "ABCD");

        when(userService.existsByEmail(anyString())).thenReturn(false);
        when(userService.existsByPhone(anyString())).thenReturn(false);
        when(userService.registerUser(any(User.class))).thenReturn(mockUser);

        mockMvc.perform(post("/register").session(session)
                        .param("firstName", "John")
                        .param("lastName", "Doe")
                        .param("email", "john@test.com")
                        .param("phone", "9876543210")
                        .param("password", "pass123")
                        .param("confirmPassword", "pass123")
                        .param("captcha", "ABCD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/verify-otp"));

        verify(userService, times(1)).registerUser(any(User.class));
        verify(otpService, times(1)).sendEmailOtp(mockUser.getEmail());
    }

    // ══════════════════════════════════════════════
    //  GET /verify-otp
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /verify-otp — with valid session shows otp page")
    void testShowVerifyOtp_WithValidSession_ReturnsView() throws Exception {
        session.setAttribute("otpUserId", "user-001");
        session.setAttribute("otpEmail", "john@test.com");

        mockMvc.perform(get("/verify-otp").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("verify-otp"))
                .andExpect(model().attribute("userId", "user-001"))
                .andExpect(model().attribute("email", "john@test.com"));
    }

    @Test
    @DisplayName("GET /verify-otp — no session data redirects to /register")
    void testShowVerifyOtp_NoSession_RedirectsToRegister() throws Exception {
        mockMvc.perform(get("/verify-otp"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"));
    }

    // ══════════════════════════════════════════════
    //  POST /verify-otp
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("POST /verify-otp — valid OTP marks email verified and redirects to /login")
    void testVerifyOtp_ValidOtp_RedirectsToLogin() throws Exception {
        session.setAttribute("otpUserId", "user-001");
        session.setAttribute("otpEmail", "john@test.com");

        when(otpService.verifyEmailOtp("john@test.com", "123456")).thenReturn(true);
        when(userService.findById("user-001")).thenReturn(Optional.of(mockUser));

        mockMvc.perform(post("/verify-otp").session(session)
                        .param("emailOtp", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(userService, times(1)).markEmailVerified("user-001");
    }

    @Test
    @DisplayName("POST /verify-otp — invalid OTP redirects back to /verify-otp")
    void testVerifyOtp_InvalidOtp_RedirectsBackToVerifyOtp() throws Exception {
        session.setAttribute("otpUserId", "user-001");
        session.setAttribute("otpEmail", "john@test.com");

        when(otpService.verifyEmailOtp("john@test.com", "000000")).thenReturn(false);

        mockMvc.perform(post("/verify-otp").session(session)
                        .param("emailOtp", "000000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/verify-otp"));

        verify(userService, never()).markEmailVerified(anyString());
    }

    @Test
    @DisplayName("POST /verify-otp — expired session redirects to /register")
    void testVerifyOtp_ExpiredSession_RedirectsToRegister() throws Exception {
        mockMvc.perform(post("/verify-otp")
                        .param("emailOtp", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"));
    }

    // ══════════════════════════════════════════════
    //  POST /resend-otp
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("POST /resend-otp — valid email returns 200 with success status")
    void testResendOtp_ValidEmail_ReturnsSuccess() throws Exception {
        doNothing().when(otpService).resendEmailOtp("john@test.com");

        mockMvc.perform(post("/resend-otp")
                        .param("email", "john@test.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /resend-otp — service throws exception returns 400")
    void testResendOtp_ServiceThrows_ReturnsBadRequest() throws Exception {
        doThrow(new RuntimeException("User not found"))
                .when(otpService).resendEmailOtp("bad@test.com");

        mockMvc.perform(post("/resend-otp")
                        .param("email", "bad@test.com"))
                .andExpect(status().isBadRequest());
    }

    // ══════════════════════════════════════════════
    //  GET /login
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /login — returns login view")
    void testShowLoginForm_ReturnsView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    // ══════════════════════════════════════════════
    //  POST /login
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("POST /login — invalid CAPTCHA redirects with error")
    void testLogin_InvalidCaptcha_RedirectsWithError() throws Exception {
        session.setAttribute("captcha", "ABCD");

        mockMvc.perform(post("/login").session(session)
                        .param("email", "john@test.com")
                        .param("password", "pass123")
                        .param("captcha", "WRONG"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("POST /login — valid credentials for normal user redirects to /")
    void testLogin_ValidUser_RedirectsToHome() throws Exception {
        session.setAttribute("captcha", "ABCD");

        when(userService.login("john@test.com", "pass123")).thenReturn(Optional.of(mockUser));

        mockMvc.perform(post("/login").session(session)
                        .param("email", "john@test.com")
                        .param("password", "pass123")
                        .param("captcha", "ABCD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("POST /login — valid admin credentials redirects to /admin")
    void testLogin_AdminUser_RedirectsToAdmin() throws Exception {
        session.setAttribute("captcha", "ABCD");

        User adminUser = new User();
        adminUser.setRole("ADMIN");
        adminUser.setEmailVerified(true);

        when(userService.login("admin@test.com", "admin123")).thenReturn(Optional.of(adminUser));

        mockMvc.perform(post("/login").session(session)
                        .param("email", "admin@test.com")
                        .param("password", "admin123")
                        .param("captcha", "ABCD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    @DisplayName("POST /login — unverified email redirects with error")
    void testLogin_UnverifiedEmail_RedirectsWithError() throws Exception {
        session.setAttribute("captcha", "ABCD");

        mockUser.setEmailVerified(false);
        when(userService.login("john@test.com", "pass123")).thenReturn(Optional.of(mockUser));

        mockMvc.perform(post("/login").session(session)
                        .param("email", "john@test.com")
                        .param("password", "pass123")
                        .param("captcha", "ABCD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("POST /login — invalid credentials redirects with error")
    void testLogin_InvalidCredentials_RedirectsWithError() throws Exception {
        session.setAttribute("captcha", "ABCD");

        when(userService.login("john@test.com", "wrong")).thenReturn(Optional.empty());

        mockMvc.perform(post("/login").session(session)
                        .param("email", "john@test.com")
                        .param("password", "wrong")
                        .param("captcha", "ABCD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ══════════════════════════════════════════════
    //  GET /forgot-password
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /forgot-password — returns forgot-password view")
    void testShowForgotPassword_ReturnsView() throws Exception {
        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("forgotpassword/forgot-password"));
    }

    // ══════════════════════════════════════════════
    //  POST /forgot-password
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("POST /forgot-password — known email sends reset link and redirects")
    void testProcessForgotPassword_KnownEmail_SendsEmailAndRedirects() throws Exception {
        when(userService.findByEmail("john@test.com")).thenReturn(Optional.of(mockUser));

        mockMvc.perform(post("/forgot-password")
                        .param("email", "john@test.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"));

        verify(userService, times(1)).updateUser(any(User.class));
        verify(emailService, times(1)).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("POST /forgot-password — unknown email still redirects (no info leak)")
    void testProcessForgotPassword_UnknownEmail_RedirectsWithoutError() throws Exception {
        when(userService.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/forgot-password")
                        .param("email", "unknown@test.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"));

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    // ══════════════════════════════════════════════
    //  GET /reset-password
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /reset-password — valid token shows reset page")
    void testShowResetPassword_ValidToken_ReturnsView() throws Exception {
        mockUser.setResetToken("valid-token");
        mockUser.setResetTokenExpiry(LocalDateTime.now().plusMinutes(10));

        when(userService.findByResetToken("valid-token")).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/reset-password").param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("forgotpassword/reset-password"))
                .andExpect(model().attribute("token", "valid-token"));
    }

    @Test
    @DisplayName("GET /reset-password — invalid token redirects to /forgot-password")
    void testShowResetPassword_InvalidToken_RedirectsToForgotPassword() throws Exception {
        when(userService.findByResetToken("bad-token")).thenReturn(Optional.empty());

        mockMvc.perform(get("/reset-password").param("token", "bad-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"));
    }

    @Test
    @DisplayName("GET /reset-password — expired token redirects to /forgot-password")
    void testShowResetPassword_ExpiredToken_RedirectsToForgotPassword() throws Exception {
        mockUser.setResetToken("expired-token");
        mockUser.setResetTokenExpiry(LocalDateTime.now().minusMinutes(1));

        when(userService.findByResetToken("expired-token")).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/reset-password").param("token", "expired-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"));
    }

    // ══════════════════════════════════════════════
    //  POST /reset-password
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("POST /reset-password — passwords mismatch redirects back")
    void testProcessResetPassword_PasswordMismatch_RedirectsBack() throws Exception {
        mockMvc.perform(post("/reset-password")
                        .param("token", "valid-token")
                        .param("password", "newpass")
                        .param("confirmPassword", "different"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reset-password?token=valid-token"));
    }

    @Test
    @DisplayName("POST /reset-password — valid data resets password and redirects to /login")
    void testProcessResetPassword_ValidData_ResetsAndRedirects() throws Exception {
        mockUser.setResetToken("valid-token");
        mockUser.setResetTokenExpiry(LocalDateTime.now().plusMinutes(10));

        when(userService.findByResetToken("valid-token")).thenReturn(Optional.of(mockUser));
        when(userService.encodePassword("newpass")).thenReturn("encoded-newpass");

        mockMvc.perform(post("/reset-password")
                        .param("token", "valid-token")
                        .param("password", "newpass")
                        .param("confirmPassword", "newpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(userService, times(1)).updateUser(any(User.class));
    }

    @Test
    @DisplayName("POST /reset-password — expired token redirects to /forgot-password")
    void testProcessResetPassword_ExpiredToken_RedirectsToForgotPassword() throws Exception {
        mockUser.setResetToken("expired-token");
        mockUser.setResetTokenExpiry(LocalDateTime.now().minusMinutes(5));

        when(userService.findByResetToken("expired-token")).thenReturn(Optional.of(mockUser));

        mockMvc.perform(post("/reset-password")
                        .param("token", "expired-token")
                        .param("password", "newpass")
                        .param("confirmPassword", "newpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"));
    }

    // ══════════════════════════════════════════════
    //  GET /logout
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /logout — invalidates session and redirects to /")
    void testLogout_InvalidatesSessionAndRedirects() throws Exception {
        session.setAttribute("user", mockUser);

        mockMvc.perform(get("/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }
}