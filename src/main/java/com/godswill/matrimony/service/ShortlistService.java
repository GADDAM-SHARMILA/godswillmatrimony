package com.godswill.matrimony.service;

import com.godswill.matrimony.model.Profile;
import com.godswill.matrimony.model.Shortlist;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.repository.ProfileRepository;
import com.godswill.matrimony.repository.ShortlistRepository;
import com.godswill.matrimony.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShortlistService {

    @Autowired
    private ShortlistRepository shortlistRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;


    // ---------------- TOGGLE ----------------
    public boolean toggleShortlist(String emailOrPhone, String profileId) {

        User currentUser = findUser(emailOrPhone);

        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        Shortlist existing =
                shortlistRepository.findByUserAndShortlistedProfile(currentUser, profile);

        if (existing != null) {
            shortlistRepository.delete(existing);
            return false;
        }

        Shortlist shortlist = new Shortlist();
        shortlist.setUser(currentUser);
        shortlist.setShortlistedProfile(profile);
        shortlist.setCreatedAt(LocalDateTime.now());

        shortlistRepository.save(shortlist);
        return true;
    }


    // ---------------- CHECK ----------------
    public boolean isShortlisted(String emailOrPhone, String profileId) {

        User currentUser = findUser(emailOrPhone);

        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        return shortlistRepository
                .findByUserAndShortlistedProfile(currentUser, profile) != null;
    }


    // ---------------- LIST ----------------
    public List<Profile> getUserShortlist(String emailOrPhone) {

        User currentUser = findUser(emailOrPhone);

        return shortlistRepository.findByUser(currentUser)
                .stream()
                .map(Shortlist::getShortlistedProfile)
                .collect(Collectors.toList());
    }


    // ---------------- PAGINATION ----------------
    public Page<Profile> getUserShortlistPage(String emailOrPhone, Pageable pageable) {

        User currentUser = findUser(emailOrPhone);

        List<Profile> profiles = shortlistRepository.findByUser(currentUser)
                .stream()
                .map(Shortlist::getShortlistedProfile)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), profiles.size());

        if (start > profiles.size()) {
            return new PageImpl<>(List.of(), pageable, profiles.size());
        }

        return new PageImpl<>(
                profiles.subList(start, end),
                pageable,
                profiles.size()
        );
    }


    // ---------------- COUNT ----------------
    public long getShortlistCount(String emailOrPhone) {
        User currentUser = findUser(emailOrPhone);
        return shortlistRepository.findByUser(currentUser).size();
    }


    // ---------------- HELPER ----------------
    private User findUser(String emailOrPhone) {
        return userRepository.findByEmail(emailOrPhone)
                .orElseGet(() ->
                        userRepository.findByPhone(emailOrPhone)
                                .orElseThrow(() ->
                                        new RuntimeException("User not found: " + emailOrPhone)
                                )
                );
    }
}
