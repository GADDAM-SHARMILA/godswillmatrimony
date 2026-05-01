package com.godswill.matrimony.controller;

import com.godswill.matrimony.model.CarouselImage;
import com.godswill.matrimony.model.Profile;
import com.godswill.matrimony.model.SuccessStory;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.service.CarouselImageService;
import com.godswill.matrimony.service.ProfileService;
import com.godswill.matrimony.service.SuccessStoryService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProfileService profileService;
    private final SuccessStoryService successStoryService;
    private final CarouselImageService carouselImageService;

    @GetMapping("/")
    public String home(Model model, HttpSession session) {

        try {

            User loggedInUser = (User) session.getAttribute("user");
            boolean isLoggedIn = loggedInUser != null;
            boolean isAdmin    = isLoggedIn
                    && loggedInUser.getRole() != null
                    && loggedInUser.getRole().equalsIgnoreCase("ADMIN");
            boolean isPremium  = isAdmin || (isLoggedIn && loggedInUser.isPremium());

            model.addAttribute("isLoggedIn", isLoggedIn);
            model.addAttribute("isPremium",  isPremium);

            // Carousel
            List<CarouselImage> carouselImages = carouselImageService.getAllActiveCarouselImages();
            model.addAttribute("carouselImages",
                    carouselImages != null ? carouselImages : Collections.emptyList());

            // Profiles — visible to admin AND premium users
            if (isPremium) {
                List<Profile> profiles = profileService.getAllProfiles();
                if (profiles == null) profiles = Collections.emptyList();
                if (profiles.size() > 12) profiles = profiles.subList(0, 12);
                model.addAttribute("profiles", profiles);
            } else {
                model.addAttribute("profiles", Collections.emptyList());
            }

            // Success Stories
            List<SuccessStory> successStories = successStoryService.getAllSuccessStories();
            model.addAttribute("successStories", successStories);

            model.addAttribute("activePage", "home");
            model.addAttribute("pageTitle", "God's Will Matrimony - Home");

        } catch (Exception e) {
            model.addAttribute("carouselImages",  List.of());
            model.addAttribute("profiles",        List.of());
            model.addAttribute("successStories",  List.of());
            model.addAttribute("activePage",      "home");
            model.addAttribute("pageTitle",       "God's Will Matrimony - Home");
        }

        return "home";
    }

    @GetMapping("/privacy-policy")
    public String privacyPolicy() { return "privacy-policy"; }

    @GetMapping("/terms-and-conditions")
    public String termsAndConditions() { return "terms-and-conditions"; }

    @GetMapping("/about")
    public String aboutus() { return "about"; }

    @GetMapping("/associate")
    public String ouraccociates() { return "associate"; }

    @GetMapping("/gallery")
    public String gallery() { return "gallery"; }

    @GetMapping("/faq")
    public String faqs() { return "faqs"; }

    @GetMapping("/premium-plans")
    public String premiumplans() { return "user/premium-plans"; }

    @GetMapping("/safety-center")
    public String safetyCenter() { return "safety-center"; }
}