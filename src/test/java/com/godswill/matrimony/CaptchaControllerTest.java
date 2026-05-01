package com.godswill.matrimony;

import com.godswill.matrimony.controller.CaptchaController;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CaptchaController Tests")
class CaptchaControllerTest {

    @Mock
    private DefaultKaptcha captchaProducer;

    @InjectMocks
    private CaptchaController captchaController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(captchaController).build();
    }

    @Test
    @DisplayName("GET /captcha-image — returns image/jpeg content type")
    void testGetCaptcha_ReturnsJpegContentType() throws Exception {
        String captchaText = "AB12";
        BufferedImage mockImage = new BufferedImage(200, 50, BufferedImage.TYPE_INT_RGB);

        when(captchaProducer.createText()).thenReturn(captchaText);
        when(captchaProducer.createImage(captchaText)).thenReturn(mockImage);

        mockMvc.perform(get("/captcha-image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));
    }

    @Test
    @DisplayName("GET /captcha-image — stores generated text in session")
    void testGetCaptcha_StoresCaptchaInSession() throws Exception {
        String captchaText = "XY34";
        BufferedImage mockImage = new BufferedImage(200, 50, BufferedImage.TYPE_INT_RGB);

        when(captchaProducer.createText()).thenReturn(captchaText);
        when(captchaProducer.createImage(captchaText)).thenReturn(mockImage);

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/captcha-image").session(session))
                .andExpect(status().isOk());

        assertEquals(captchaText, session.getAttribute("captcha"));
    }

    @Test
    @DisplayName("GET /captcha-image — sets no-cache headers")
    void testGetCaptcha_SetsNoCacheHeaders() throws Exception {
        String captchaText = "ZZ99";
        BufferedImage mockImage = new BufferedImage(200, 50, BufferedImage.TYPE_INT_RGB);

        when(captchaProducer.createText()).thenReturn(captchaText);
        when(captchaProducer.createImage(captchaText)).thenReturn(mockImage);

        mockMvc.perform(get("/captcha-image"))
                .andExpect(status().isOk())
                .andExpect(header().string("Pragma", "no-cache"));
    }

    @Test
    @DisplayName("GET /captcha-image — calls captchaProducer to create text and image")
    void testGetCaptcha_InvokesCaptchaProducer() throws Exception {
        String captchaText = "QR56";
        BufferedImage mockImage = new BufferedImage(200, 50, BufferedImage.TYPE_INT_RGB);

        when(captchaProducer.createText()).thenReturn(captchaText);
        when(captchaProducer.createImage(captchaText)).thenReturn(mockImage);

        mockMvc.perform(get("/captcha-image")).andExpect(status().isOk());

        verify(captchaProducer, times(1)).createText();
        verify(captchaProducer, times(1)).createImage(captchaText);
    }

    @Test
    @DisplayName("GET /captcha-image — each call generates unique CAPTCHA text")
    void testGetCaptcha_GeneratesUniqueText() throws Exception {
        BufferedImage mockImage = new BufferedImage(200, 50, BufferedImage.TYPE_INT_RGB);

        when(captchaProducer.createText()).thenReturn("AAAA", "BBBB");
        when(captchaProducer.createImage(anyString())).thenReturn(mockImage);

        MockHttpSession session1 = new MockHttpSession();
        MockHttpSession session2 = new MockHttpSession();

        mockMvc.perform(get("/captcha-image").session(session1)).andExpect(status().isOk());
        mockMvc.perform(get("/captcha-image").session(session2)).andExpect(status().isOk());

        assertNotEquals(session1.getAttribute("captcha"), session2.getAttribute("captcha"));
    }

    @Test
    @DisplayName("GET /captcha-image — response body is non-empty image bytes")
    void testGetCaptcha_ResponseBodyNotEmpty() throws Exception {
        String captchaText = "MN78";
        BufferedImage mockImage = new BufferedImage(200, 50, BufferedImage.TYPE_INT_RGB);

        when(captchaProducer.createText()).thenReturn(captchaText);
        when(captchaProducer.createImage(captchaText)).thenReturn(mockImage);

        byte[] responseBody = mockMvc.perform(get("/captcha-image"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertTrue(responseBody.length > 0);
    }
}