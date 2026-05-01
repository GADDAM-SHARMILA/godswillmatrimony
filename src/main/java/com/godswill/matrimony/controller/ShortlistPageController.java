package com.godswill.matrimony.controller;

import com.godswill.matrimony.model.Profile;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.service.ShortlistService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/shortlist")
public class ShortlistPageController {

    @Autowired
    private ShortlistService shortlistService;

    @GetMapping
    public String getShortlistPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model,
            HttpSession session) {

        // Get logged in user from session
        User currentUser = (User) session.getAttribute("user");

        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            Pageable pageable = PageRequest.of(page, size);

            // 🔹 CHANGED: Page<Profile>
            Page<Profile> shortlistedProfiles =
                    shortlistService.getUserShortlistPage(
                            currentUser.getEmail(),
                            pageable
                    );

            model.addAttribute("shortlistedProfiles", shortlistedProfiles.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", shortlistedProfiles.getTotalPages());
            model.addAttribute("totalItems", shortlistedProfiles.getTotalElements());
            model.addAttribute("pageSize", size);
            model.addAttribute("hasNext", shortlistedProfiles.hasNext());
            model.addAttribute("hasPrevious", shortlistedProfiles.hasPrevious());
            model.addAttribute("activePage", "shortlist");

            return "shortlist";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error loading shortlist: " + e.getMessage());
            return "shortlist";
        }
    }

    // ---------------- NAVBAR BADGE COUNT ----------------
    @GetMapping("/api/count")
    @ResponseBody
    public CountResponse getShortlistCount(HttpSession session) {

        User currentUser = (User) session.getAttribute("user");

        if (currentUser == null) {
            return new CountResponse(0);
        }

        try {
            long count =
                    shortlistService.getShortlistCount(currentUser.getEmail());

            return new CountResponse(count);

        } catch (Exception e) {
            e.printStackTrace();
            return new CountResponse(0);
        }
    }

    // ---------------- RESPONSE CLASS ----------------
    public static class CountResponse {
        public long count;

        public CountResponse(long count) {
            this.count = count;
        }

        public long getCount() {
            return count;
        }
    }
}
