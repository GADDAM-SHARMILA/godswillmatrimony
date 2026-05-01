package com.godswill.matrimony.controller;

import com.godswill.matrimony.model.CateringItem;
import com.godswill.matrimony.service.CateringItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/catering")
public class CateringController {

    private final CateringItemService cateringItemService;

    @GetMapping
    public String cateringPage(@RequestParam(required = false) String category, Model model) {
        List<CateringItem> items = cateringItemService.getAllActive();

        // Group by category for the dropdown layout
        Map<String, List<CateringItem>> grouped = items.stream()
                .collect(Collectors.groupingBy(CateringItem::getCategory));

        model.addAttribute("grouped", grouped);
        model.addAttribute("totalItems", items.size());
        model.addAttribute("pageTitle", "Catering Menu");
        model.addAttribute("activePage", "catering");
        return "catering";
    }
}