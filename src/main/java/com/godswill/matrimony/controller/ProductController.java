package com.godswill.matrimony.controller;

import com.godswill.matrimony.model.Product;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.service.ImageStorageService;
import com.godswill.matrimony.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ImageStorageService imageStorageService;

    private boolean isAdmin(HttpSession session) {
        User u = (User) session.getAttribute("user");
        return u != null && "ADMIN".equalsIgnoreCase(u.getRole());
    }

    /**
     * ✅ UPDATED: Convert base64 directly to S3 URL
     */
    private String saveBase64Image(String base64DataUrl) throws Exception {
        if (base64DataUrl == null || base64DataUrl.isBlank()) {
            return "";
        }

        String[] parts = base64DataUrl.split(",", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid base64 image format");
        }

        String metadata = parts[0];
        byte[] imageBytes = java.util.Base64.getDecoder().decode(parts[1]);
        String mimeType = metadata.split(":")[1].split(";")[0];
        String extension = mimeType.split("/")[1];
        String filename = "product." + extension;

        // ✅ Direct S3 upload
        return imageStorageService.saveBytes(imageBytes, filename, mimeType);
    }

    @GetMapping("/shopping-page")
    public String shoppingPage(Model model) {
        model.addAttribute("activePage", "shopping");
        return "shopping-page";
    }

    @GetMapping("/bride-products")
    public String brideProducts(Model model) {
        model.addAttribute("activePage", "shopping");
        model.addAttribute("products", productService.getBrideProducts());
        return "bride-products";
    }

    @GetMapping("/groom-products")
    public String groomProducts(Model model) {
        model.addAttribute("activePage", "shopping");
        model.addAttribute("products", productService.getGroomProducts());
        return "groom-products";
    }

    @GetMapping("/cart")
    public String cartPage(Model model, HttpSession session) {
        model.addAttribute("activePage", "shopping");
        User user = (User) session.getAttribute("user");
        if (user != null) {
            boolean isPremium = user.isPremium();
            model.addAttribute("isPremium", isPremium);
            model.addAttribute("planName", isPremium && user.getCurrentPlan() != null
                    ? user.getCurrentPlan().name() : "");
            model.addAttribute("userId", user.getId());
        } else {
            model.addAttribute("isPremium", false);
            model.addAttribute("planName", "");
            model.addAttribute("userId", null);
        }
        return "cart";
    }

    @GetMapping("/admin/products/add")
    public String addProductPage(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login?error=Access+denied";
        model.addAttribute("activePage", "admin");
        return "admin-add-product";
    }

    @GetMapping("/admin/products/edit/{id}")
    public String editProductPage(@PathVariable String id, Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login?error=Access+denied";
        model.addAttribute("activePage", "admin");
        productService.getProductById(id)
                .ifPresentOrElse(
                        product -> model.addAttribute("product", product),
                        () -> model.addAttribute("error", "Product not found with id: " + id)
                );
        return "admin-add-product";
    }

    @GetMapping("/admin/products/list")
    public String adminProductList(Model model, HttpSession session,
                                   @RequestParam(required = false) String success,
                                   @RequestParam(required = false) String error) {
        if (!isAdmin(session)) return "redirect:/login?error=Access+denied";
        model.addAttribute("activePage", "admin");
        model.addAttribute("products", productService.getAllProductsAdmin());
        if (success != null) model.addAttribute("success", success);
        if (error != null) model.addAttribute("error", error);
        return "admin-product-list";
    }

    /**
     * ✅ UPDATED: Handle S3 images
     */
    @PostMapping("/admin/products/save")
    public String saveProduct(
            @RequestParam Map<String, String> params,
            @RequestParam(value = "imageData", required = false) String imageData,
            @RequestParam(value = "imageData2", required = false) String imageData2,
            @RequestParam(value = "existingImageUrl", required = false) String existingImageUrl,
            @RequestParam(value = "existingImageUrl2", required = false) String existingImageUrl2,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAdmin(session)) return "redirect:/login?error=Access+denied";

        try {
            String id = params.getOrDefault("id", "").trim();

            Product product = id.isEmpty()
                    ? new Product()
                    : productService.getProductById(id).orElse(new Product());

            product.setName(params.getOrDefault("name", "").trim());
            product.setDescription(params.getOrDefault("description", "").trim());
            product.setBrand(params.getOrDefault("brand", "").trim());
            product.setCategory(params.getOrDefault("category", "both").trim());
            product.setSubCategory(params.getOrDefault("subCategory", "").trim());
            product.setMaterial(params.getOrDefault("material", "").trim());
            product.setSizes(params.getOrDefault("sizes", "").trim());
            product.setColors(params.getOrDefault("colors", "").trim());
            product.setTags(params.getOrDefault("tags", "").trim());

            String origPrice = params.get("originalPrice");
            if (origPrice != null && !origPrice.isBlank())
                product.setOriginalPrice(Double.parseDouble(origPrice));

            String discPrice = params.get("discountedPrice");
            if (discPrice != null && !discPrice.isBlank())
                product.setDiscountedPrice(Double.parseDouble(discPrice));

            String premDisc = params.get("premiumDiscountPercent");
            if (premDisc != null && !premDisc.isBlank())
                product.setPremiumDiscountPercent(Double.parseDouble(premDisc));

            String stockStr = params.get("stock");
            if (stockStr != null && !stockStr.isBlank())
                product.setStock(Integer.parseInt(stockStr));

            String delivDays = params.get("deliveryDays");
            if (delivDays != null && !delivDays.isBlank())
                product.setDeliveryDays(Integer.parseInt(delivDays));

            product.setActive(params.containsKey("active"));
            product.setFeatured(params.containsKey("featured"));

            // ✅ Handle first image
            if (imageData != null && imageData.startsWith("data:image")) {
                // Delete old S3 image if exists
                Optional<Product> existing = productService.getProductById(id);
                if (existing.isPresent() && existing.get().getImageUrl() != null) {
                    imageStorageService.delete(existing.get().getImageUrl());
                }
                product.setImageUrl(saveBase64Image(imageData));
            } else if (existingImageUrl != null && !existingImageUrl.isBlank()) {
                product.setImageUrl(existingImageUrl);
            }

            // ✅ Handle second image
            if (imageData2 != null && imageData2.startsWith("data:image")) {
                Optional<Product> existing = productService.getProductById(id);
                if (existing.isPresent() && existing.get().getImageUrl2() != null) {
                    imageStorageService.delete(existing.get().getImageUrl2());
                }
                product.setImageUrl2(saveBase64Image(imageData2));
            } else if (existingImageUrl2 != null && !existingImageUrl2.isBlank()) {
                product.setImageUrl2(existingImageUrl2);
            }

            if (!id.isEmpty()) {
                product.setId(id);
                productService.updateProduct(id, product);
                redirectAttributes.addFlashAttribute("success", "✅ Product updated successfully!");
            } else {
                productService.createProduct(product);
                redirectAttributes.addFlashAttribute("success", "✅ Product added successfully!");
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ Failed to save product: " + e.getMessage());
        }

        return "redirect:/admin/products/list";
    }

    @GetMapping("/api/products/bride")
    @ResponseBody
    public ResponseEntity<List<Product>> getBrideProductsApi() {
        return ResponseEntity.ok(productService.getBrideProducts());
    }

    @GetMapping("/api/products/groom")
    @ResponseBody
    public ResponseEntity<List<Product>> getGroomProductsApi() {
        return ResponseEntity.ok(productService.getGroomProducts());
    }

    @GetMapping("/api/products/{id}")
    @ResponseBody
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/products/search")
    @ResponseBody
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String keyword) {
        return ResponseEntity.ok(productService.searchProducts(keyword));
    }

    @PostMapping("/api/admin/products")
    @ResponseBody
    public ResponseEntity<?> createProduct(@RequestBody Product product, HttpSession session) {
        if (!isAdmin(session))
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        return ResponseEntity.ok(productService.createProduct(product));
    }

    @PutMapping("/api/admin/products/{id}")
    @ResponseBody
    public ResponseEntity<?> updateProduct(@PathVariable String id,
                                           @RequestBody Product product,
                                           HttpSession session) {
        if (!isAdmin(session))
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        return ResponseEntity.ok(productService.updateProduct(id, product));
    }

    @DeleteMapping("/api/admin/products/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteProduct(@PathVariable String id, HttpSession session) {
        if (!isAdmin(session))
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));

        // ✅ Delete images from S3
        Optional<Product> product = productService.getProductById(id);
        if (product.isPresent()) {
            if (product.get().getImageUrl() != null && !product.get().getImageUrl().isBlank()) {
                imageStorageService.delete(product.get().getImageUrl());
            }
            if (product.get().getImageUrl2() != null && !product.get().getImageUrl2().isBlank()) {
                imageStorageService.delete(product.get().getImageUrl2());
            }
        }

        productService.deleteProduct(id);
        return ResponseEntity.ok(Map.of("message", "Product deactivated successfully"));
    }

    @GetMapping("/api/admin/products")
    @ResponseBody
    public ResponseEntity<?> getAllProductsAdmin(HttpSession session) {
        if (!isAdmin(session))
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        return ResponseEntity.ok(productService.getAllProductsAdmin());
    }
}