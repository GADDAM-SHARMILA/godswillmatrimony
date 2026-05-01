package com.godswill.matrimony.controller;

import com.godswill.matrimony.model.CateringItem;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.service.CateringItemService;
import com.godswill.matrimony.service.ImageStorageService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/catering")
public class AdminCateringController {

    private final CateringItemService cateringItemService;
    private final ImageStorageService imageStorageService;

    // ── Auth guard ──
    private boolean isAdmin(HttpSession session) {
        User u = (User) session.getAttribute("user");
        return u != null && u.getRole() != null && "ADMIN".equalsIgnoreCase(u.getRole());
    }

    /**
     * ✅ UPDATED: Convert base64 image directly to S3
     * No MockMultipartFile needed — direct bytes upload
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
        String filename = "catering." + extension;

        // ✅ Direct S3 upload — returns full public URL
        return imageStorageService.saveBytes(imageBytes, filename, mimeType);
    }

    // ── List all items ──
    @GetMapping
    public String listItems(HttpSession session, Model model, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", "Access denied");
            return "redirect:/login";
        }
        model.addAttribute("items", cateringItemService.getAll());
        model.addAttribute("pageTitle", "Admin - Catering Items");
        model.addAttribute("activePage", "admin");
        return "admin-catering-list";
    }

    // ── Show add form ──
    @GetMapping("/add")
    public String showAddForm(HttpSession session, Model model, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", "Access denied");
            return "redirect:/login";
        }
        model.addAttribute("item", new CateringItem());
        model.addAttribute("isEdit", false);
        model.addAttribute("pageTitle", "Admin - Add Catering Item");
        return "admin-catering-form";
    }

    // ── Save new item ──
    @PostMapping("/add")
    public String saveItem(
            @RequestParam String name,
            @RequestParam String category,
            @RequestParam String description,
            @RequestParam(required = false) String origin,
            @RequestParam(required = false, defaultValue = "0") int displayOrder,
            @RequestParam(required = false) String active,
            @RequestParam(required = false) String imageData,
            @RequestParam(required = false) String existingImageUrl,
            HttpSession session,
            RedirectAttributes ra) {

        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", "Access denied");
            return "redirect:/login";
        }

        try {
            CateringItem item = new CateringItem();
            item.setName(name);
            item.setCategory(category);
            item.setDescription(description);
            item.setOrigin(origin);
            item.setDisplayOrder(displayOrder);
            item.setActive(active != null && !active.equals("false"));

            // ── Resolve image ──
            item.setImageUrl(resolveImage(imageData, existingImageUrl));

            cateringItemService.save(item);
            ra.addFlashAttribute("success", "✅ Catering item added successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("error", "❌ Error saving item: " + e.getMessage());
        }
        return "redirect:/admin/catering";
    }

    // ── Show edit form ──
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable String id, HttpSession session,
                               Model model, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", "Access denied");
            return "redirect:/login";
        }
        Optional<CateringItem> opt = cateringItemService.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Item not found");
            return "redirect:/admin/catering";
        }
        model.addAttribute("item", opt.get());
        model.addAttribute("isEdit", true);
        model.addAttribute("pageTitle", "Admin - Edit Catering Item");
        return "admin-catering-form";
    }

    // ── Update item ──
    @PostMapping("/{id}/edit")
    public String updateItem(
            @PathVariable String id,
            @RequestParam String name,
            @RequestParam String category,
            @RequestParam String description,
            @RequestParam(required = false) String origin,
            @RequestParam(required = false, defaultValue = "0") int displayOrder,
            @RequestParam(required = false) String active,
            @RequestParam(required = false) String imageData,
            @RequestParam(required = false) String existingImageUrl,
            HttpSession session,
            RedirectAttributes ra) {

        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", "Access denied");
            return "redirect:/login";
        }

        Optional<CateringItem> existing = cateringItemService.findById(id);
        if (existing.isEmpty()) {
            ra.addFlashAttribute("error", "Item not found");
            return "redirect:/admin/catering";
        }

        try {
            CateringItem item = existing.get();
            item.setName(name);
            item.setCategory(category);
            item.setDescription(description);
            item.setOrigin(origin);
            item.setDisplayOrder(displayOrder);
            item.setActive(active != null && !active.equals("false"));

            // ── Handle image update ──
            String newImageUrl = resolveImage(imageData, existingImageUrl);

            // Delete old image if new one is being uploaded
            if (imageData != null && imageData.startsWith("data:image") &&
                    item.getImageUrl() != null && !item.getImageUrl().isBlank()) {
                imageStorageService.delete(item.getImageUrl());
            }

            item.setImageUrl(newImageUrl);

            cateringItemService.save(item);
            ra.addFlashAttribute("success", "✅ Catering item updated successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("error", "❌ Error updating item: " + e.getMessage());
        }
        return "redirect:/admin/catering";
    }

    // ── Toggle active ──
    @PostMapping("/{id}/toggle")
    public String toggleActive(@PathVariable String id, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", "Access denied");
            return "redirect:/login";
        }
        cateringItemService.findById(id).ifPresent(item -> {
            item.setActive(!item.isActive());
            cateringItemService.save(item);
        });
        ra.addFlashAttribute("success", "Item visibility updated.");
        return "redirect:/admin/catering";
    }

    // ── Delete ──
    @PostMapping("/{id}/delete")
    public String deleteItem(@PathVariable String id, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", "Access denied");
            return "redirect:/login";
        }

        // ✅ Delete image from S3 before deleting item
        Optional<CateringItem> item = cateringItemService.findById(id);
        if (item.isPresent() && item.get().getImageUrl() != null && !item.get().getImageUrl().isBlank()) {
            imageStorageService.delete(item.get().getImageUrl());
        }

        cateringItemService.deleteById(id);
        ra.addFlashAttribute("success", "🗑️ Item deleted.");
        return "redirect:/admin/catering";
    }

    // ── Helper: new base64 upload wins, else fall back to existing URL ──
    private String resolveImage(String imageData, String existingImageUrl) throws Exception {
        if (imageData != null && imageData.startsWith("data:image")) {
            return saveBase64Image(imageData);
        }
        return (existingImageUrl != null && !existingImageUrl.isBlank()) ? existingImageUrl : "";
    }
}