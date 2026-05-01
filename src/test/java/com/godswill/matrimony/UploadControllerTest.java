//package com.godswill.matrimony;
//
//import com.godswill.matrimony.controller.UploadController;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.awt.image.BufferedImage;
//import java.lang.reflect.Field;
//import java.lang.reflect.Method;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("UploadController Tests")
//class UploadControllerTest {
//
//    @Mock
//    private org.springframework.data.mongodb.gridfs.GridFsTemplate gridFsTemplate;
//
//    // ══════════════════════════════════════════════
//    //  Constants
//    // ══════════════════════════════════════════════
//
//    @Test
//    @DisplayName("MAX_WIDTH constant is 800")
//    void testMaxWidthConstant() throws Exception {
//        Field field = UploadController.class.getDeclaredField("MAX_WIDTH");
//        field.setAccessible(true);
//        int maxWidth = (int) field.get(null);
//        assertEquals(800, maxWidth);
//    }
//
//    @Test
//    @DisplayName("MAX_HEIGHT constant is 800")
//    void testMaxHeightConstant() throws Exception {
//        Field field = UploadController.class.getDeclaredField("MAX_HEIGHT");
//        field.setAccessible(true);
//        int maxHeight = (int) field.get(null);
//        assertEquals(800, maxHeight);
//    }
//
//    // ══════════════════════════════════════════════
//    //  GET /uploads/{id} — ObjectId validation
//    // ══════════════════════════════════════════════
//
//    @Test
//    @DisplayName("GET /uploads/{id} — invalid ObjectId format throws IllegalArgumentException")
//    void testGetUpload_InvalidId_ThrowsIllegalArgument() {
//        assertThrows(IllegalArgumentException.class, () ->
//                new org.bson.types.ObjectId("not-a-valid-id")
//        );
//    }
//
//    @Test
//    @DisplayName("GET /uploads/{id} — valid 24-char hex ObjectId is accepted")
//    void testGetUpload_ValidObjectId_IsAccepted() {
//        assertDoesNotThrow(() ->
//                new org.bson.types.ObjectId("507f1f77bcf86cd799439011")
//        );
//    }
//
//    // ══════════════════════════════════════════════
//    //  resizeIfNeeded
//    // ══════════════════════════════════════════════
//
//    @Test
//    @DisplayName("resizeIfNeeded — image within 800x800 bounds returns same instance")
//    void testResizeIfNeeded_SmallImage_ReturnsSameInstance() throws Exception {
//        UploadController controller = new UploadController(null);
//        Method method = UploadController.class
//                .getDeclaredMethod("resizeIfNeeded", BufferedImage.class);
//        method.setAccessible(true);
//
//        BufferedImage small = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
//        BufferedImage result = (BufferedImage) method.invoke(controller, small);
//
//        assertSame(small, result, "Small image should not be resized");
//    }
//
//    @Test
//    @DisplayName("resizeIfNeeded — image exceeding MAX_WIDTH is scaled down")
//    void testResizeIfNeeded_WideImage_IsScaledDown() throws Exception {
//        UploadController controller = new UploadController(null);
//        Method method = UploadController.class
//                .getDeclaredMethod("resizeIfNeeded", BufferedImage.class);
//        method.setAccessible(true);
//
//        BufferedImage wide = new BufferedImage(1600, 400, BufferedImage.TYPE_INT_RGB);
//        BufferedImage result = (BufferedImage) method.invoke(controller, wide);
//
//        assertTrue(result.getWidth() <= 800, "Width should be within MAX_WIDTH");
//    }
//
//    @Test
//    @DisplayName("resizeIfNeeded — image exceeding MAX_HEIGHT is scaled down")
//    void testResizeIfNeeded_TallImage_IsScaledDown() throws Exception {
//        UploadController controller = new UploadController(null);
//        Method method = UploadController.class
//                .getDeclaredMethod("resizeIfNeeded", BufferedImage.class);
//        method.setAccessible(true);
//
//        BufferedImage tall = new BufferedImage(400, 1600, BufferedImage.TYPE_INT_RGB);
//        BufferedImage result = (BufferedImage) method.invoke(controller, tall);
//
//        assertTrue(result.getHeight() <= 800, "Height should be within MAX_HEIGHT");
//    }
//
//    @Test
//    @DisplayName("resizeIfNeeded — aspect ratio is preserved after resize")
//    void testResizeIfNeeded_AspectRatioPreserved() throws Exception {
//        UploadController controller = new UploadController(null);
//        Method method = UploadController.class
//                .getDeclaredMethod("resizeIfNeeded", BufferedImage.class);
//        method.setAccessible(true);
//
//        // 1600x800 — aspect ratio 2:1
//        BufferedImage img = new BufferedImage(1600, 800, BufferedImage.TYPE_INT_RGB);
//        BufferedImage result = (BufferedImage) method.invoke(controller, img);
//
//        double originalRatio = 1600.0 / 800.0;
//        double resultRatio = (double) result.getWidth() / result.getHeight();
//
//        assertEquals(originalRatio, resultRatio, 0.01, "Aspect ratio should be preserved");
//    }
//
//    // ══════════════════════════════════════════════
//    //  toRGB
//    // ══════════════════════════════════════════════
//
//    @Test
//    @DisplayName("toRGB — ARGB image is converted to TYPE_INT_RGB")
//    void testToRGB_ArgbImage_ConvertsToRgb() throws Exception {
//        UploadController controller = new UploadController(null);
//        Method method = UploadController.class
//                .getDeclaredMethod("toRGB", BufferedImage.class);
//        method.setAccessible(true);
//
//        BufferedImage argb = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
//        BufferedImage result = (BufferedImage) method.invoke(controller, argb);
//
//        assertEquals(BufferedImage.TYPE_INT_RGB, result.getType());
//    }
//
//    @Test
//    @DisplayName("toRGB — already RGB image is returned as-is")
//    void testToRGB_RgbImage_ReturnsSameInstance() throws Exception {
//        UploadController controller = new UploadController(null);
//        Method method = UploadController.class
//                .getDeclaredMethod("toRGB", BufferedImage.class);
//        method.setAccessible(true);
//
//        BufferedImage rgb = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
//        BufferedImage result = (BufferedImage) method.invoke(controller, rgb);
//
//        assertSame(rgb, result, "Already-RGB image should not be re-converted");
//    }
//
//    @Test
//    @DisplayName("toRGB — dimensions are preserved after conversion")
//    void testToRGB_DimensionsPreserved() throws Exception {
//        UploadController controller = new UploadController(null);
//        Method method = UploadController.class
//                .getDeclaredMethod("toRGB", BufferedImage.class);
//        method.setAccessible(true);
//
//        BufferedImage argb = new BufferedImage(320, 240, BufferedImage.TYPE_INT_ARGB);
//        BufferedImage result = (BufferedImage) method.invoke(controller, argb);
//
//        assertEquals(320, result.getWidth());
//        assertEquals(240, result.getHeight());
//    }
//
//    // ══════════════════════════════════════════════
//    //  resizeAndCompress
//    // ══════════════════════════════════════════════
//
//    @Test
//    @DisplayName("resizeAndCompress — invalid bytes (non-image) returns original input")
//    void testResizeAndCompress_InvalidBytes_ReturnsOriginal() throws Exception {
//        UploadController controller = new UploadController(null);
//        Method method = UploadController.class
//                .getDeclaredMethod("resizeAndCompress", byte[].class, String.class);
//        method.setAccessible(true);
//
//        byte[] input = new byte[]{1, 2, 3};
//        byte[] result = (byte[]) method.invoke(controller, input, "image/jpeg");
//
//        assertSame(input, result, "Invalid image bytes should fall back to original");
//    }
//
//    @Test
//    @DisplayName("resizeAndCompress — empty byte array returns original input")
//    void testResizeAndCompress_EmptyBytes_ReturnsOriginal() throws Exception {
//        UploadController controller = new UploadController(null);
//        Method method = UploadController.class
//                .getDeclaredMethod("resizeAndCompress", byte[].class, String.class);
//        method.setAccessible(true);
//
//        byte[] input = new byte[0];
//        byte[] result = (byte[]) method.invoke(controller, input, "image/jpeg");
//
//        assertSame(input, result);
//    }
//}