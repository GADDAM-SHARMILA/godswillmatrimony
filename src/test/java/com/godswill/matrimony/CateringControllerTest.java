package com.godswill.matrimony;

import com.godswill.matrimony.controller.CateringController;
import com.godswill.matrimony.model.CateringItem;
import com.godswill.matrimony.service.CateringItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CateringControllerTest {

    @InjectMocks
    private CateringController controller;

    @Mock private CateringItemService cateringItemService;
    @Mock private Model model;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ── cateringPage ──────────────────────────────────────────────────────────

    @Test
    void cateringPage_returnsCorrectViewName() {
        when(cateringItemService.getAllActive()).thenReturn(List.of());

        String view = controller.cateringPage(null, model);

        assertThat(view).isEqualTo("catering");
    }

    @Test
    void cateringPage_addsGroupedItemsToModel() {
        CateringItem biryani = new CateringItem();
        biryani.setCategory("Biryani");
        biryani.setName("Hyderabadi Dum Biryani");

        CateringItem snack = new CateringItem();
        snack.setCategory("Snacks");
        snack.setName("Samosa");

        when(cateringItemService.getAllActive()).thenReturn(List.of(biryani, snack));

        String view = controller.cateringPage(null, model);

        assertThat(view).isEqualTo("catering");
        verify(model).addAttribute(eq("grouped"), argThat(arg -> {
            @SuppressWarnings("unchecked")
            Map<String, List<CateringItem>> grouped = (Map<String, List<CateringItem>>) arg;
            return grouped.containsKey("Biryani") && grouped.containsKey("Snacks");
        }));
    }

    @Test
    void cateringPage_addsTotalItemsToModel() {
        CateringItem item1 = new CateringItem();
        item1.setCategory("Biryani");
        CateringItem item2 = new CateringItem();
        item2.setCategory("Snacks");

        when(cateringItemService.getAllActive()).thenReturn(List.of(item1, item2));

        controller.cateringPage(null, model);

        verify(model).addAttribute("totalItems", 2);
    }

    @Test
    void cateringPage_emptyMenu_returnsEmptyGrouped() {
        when(cateringItemService.getAllActive()).thenReturn(List.of());

        String view = controller.cateringPage(null, model);

        assertThat(view).isEqualTo("catering");
        verify(model).addAttribute("totalItems", 0);
    }

    @Test
    void cateringPage_setsCorrectActivePage() {
        when(cateringItemService.getAllActive()).thenReturn(List.of());

        controller.cateringPage(null, model);

        verify(model).addAttribute("activePage", "catering");
    }

    @Test
    void cateringPage_setsPageTitle() {
        when(cateringItemService.getAllActive()).thenReturn(List.of());

        controller.cateringPage(null, model);

        verify(model).addAttribute("pageTitle", "Catering Menu");
    }

    @Test
    void cateringPage_itemsGroupedByCategory_correctCounts() {
        CateringItem b1 = new CateringItem(); b1.setCategory("Biryani");
        CateringItem b2 = new CateringItem(); b2.setCategory("Biryani");
        CateringItem s1 = new CateringItem(); s1.setCategory("Snacks");

        when(cateringItemService.getAllActive()).thenReturn(List.of(b1, b2, s1));

        controller.cateringPage(null, model);

        verify(model).addAttribute(eq("grouped"), argThat(arg -> {
            @SuppressWarnings("unchecked")
            Map<String, List<CateringItem>> grouped = (Map<String, List<CateringItem>>) arg;
            return grouped.get("Biryani").size() == 2
                    && grouped.get("Snacks").size() == 1;
        }));
    }

    @Test
    void cateringPage_callsGetAllActive() {
        when(cateringItemService.getAllActive()).thenReturn(List.of());

        controller.cateringPage("Biryani", model);

        verify(cateringItemService).getAllActive();
    }
}