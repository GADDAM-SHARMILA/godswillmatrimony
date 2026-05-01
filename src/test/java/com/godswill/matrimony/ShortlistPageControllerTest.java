package com.godswill.matrimony;

import com.godswill.matrimony.controller.ShortlistPageController;
import com.godswill.matrimony.model.Profile;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.service.ShortlistService;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.Model;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ShortlistPageControllerTest {

    @InjectMocks
    private ShortlistPageController controller;

    @Mock private ShortlistService shortlistService;
    @Mock private Model model;

    private MockHttpSession loggedInSession;
    private MockHttpSession guestSession;
    private User testUser;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId("user-001");
        testUser.setEmail("ravi@example.com");

        loggedInSession = new MockHttpSession();
        loggedInSession.setAttribute("user", testUser);

        guestSession = new MockHttpSession();
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void getShortlistPage_notLoggedIn_redirectsToLogin() {
        String view = controller.getShortlistPage(0, 12, model, guestSession);
        assertThat(view).isEqualTo("redirect:/login");
    }

    @Test
    void getShortlistPage_loggedIn_returnsShortlistView() {
        Page<Profile> page = new PageImpl<>(List.of(), PageRequest.of(0, 12), 0);

        when(shortlistService.getUserShortlistPage(eq("ravi@example.com"), any()))
                .thenReturn(page);

        String view = controller.getShortlistPage(0, 12, model, loggedInSession);

        assertThat(view).isEqualTo("shortlist");
    }

    @Test
    void getShortlistPage_addsProfilesContentToModel() {
        Profile p = new Profile();
        p.setFirstName("Priya");
        p.setLastName("Kumari");

        Page<Profile> page = new PageImpl<>(List.of(p), PageRequest.of(0, 12), 1);

        when(shortlistService.getUserShortlistPage(eq("ravi@example.com"), any()))
                .thenReturn(page);

        controller.getShortlistPage(0, 12, model, loggedInSession);

        verify(model).addAttribute("shortlistedProfiles", List.of(p));
    }

    @Test
    void getShortlistPage_multipleProfiles_allAddedToModel() {
        Profile p1 = new Profile();
        p1.setFirstName("Priya");
        p1.setLastName("Kumari");

        Profile p2 = new Profile();
        p2.setFirstName("Anjali");
        p2.setLastName("Reddy");

        Page<Profile> page = new PageImpl<>(List.of(p1, p2), PageRequest.of(0, 12), 2);

        when(shortlistService.getUserShortlistPage(any(), any())).thenReturn(page);

        controller.getShortlistPage(0, 12, model, loggedInSession);

        verify(model).addAttribute("shortlistedProfiles", List.of(p1, p2));
        verify(model).addAttribute("totalItems", 2L);
    }

    @Test
    void getShortlistCount_loggedInUser_returnsCount() {
        when(shortlistService.getShortlistCount("ravi@example.com")).thenReturn(5L);

        ShortlistPageController.CountResponse response =
                controller.getShortlistCount(loggedInSession);

        assertThat(response.getCount()).isEqualTo(5L);
    }
}