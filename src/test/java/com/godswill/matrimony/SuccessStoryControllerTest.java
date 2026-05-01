//package com.godswill.matrimony;
//
//import com.godswill.matrimony.controller.SuccessStoryController;
//import com.godswill.matrimony.model.SuccessStory;
//import com.godswill.matrimony.service.SuccessStoryService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//import org.springframework.web.servlet.view.InternalResourceViewResolver;
//
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.isNull;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("SuccessStoryController Tests")
//class SuccessStoryControllerTest {
//
//    @Mock private SuccessStoryService successStoryService;
//
//    @InjectMocks
//    private SuccessStoryController successStoryController;
//
//    private MockMvc mockMvc;
//    private SuccessStory mockStory;
//
//    @BeforeEach
//    void setUp() {
//        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
//        viewResolver.setPrefix("/WEB-INF/views/");
//        viewResolver.setSuffix(".html");
//        mockMvc = MockMvcBuilders.standaloneSetup(successStoryController)
//                .setViewResolvers(viewResolver)
//                .build();
//
//        mockStory = new SuccessStory();
//        mockStory.setId("story-001");
//        mockStory.setImage(new byte[]{1, 2, 3});
//        mockStory.setImageType("image/jpeg");
//    }
//
//    // ══════════════════════════════════════════════
//    //  GET /success-stories
//    // ══════════════════════════════════════════════
//
//    @Test
//    @DisplayName("GET /success-stories — returns success-stories view with all stories")
//    void testSuccessStories_ReturnsView() throws Exception {
//        when(successStoryService.getAllSuccessStories()).thenReturn(List.of(mockStory));
//
//        mockMvc.perform(get("/success-stories"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("success-stories"))
//                .andExpect(model().attributeExists("successStories"))
//                .andExpect(model().attribute("activePage", "stories"));
//    }
//
//    @Test
//    @DisplayName("GET /success-stories — empty list returns view with empty model attribute")
//    void testSuccessStories_EmptyList_ReturnsView() throws Exception {
//        when(successStoryService.getAllSuccessStories()).thenReturn(List.of());
//
//        mockMvc.perform(get("/success-stories"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("success-stories"));
//    }
//
//    // ══════════════════════════════════════════════
//    //  GET /submit-story
//    // ══════════════════════════════════════════════
//
//    @Test
//    @DisplayName("GET /submit-story — returns submit-story view with empty successStory model")
//    void testSubmitStoryPage_ReturnsView() throws Exception {
//        mockMvc.perform(get("/submit-story"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("submit-story"))
//                .andExpect(model().attributeExists("successStory"));
//    }
//
//    @Test
//    @DisplayName("GET /submit-story — model contains pageTitle")
//    void testSubmitStoryPage_ModelContainsPageTitle() throws Exception {
//        mockMvc.perform(get("/submit-story"))
//                .andExpect(model().attribute("pageTitle", "Submit Story"));
//    }
//
//    // ══════════════════════════════════════════════
//    //  POST /submit-story
//    // ══════════════════════════════════════════════
//
//    @Test
//    @DisplayName("POST /submit-story — valid story without image redirects to /success-stories")
//    void testSaveStory_NoImage_RedirectsToStories() throws Exception {
//        when(successStoryService.createSuccessStory(any(SuccessStory.class), isNull()))
//                .thenReturn(mockStory);
//
//        mockMvc.perform(post("/submit-story")
//                        .param("title", "Our Love Story")
//                        .param("content", "We met on this platform..."))
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/success-stories"));
//
//        verify(successStoryService, times(1)).createSuccessStory(any(), any());
//    }
//
//    @Test
//    @DisplayName("POST /submit-story — valid story with image redirects to /success-stories")
//    void testSaveStory_WithImage_RedirectsToStories() throws Exception {
//        MockMultipartFile imageFile = new MockMultipartFile(
//                "imageFile", "photo.jpg", "image/jpeg", "imagedata".getBytes());
//
//        mockMvc.perform(multipart("/submit-story")
//                        .file(imageFile)
//                        .param("title", "Our Story")
//                        .param("content", "We found each other here..."))
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/success-stories"));
//
//        verify(successStoryService, times(1)).createSuccessStory(any(), any());
//    }
//
//    @Test
//    @DisplayName("POST /submit-story — calls createSuccessStory service method exactly once")
//    void testSaveStory_CallsServiceOnce() throws Exception {
//        mockMvc.perform(post("/submit-story")
//                        .param("title", "Test")
//                        .param("content", "Content"))
//                .andExpect(status().is3xxRedirection());
//
//        verify(successStoryService, times(1)).createSuccessStory(any(), any());
//    }
//
//    // ══════════════════════════════════════════════
//    //  GET /success-story/image/{id}
//    // ══════════════════════════════════════════════
//
//    @Test
//    @DisplayName("GET /success-story/image/{id} — found story with image returns 200 with image bytes")
//    void testGetImage_Found_ReturnsImageBytes() throws Exception {
//        // Controller calls getStoryById(id), NOT getAllSuccessStories()
//        when(successStoryService.getStoryById("story-001")).thenReturn(mockStory);
//
//        mockMvc.perform(get("/success-story/image/story-001"))
//                .andExpect(status().isOk())
//                .andExpect(header().string("Content-Type", "image/jpeg"));
//    }
//
//    @Test
//    @DisplayName("GET /success-story/image/{id} — story not found returns 404")
//    void testGetImage_StoryNotFound_Returns404() throws Exception {
//        // Controller calls getStoryById(id) which returns null when not found
//        when(successStoryService.getStoryById("bad-id")).thenReturn(null);
//
//        mockMvc.perform(get("/success-story/image/bad-id"))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    @DisplayName("GET /success-story/image/{id} — story found but image is null returns 404")
//    void testGetImage_NullImage_Returns404() throws Exception {
//        SuccessStory storyNoImage = new SuccessStory();
//        storyNoImage.setId("story-002");
//        storyNoImage.setImage(null);
//
//        // Controller calls getStoryById(id), story exists but image byte[] is null
//        when(successStoryService.getStoryById("story-002")).thenReturn(storyNoImage);
//
//        mockMvc.perform(get("/success-story/image/story-002"))
//                .andExpect(status().isNotFound());
//    }
//}