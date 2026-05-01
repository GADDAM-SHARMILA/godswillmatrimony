package com.godswill.matrimony.controller;

import com.godswill.matrimony.model.User;
import com.godswill.matrimony.service.ShortlistService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/shortlist")
public class ShortlistController {

    @Autowired
    private ShortlistService shortlistService;

    @PostMapping("/toggle/{profileId}")
    public ResponseEntity<?> toggleShortlist(
            @PathVariable String profileId,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("user");

        if (currentUser == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "User not logged in"));
        }

        try {
            boolean isShortlisted =
                    shortlistService.toggleShortlist(
                            currentUser.getEmail(),
                            profileId
                    );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("shortlisted", isShortlisted);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/check/{profileId}")
    public ResponseEntity<?> checkShortlistStatus(
            @PathVariable String profileId,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("user");

        if (currentUser == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("shortlisted", false));
        }

        try {
            boolean isShortlisted =
                    shortlistService.isShortlisted(
                            currentUser.getEmail(),
                            profileId
                    );

            return ResponseEntity.ok(
                    Map.of("shortlisted", isShortlisted)
            );

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
