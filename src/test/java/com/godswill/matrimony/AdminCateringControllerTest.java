package com.godswill.matrimony;

import com.godswill.matrimony.controller.AdminCateringController;
import com.godswill.matrimony.model.CateringItem;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.service.CateringItemService;
import com.godswill.matrimony.service.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminCateringControllerTest {

    @InjectMocks
    private AdminCateringController controller;

    @Mock private CateringItemService cateringItemService;
    @Mock private ImageStorageService imageStorageService;
    @Mock private Model model;
    @Mock private RedirectAttributes ra;

    private MockHttpSession adminSession;
    private MockHttpSession guestSession;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        User admin = new User();
        admin.setRole("ADMIN");

        adminSession = new MockHttpSession();
        adminSession.setAttribute("user", admin);

        guestSession = new MockHttpSession(); // no user
    }

    // ── listItems ─────────────────────────────────────────────────────────────

    @Test
    void listItems_asAdmin_returnsListView() {
        when(cateringItemService.getAll()).thenReturn(List.of(new CateringItem()));

        String view = controller.listItems(adminSession, model, ra);

        assertThat(view).isEqualTo("admin-catering-list");
        verify(model).addAttribute(eq("items"), anyList());
    }

    @Test
    void listItems_notAdmin_redirectsToLogin() {
        String view = controller.listItems(guestSession, model, ra);

        assertThat(view).isEqualTo("redirect:/login");
        verify(ra).addFlashAttribute(eq("error"), anyString());
    }

    // ── showAddForm ───────────────────────────────────────────────────────────

    @Test
    void showAddForm_asAdmin_returnsFormView() {
        String view = controller.showAddForm(adminSession, model, ra);

        assertThat(view).isEqualTo("admin-catering-form");
        verify(model).addAttribute("isEdit", false);
    }

    @Test
    void showAddForm_notAdmin_redirectsToLogin() {
        String view = controller.showAddForm(guestSession, model, ra);

        assertThat(view).isEqualTo("redirect:/login");
    }

    // ── saveItem ──────────────────────────────────────────────────────────────

    @Test
    void saveItem_asAdmin_savesAndRedirects() {
        String view = controller.saveItem(
                "Biryani", "Biryani", "Tasty biryani",
                "Hyderabad", 1, "true",
                null, null,
                adminSession, ra);

        assertThat(view).isEqualTo("redirect:/admin/catering");
        verify(cateringItemService).save(any(CateringItem.class));
        verify(ra).addFlashAttribute(eq("success"), anyString());
    }

    @Test
    void saveItem_notAdmin_redirectsToLogin() {
        String view = controller.saveItem(
                "Biryani", "Biryani", "Tasty biryani",
                "Hyderabad", 1, "true",
                null, null,
                guestSession, ra);

        assertThat(view).isEqualTo("redirect:/login");
        verify(cateringItemService, never()).save(any());
    }

    @Test
    void saveItem_withExistingImageUrl_preservesUrl() {
        String view = controller.saveItem(
                "Biryani", "Biryani", "Tasty",
                "Hyderabad", 1, "true",
                null, "/uploads/existing.jpg",
                adminSession, ra);

        assertThat(view).isEqualTo("redirect:/admin/catering");
        verify(cateringItemService).save(argThat(item ->
                "/uploads/existing.jpg".equals(item.getImageUrl())));
    }

    @Test
    void saveItem_activeIsFalse_setsInactive() {
        controller.saveItem(
                "Biryani", "Biryani", "Tasty",
                "Hyderabad", 1, "false",
                null, null,
                adminSession, ra);

        verify(cateringItemService).save(argThat(item -> !item.isActive()));
    }

    // ── showEditForm ──────────────────────────────────────────────────────────

    @Test
    void showEditForm_itemExists_returnsFormView() {
        CateringItem item = new CateringItem();
        item.setId("abc123");
        when(cateringItemService.findById("abc123")).thenReturn(Optional.of(item));

        String view = controller.showEditForm("abc123", adminSession, model, ra);

        assertThat(view).isEqualTo("admin-catering-form");
        verify(model).addAttribute("isEdit", true);
        verify(model).addAttribute("item", item);
    }

    @Test
    void showEditForm_itemNotFound_redirectsToList() {
        when(cateringItemService.findById("bad-id")).thenReturn(Optional.empty());

        String view = controller.showEditForm("bad-id", adminSession, model, ra);

        assertThat(view).isEqualTo("redirect:/admin/catering");
        verify(ra).addFlashAttribute(eq("error"), anyString());
    }

    @Test
    void showEditForm_notAdmin_redirectsToLogin() {
        String view = controller.showEditForm("abc", guestSession, model, ra);

        assertThat(view).isEqualTo("redirect:/login");
    }

    // ── updateItem ────────────────────────────────────────────────────────────

    @Test
    void updateItem_asAdmin_updatesAndRedirects() {
        CateringItem existing = new CateringItem();
        existing.setId("abc123");
        when(cateringItemService.findById("abc123")).thenReturn(Optional.of(existing));

        String view = controller.updateItem(
                "abc123",
                "Updated Biryani", "Biryani", "Updated desc",
                "Lucknow", 2, "true",
                null, "/uploads/old.jpg",
                adminSession, ra);

        assertThat(view).isEqualTo("redirect:/admin/catering");
        verify(cateringItemService).save(any(CateringItem.class));
        verify(ra).addFlashAttribute(eq("success"), anyString());
    }

    @Test
    void updateItem_itemNotFound_redirectsToList() {
        when(cateringItemService.findById("bad-id")).thenReturn(Optional.empty());

        String view = controller.updateItem(
                "bad-id",
                "Biryani", "Biryani", "desc",
                null, 0, "true",
                null, null,
                adminSession, ra);

        assertThat(view).isEqualTo("redirect:/admin/catering");
        verify(cateringItemService, never()).save(any());
    }

    @Test
    void updateItem_notAdmin_redirectsToLogin() {
        String view = controller.updateItem(
                "abc", "Biryani", "Biryani", "desc",
                null, 0, "true",
                null, null,
                guestSession, ra);

        assertThat(view).isEqualTo("redirect:/login");
    }

    // ── toggleActive ──────────────────────────────────────────────────────────

    @Test
    void toggleActive_asAdmin_togglingActiveItem_makesInactive() {
        CateringItem item = new CateringItem();
        item.setActive(true);
        when(cateringItemService.findById("abc123")).thenReturn(Optional.of(item));

        String view = controller.toggleActive("abc123", adminSession, ra);

        assertThat(view).isEqualTo("redirect:/admin/catering");
        verify(cateringItemService).save(argThat(i -> !i.isActive()));
    }

    @Test
    void toggleActive_asAdmin_togglingInactiveItem_makesActive() {
        CateringItem item = new CateringItem();
        item.setActive(false);
        when(cateringItemService.findById("abc123")).thenReturn(Optional.of(item));

        controller.toggleActive("abc123", adminSession, ra);

        verify(cateringItemService).save(argThat(CateringItem::isActive));
    }

    @Test
    void toggleActive_notAdmin_redirectsToLogin() {
        String view = controller.toggleActive("abc", guestSession, ra);

        assertThat(view).isEqualTo("redirect:/login");
        verify(cateringItemService, never()).save(any());
    }

    // ── deleteItem ────────────────────────────────────────────────────────────

    @Test
    void deleteItem_asAdmin_deletesAndRedirects() {
        String view = controller.deleteItem("abc123", adminSession, ra);

        assertThat(view).isEqualTo("redirect:/admin/catering");
        verify(cateringItemService).deleteById("abc123");
        verify(ra).addFlashAttribute(eq("success"), anyString());
    }

    @Test
    void deleteItem_notAdmin_redirectsToLogin() {
        String view = controller.deleteItem("abc", guestSession, ra);

        assertThat(view).isEqualTo("redirect:/login");
        verify(cateringItemService, never()).deleteById(any());
    }
}