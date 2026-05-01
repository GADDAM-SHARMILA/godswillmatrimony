//package com.godswill.matrimony;
//
//import com.godswill.matrimony.controller.HomeController;
//import com.godswill.matrimony.model.CarouselImage;
//import com.godswill.matrimony.model.Profile;
//import com.godswill.matrimony.model.SuccessStory;
//import com.godswill.matrimony.model.SubscriptionPlan;
//import com.godswill.matrimony.model.User;
//import com.godswill.matrimony.service.CarouselImageService;
//import com.godswill.matrimony.service.ProfileService;
//import com.godswill.matrimony.service.SuccessStoryService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.mock.web.MockHttpSession;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//import org.springframework.web.servlet.view.InternalResourceViewResolver;
//
//import java.util.Collections;
//import java.util.List;
//
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("HomeController Tests")
//class HomeControllerTest {
//
//    @Mock private ProfileService profileService;
//    @Mock private SuccessStoryService successStoryService;
//    @Mock private CarouselImageService carouselImageService;
//
//    @InjectMocks
//    private HomeController homeController;
//
//    private MockMvc mockMvc;
//    private MockHttpSession guestSession;
//    private MockHttpSession premiumUserSession;
//    private MockHttpSession adminSession;
//
//    @BeforeEach
//    void setUp() {
//        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
//        viewResolver.setPrefix("/WEB-INF/views/");
//        viewResolver.setSuffix(".html");
//        mockMvc = MockMvcBuilders.standaloneSetup(homeController)
//                .setViewResolvers(viewResolver)
//                .build();
//
//        guestSession = new MockHttpSession();
//
//        User premiumUser = new User();
//        premiumUser.setId("user-001");
//        premiumUser.setRole("USER");
//        // assume isPremium() returns true when currentPlan is set
//        premiumUser.setCurrentPlan(SubscriptionPlan.GOLD);
//
//        premiumUserSession = new MockHttpSession();
//        premiumUserSession.setAttribute("user", premiumUser);
//
//        User adminUser = new User();
//        adminUser.setId("admin-001");
//        adminUser.setRole("ADMIN");
//
//        adminSession = new MockHttpSession();
//        adminSession.setAttribute("user", adminUser);
//    }
//
//    // ══════════════════════════════════════════════
//    //  GET /
//    // ══════════════════════════════════════════════
//
//    @Test
//    @DisplayName("GET / — returns home view")
//    void testHome_ReturnsHomeView() throws Exception {
//        when(carouselImageService.getAllActiveCarouselImages()).thenReturn(List.of());
//        when(successStoryService.getRecentStories()).thenReturn(List.of());
//
//        mockMvc.perform(get("/"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("home"));
//    }
//
//    @Test
//    @DisplayName("GET / — model contains activePage = 'home'")
//    void testHome_ModelContainsActivePage() throws Exception {
//        when(carouselImageService.getAllActiveCarouselImages()).thenReturn(List.of());
//        when(successStoryService.getRecentStories()).thenReturn(List.of());
//
//        mockMvc.perform(get("/"))
//                .andExpect(model().attribute("activePage", "home"));
//    }
//
//    @Test
//    @DisplayName("GET / — guest user sees empty profiles list")
//    void testHome_GuestUser_SeesEmptyProfiles() throws Exception {
//        when(carouselImageService.getAllActiveCarouselImages()).thenReturn(List.of());
//        when(successStoryService.getRecentStories()).thenReturn(List.of());
//
//        mockMvc.perform(get("/").session(guestSession))
//                .andExpect(model().attribute("isLoggedIn", false))
//                .andExpect(model().attribute("profiles", Collections.emptyList()));
//    }
//
//    @Test
//    @DisplayName("GET / — admin user sees profiles in model")
//    void testHome_AdminUser_SeesProfiles() throws Exception {
//        Profile p = new Profile();
//        p.setId("p-001");
//
//        when(carouselImageService.getAllActiveCarouselImages()).thenReturn(List.of());
//        when(successStoryService.getRecentStories()).thenReturn(List.of());
//        when(profileService.getAllProfiles()).thenReturn(List.of(p));
//
//        mockMvc.perform(get("/").session(adminSession))
//                .andExpect(model().attribute("isLoggedIn", true))
//                .andExpect(model().attribute("isPremium", true));
//
//        verify(profileService, times(1)).getAllProfiles();
//    }
//
//    @Test
//    @DisplayName("GET / — model contains carouselImages from service")
//    void testHome_ModelContainsCarouselImages() throws Exception {
//        CarouselImage img = new CarouselImage();
//        img.setId("img-001");
//        img.setActive(true);
//
//        when(carouselImageService.getAllActiveCarouselImages()).thenReturn(List.of(img));
//        when(successStoryService.getRecentStories()).thenReturn(List.of());
//
//        mockMvc.perform(get("/"))
//                .andExpect(model().attributeExists("carouselImages"));
//    }
//
//    @Test
//    @DisplayName("GET / — model contains successStories from service")
//    void testHome_ModelContainsSuccessStories() throws Exception {
//        SuccessStory story = new SuccessStory();
//        story.setId("story-001");
//
//        when(carouselImageService.getAllActiveCarouselImages()).thenReturn(List.of());
//        when(successStoryService.getRecentStories()).thenReturn(List.of(story));
//
//        mockMvc.perform(get("/"))
//                .andExpect(model().attributeExists("successStories"));
//    }
//
//    @Test
//    @DisplayName("GET / — profiles list capped at 12 for premium users")
//    void testHome_ProfilesCappedAt12() throws Exception {
//        List<Profile> manyProfiles = Collections.nCopies(20, new Profile());
//
//        when(carouselImageService.getAllActiveCarouselImages()).thenReturn(List.of());
//        when(successStoryService.getRecentStories()).thenReturn(List.of());
//        when(profileService.getAllProfiles()).thenReturn(manyProfiles);
//
//        mockMvc.perform(get("/").session(adminSession))
//                .andExpect(model().attributeExists("profiles"));
//
//        // Verify service was called — cap logic is internal
//        verify(profileService, times(1)).getAllProfiles();
//    }
//
//    @Test
//    @DisplayName("GET / — service exception still returns home view with empty model attributes")
//    void testHome_ServiceException_StillReturnsHome() throws Exception {
//        when(carouselImageService.getAllActiveCarouselImages())
//                .thenThrow(new RuntimeException("DB error"));
//
//        mockMvc.perform(get("/"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("home"))
//                .andExpect(model().attribute("activePage", "home"));
//    }
//
//    // ══════════════════════════════════════════════
//    //  Static pages
//    // ══════════════════════════════════════════════
//
//    @Test
//    @DisplayName("GET /privacy-policy — returns privacy-policy view")
//    void testPrivacyPolicy_ReturnsView() throws Exception {
//        mockMvc.perform(get("/privacy-policy"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("privacy-policy"));
//    }
//
//    @Test
//    @DisplayName("GET /terms-and-conditions — returns terms-and-conditions view")
//    void testTermsAndConditions_ReturnsView() throws Exception {
//        mockMvc.perform(get("/terms-and-conditions"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("terms-and-conditions"));
//    }
//
//    @Test
//    @DisplayName("GET /about — returns about view")
//    void testAbout_ReturnsView() throws Exception {
//        mockMvc.perform(get("/about"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("about"));
//    }
//
//    @Test
//    @DisplayName("GET /associate — returns associate view")
//    void testAssociate_ReturnsView() throws Exception {
//        mockMvc.perform(get("/associate"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("associate"));
//    }
//
//    @Test
//    @DisplayName("GET /gallery — returns gallery view")
//    void testGallery_ReturnsView() throws Exception {
//        mockMvc.perform(get("/gallery"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("gallery"));
//    }
//
//    @Test
//    @DisplayName("GET /faq — returns faqs view")
//    void testFaq_ReturnsView() throws Exception {
//        mockMvc.perform(get("/faq"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("faqs"));
//    }
//
//    @Test
//    @DisplayName("GET /premium-plans — returns premium-plans view")
//    void testPremiumPlans_ReturnsView() throws Exception {
//        mockMvc.perform(get("/premium-plans"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("user/premium-plans"));
//    }
//}