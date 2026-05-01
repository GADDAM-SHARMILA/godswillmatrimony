package com.godswill.matrimony;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godswill.matrimony.controller.CarouselImageController;
import com.godswill.matrimony.model.CarouselImage;
import com.godswill.matrimony.service.CarouselImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CarouselImageController Tests")
class CarouselImageControllerTest {

    @Mock
    private CarouselImageService carouselImageService;

    @InjectMocks
    private CarouselImageController carouselImageController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private CarouselImage mockImage;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(carouselImageController).build();
        objectMapper = new ObjectMapper();

        mockImage = new CarouselImage();
        mockImage.setId("img-001");
        mockImage.setActive(true);
    }

    // ══════════════════════════════════════════════
    //  GET /api/carousel-images
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/carousel-images — returns 200 with all images list")
    void testGetAllCarouselImages_ReturnsList() throws Exception {
        when(carouselImageService.getAllCarouselImages()).thenReturn(List.of(mockImage));

        mockMvc.perform(get("/api/carousel-images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/carousel-images — returns 200 with empty list when no images")
    void testGetAllCarouselImages_EmptyList_Returns200() throws Exception {
        when(carouselImageService.getAllCarouselImages()).thenReturn(List.of());

        mockMvc.perform(get("/api/carousel-images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ══════════════════════════════════════════════
    //  GET /api/carousel-images/active
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/carousel-images/active — returns only active images")
    void testGetActiveCarouselImages_ReturnsActiveOnly() throws Exception {
        when(carouselImageService.getAllActiveCarouselImages()).thenReturn(List.of(mockImage));

        mockMvc.perform(get("/api/carousel-images/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ══════════════════════════════════════════════
    //  GET /api/carousel-images/{id}
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/carousel-images/{id} — found image returns 200")
    void testGetCarouselImageById_Found_Returns200() throws Exception {
        when(carouselImageService.getCarouselImageById("img-001")).thenReturn(mockImage);

        mockMvc.perform(get("/api/carousel-images/img-001"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/carousel-images/{id} — not found returns 404")
    void testGetCarouselImageById_NotFound_Returns404() throws Exception {
        when(carouselImageService.getCarouselImageById("bad-id")).thenReturn(null);

        mockMvc.perform(get("/api/carousel-images/bad-id"))
                .andExpect(status().isNotFound());
    }

    // ══════════════════════════════════════════════
    //  POST /api/carousel-images
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("POST /api/carousel-images — creates image and returns 200")
    void testCreateCarouselImage_ValidBody_Returns200() throws Exception {
        when(carouselImageService.saveCarouselImage(any(CarouselImage.class))).thenReturn(mockImage);

        mockMvc.perform(post("/api/carousel-images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockImage)))
                .andExpect(status().isOk());

        verify(carouselImageService, times(1)).saveCarouselImage(any(CarouselImage.class));
    }

    // ══════════════════════════════════════════════
    //  PUT /api/carousel-images/{id}
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("PUT /api/carousel-images/{id} — existing image updates and returns 200")
    void testUpdateCarouselImage_Found_Returns200() throws Exception {
        when(carouselImageService.updateCarouselImage(eq("img-001"), any(CarouselImage.class)))
                .thenReturn(mockImage);

        mockMvc.perform(put("/api/carousel-images/img-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockImage)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/carousel-images/{id} — not found returns 404")
    void testUpdateCarouselImage_NotFound_Returns404() throws Exception {
        when(carouselImageService.updateCarouselImage(eq("bad-id"), any(CarouselImage.class)))
                .thenReturn(null);

        mockMvc.perform(put("/api/carousel-images/bad-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockImage)))
                .andExpect(status().isNotFound());
    }

    // ══════════════════════════════════════════════
    //  DELETE /api/carousel-images/{id}
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("DELETE /api/carousel-images/{id} — deletes image and returns 204")
    void testDeleteCarouselImage_Returns204() throws Exception {
        doNothing().when(carouselImageService).deleteCarouselImage("img-001");

        mockMvc.perform(delete("/api/carousel-images/img-001"))
                .andExpect(status().isNoContent());

        verify(carouselImageService, times(1)).deleteCarouselImage("img-001");
    }

    // ══════════════════════════════════════════════
    //  PATCH /api/carousel-images/{id}/toggle
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("PATCH /api/carousel-images/{id}/toggle — toggles active status and returns 200")
    void testToggleCarouselImageActive_Found_Returns200() throws Exception {
        CarouselImage toggled = new CarouselImage();
        toggled.setId("img-001");
        toggled.setActive(false);

        when(carouselImageService.toggleCarouselImageActive("img-001")).thenReturn(toggled);

        mockMvc.perform(patch("/api/carousel-images/img-001/toggle"))
                .andExpect(status().isOk());

        verify(carouselImageService, times(1)).toggleCarouselImageActive("img-001");
    }

    @Test
    @DisplayName("PATCH /api/carousel-images/{id}/toggle — not found returns 404")
    void testToggleCarouselImageActive_NotFound_Returns404() throws Exception {
        when(carouselImageService.toggleCarouselImageActive("bad-id")).thenReturn(null);

        mockMvc.perform(patch("/api/carousel-images/bad-id/toggle"))
                .andExpect(status().isNotFound());
    }
}