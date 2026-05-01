package com.godswill.matrimony;

import com.godswill.matrimony.controller.ProductController;
import com.godswill.matrimony.model.Product;
import com.godswill.matrimony.model.SubscriptionPlan;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.service.ImageStorageService;
import com.godswill.matrimony.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProductControllerTest {

    @InjectMocks
    private ProductController controller;

    @Mock private ProductService productService;
    @Mock private ImageStorageService imageStorageService;
    @Mock private Model model;
    @Mock private RedirectAttributes redirectAttributes;

    private MockHttpSession adminSession;
    private MockHttpSession guestSession;
    private Product mockProduct;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        User admin = new User();
        admin.setRole("ADMIN");

        adminSession = new MockHttpSession();
        adminSession.setAttribute("user", admin);

        guestSession = new MockHttpSession();

        mockProduct = new Product();
        mockProduct.setId("prod-001");
        mockProduct.setName("Silk Saree");
        mockProduct.setCategory("bride");
        mockProduct.setActive(true);
    }

    // ── Page views ────────────────────────────────────────────────────────────

    @Test
    void shoppingPage_returnsCorrectView() {
        String view = controller.shoppingPage(model);
        assertThat(view).isEqualTo("shopping-page");
        verify(model).addAttribute("activePage", "shopping");
    }

    @Test
    void brideProducts_returnsViewWithProducts() {
        when(productService.getBrideProducts()).thenReturn(List.of(mockProduct));
        String view = controller.brideProducts(model);
        assertThat(view).isEqualTo("bride-products");
        verify(model).addAttribute("products", List.of(mockProduct));
    }

    @Test
    void groomProducts_returnsViewWithProducts() {
        Product groomProduct = new Product();
        groomProduct.setCategory("groom");
        when(productService.getGroomProducts()).thenReturn(List.of(groomProduct));
        String view = controller.groomProducts(model);
        assertThat(view).isEqualTo("groom-products");
        verify(model).addAttribute("products", List.of(groomProduct));
    }

    @Test
    void cartPage_loggedInPremiumUser_addsIsPremiumTrue() {
        User premiumUser = new User();
        premiumUser.setId("user-001");
        premiumUser.setCurrentPlan(SubscriptionPlan.GOLD);
        premiumUser.setSubscriptionExpiryDate(LocalDateTime.now().plusDays(10));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", premiumUser);

        String view = controller.cartPage(model, session);

        assertThat(view).isEqualTo("cart");
        verify(model).addAttribute("isPremium", true);
        verify(model).addAttribute("userId", "user-001");
    }

    @Test
    void cartPage_guestUser_addsIsPremiumFalse() {
        String view = controller.cartPage(model, guestSession);

        assertThat(view).isEqualTo("cart");
        verify(model).addAttribute("isPremium", false);
        verify(model).addAttribute("userId", null);
    }

    // ── Admin page views ──────────────────────────────────────────────────────

    @Test
    void addProductPage_asAdmin_returnsAdminView() {
        String view = controller.addProductPage(model, adminSession);
        assertThat(view).isEqualTo("admin-add-product");
    }

    @Test
    void addProductPage_notAdmin_redirectsToLogin() {
        String view = controller.addProductPage(model, guestSession);
        assertThat(view).contains("redirect:/login");
    }

    @Test
    void editProductPage_asAdmin_productFound_addsToModel() {
        when(productService.getProductById("prod-001")).thenReturn(Optional.of(mockProduct));

        String view = controller.editProductPage("prod-001", model, adminSession);

        assertThat(view).isEqualTo("admin-add-product");
        verify(model).addAttribute("product", mockProduct);
    }

    @Test
    void editProductPage_asAdmin_productNotFound_addsError() {
        when(productService.getProductById("bad-id")).thenReturn(Optional.empty());

        String view = controller.editProductPage("bad-id", model, adminSession);

        assertThat(view).isEqualTo("admin-add-product");
        verify(model).addAttribute(eq("error"), anyString());
    }

    @Test
    void editProductPage_notAdmin_redirectsToLogin() {
        String view = controller.editProductPage("prod-001", model, guestSession);
        assertThat(view).contains("redirect:/login");
    }

    @Test
    void adminProductList_asAdmin_loadsProducts() {
        when(productService.getAllProductsAdmin()).thenReturn(List.of(mockProduct));

        String view = controller.adminProductList(model, adminSession, null, null);

        assertThat(view).isEqualTo("admin-product-list");
        verify(model).addAttribute("products", List.of(mockProduct));
    }

    @Test
    void adminProductList_notAdmin_redirectsToLogin() {
        String view = controller.adminProductList(model, guestSession, null, null);
        assertThat(view).contains("redirect:/login");
    }

    // ── saveProduct ───────────────────────────────────────────────────────────

    @Test
    void saveProduct_newProduct_createsAndRedirects() {
        when(productService.createProduct(any())).thenReturn(mockProduct);

        Map<String, String> params = buildProductParams("");
        String view = controller.saveProduct(params, null, null, null, null,
                adminSession, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/products/list");
        verify(productService).createProduct(any(Product.class));
        verify(redirectAttributes).addFlashAttribute(eq("success"), anyString());
    }

    @Test
    void saveProduct_existingProduct_updatesAndRedirects() {
        when(productService.getProductById("prod-001")).thenReturn(Optional.of(mockProduct));
        when(productService.updateProduct(eq("prod-001"), any())).thenReturn(mockProduct);

        Map<String, String> params = buildProductParams("prod-001");
        String view = controller.saveProduct(params, null, null, null, null,
                adminSession, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/products/list");
        verify(productService).updateProduct(eq("prod-001"), any(Product.class));
    }

    @Test
    void saveProduct_notAdmin_redirectsToLogin() {
        String view = controller.saveProduct(new HashMap<>(), null, null, null, null,
                guestSession, redirectAttributes);
        assertThat(view).contains("redirect:/login");
    }

    @Test
    void saveProduct_withExistingImageUrl_preservesUrl() {
        when(productService.createProduct(any())).thenReturn(mockProduct);

        Map<String, String> params = buildProductParams("");
        controller.saveProduct(params, null, null, "/uploads/old.jpg", null,
                adminSession, redirectAttributes);

        verify(productService).createProduct(argThat(p ->
                "/uploads/old.jpg".equals(p.getImageUrl())));
    }

    // ── REST API ──────────────────────────────────────────────────────────────

    @Test
    void getBrideProductsApi_returnsOk() {
        when(productService.getBrideProducts()).thenReturn(List.of(mockProduct));

        ResponseEntity<List<Product>> response = controller.getBrideProductsApi();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getGroomProductsApi_returnsOk() {
        when(productService.getGroomProducts()).thenReturn(List.of());

        ResponseEntity<List<Product>> response = controller.getGroomProductsApi();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getProductById_found_returnsOk() {
        when(productService.getProductById("prod-001")).thenReturn(Optional.of(mockProduct));

        ResponseEntity<Product> response = controller.getProductById("prod-001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockProduct);
    }

    @Test
    void getProductById_notFound_returns404() {
        when(productService.getProductById("bad-id")).thenReturn(Optional.empty());

        ResponseEntity<Product> response = controller.getProductById("bad-id");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void searchProducts_returnsMatchingProducts() {
        when(productService.searchProducts("saree")).thenReturn(List.of(mockProduct));

        ResponseEntity<List<Product>> response = controller.searchProducts("saree");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void createProductApi_asAdmin_returnsCreatedProduct() {
        when(productService.createProduct(any())).thenReturn(mockProduct);

        ResponseEntity<?> response = controller.createProduct(mockProduct, adminSession);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockProduct);
    }

    @Test
    void createProductApi_notAdmin_returns403() {
        ResponseEntity<?> response = controller.createProduct(mockProduct, guestSession);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void updateProductApi_asAdmin_returnsUpdatedProduct() {
        when(productService.updateProduct("prod-001", mockProduct)).thenReturn(mockProduct);

        ResponseEntity<?> response = controller.updateProduct("prod-001", mockProduct, adminSession);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateProductApi_notAdmin_returns403() {
        ResponseEntity<?> response = controller.updateProduct("prod-001", mockProduct, guestSession);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deleteProductApi_asAdmin_returnsOk() {
        ResponseEntity<?> response = controller.deleteProduct("prod-001", adminSession);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(productService).deleteProduct("prod-001");
    }

    @Test
    void deleteProductApi_notAdmin_returns403() {
        ResponseEntity<?> response = controller.deleteProduct("prod-001", guestSession);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(productService, never()).deleteProduct(any());
    }

    @Test
    void getAllProductsAdminApi_asAdmin_returnsAllProducts() {
        when(productService.getAllProductsAdmin()).thenReturn(List.of(mockProduct));

        ResponseEntity<?> response = controller.getAllProductsAdmin(adminSession);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getAllProductsAdminApi_notAdmin_returns403() {
        ResponseEntity<?> response = controller.getAllProductsAdmin(guestSession);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Map<String, String> buildProductParams(String id) {
        Map<String, String> params = new HashMap<>();
        params.put("id",                    id);
        params.put("name",                  "Silk Saree");
        params.put("description",           "Beautiful saree");
        params.put("brand",                 "BrandX");
        params.put("category",              "bride");
        params.put("subCategory",           "saree");
        params.put("material",              "silk");
        params.put("sizes",                 "S,M,L");
        params.put("colors",                "Red,Blue");
        params.put("tags",                  "bridal,silk");
        params.put("originalPrice",         "2000");
        params.put("discountedPrice",       "1800");
        params.put("premiumDiscountPercent","10");
        params.put("stock",                 "50");
        params.put("deliveryDays",          "5");
        return params;
    }
}