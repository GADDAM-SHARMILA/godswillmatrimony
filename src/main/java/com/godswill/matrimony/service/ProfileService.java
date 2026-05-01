package com.godswill.matrimony.service;

import com.godswill.matrimony.model.Profile;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final MongoTemplate mongoTemplate;
    private final SequenceGeneratorService sequenceGeneratorService;

    private static final String PROFILE_SEQ = "profile_sequence";
    private static final String PROFILE_PREFIX = "GWM-";

    // ================= CREATE OR UPDATE PROFILE =================
    public Profile createProfile(Profile profile) {

        Optional<Profile> existingOpt = profileRepository.findByUserId(profile.getUserId());

        if (existingOpt.isPresent()) {
            // UPDATE EXISTING
            Profile existing = existingOpt.get();

            // keep same Mongo _id
            profile.setId(existing.getId());

            // Preserve existing imageUrl if request didn't send it
            if (profile.getImageUrl() == null || profile.getImageUrl().isBlank()) {
                profile.setImageUrl(existing.getImageUrl());
            }
            if (profile.getImageUrl2() == null || profile.getImageUrl2().isBlank()) {
                profile.setImageUrl2(existing.getImageUrl2());
            }
            if (profile.getImageUrl3() == null || profile.getImageUrl3().isBlank()) {
                profile.setImageUrl3(existing.getImageUrl3());
            }

            // Preserve profileNumber so it doesn't get overwritten/lost
            if (profile.getProfileNumber() == null || profile.getProfileNumber().isBlank()) {
                profile.setProfileNumber(existing.getProfileNumber());
            }

            profile.onUpdate();
        } else {
            // CREATE NEW
            long next = sequenceGeneratorService.generateSequence(PROFILE_SEQ);
            profile.setProfileNumber(PROFILE_PREFIX + next); // GWM-1, GWM-2...
            profile.onCreate();
        }

        // Normalize (extra safety)
        if (profile.getReligion() != null) profile.setReligion(profile.getReligion().trim().toLowerCase());
        if (profile.getDenomination() != null) profile.setDenomination(profile.getDenomination().trim().toLowerCase());

        // If DOB exists and age missing, compute age
        if (profile.getDateOfBirth() != null && profile.getAge() == null) {
            profile.setAge(Period.between(profile.getDateOfBirth(), LocalDate.now()).getYears());
        }

        return profileRepository.save(profile);
    }

    // ================= UPDATE PROFILE =================
    public Profile updateProfile(Profile profile) {
        // Preserve imageUrl and profileNumber on updateProfile() for safety
        if (profile.getUserId() != null) {
            profileRepository.findByUserId(profile.getUserId()).ifPresent(existing -> {
                if (profile.getImageUrl() == null || profile.getImageUrl().isBlank()) {
                    profile.setImageUrl(existing.getImageUrl());
                }
                if (profile.getProfileNumber() == null || profile.getProfileNumber().isBlank()) {
                    profile.setProfileNumber(existing.getProfileNumber());
                }
            });
        }

        profile.onUpdate();
        return profileRepository.save(profile);
    }

    // ================= SAVE PROFILE (Admin / Pastor flow) =================
    public Profile saveProfile(Profile profile) {

        // ✅ FIX: Assign profileNumber if not already set
        // - New profiles by Admin/Pastor will get a sequence number (GWM-1, GWM-2...)
        // - Existing profiles being updated will keep their existing number (preserved above)
        if (profile.getProfileNumber() == null || profile.getProfileNumber().isBlank()) {
            long next = sequenceGeneratorService.generateSequence(PROFILE_SEQ);
            profile.setProfileNumber(PROFILE_PREFIX + next);
        }

        // Auto-calculate age if DOB is provided
        if (profile.getDateOfBirth() != null && profile.getAge() == null) {
            profile.setAge(Period.between(profile.getDateOfBirth(), LocalDate.now()).getYears());
        }

        // Normalize religion/denomination
        normalizeProfile(profile);

        // Call lifecycle hook
        profile.onCreate();

        return profileRepository.save(profile);
    }

    // ================= GET PROFILE BY ID =================
    public Optional<Profile> getProfileById(String id) {
        Optional<Profile> profile = profileRepository.findById(id);

        profile.ifPresent(p -> {
            if (p.getViewCount() == null) p.setViewCount(0);
            p.setViewCount(p.getViewCount() + 1);
            p.onUpdate();
            profileRepository.save(p);
        });

        return profile;
    }

    // ================= GET PROFILE BY USER ID =================
    public Optional<Profile> getProfileByUserId(String userId) {
        return profileRepository.findByUserId(userId);
    }

    // ================= GET ALL ACTIVE PROFILES =================
    public List<Profile> getAllProfiles() {
        return profileRepository.findByActive(true);
    }

    // ================= SEARCH =================
    public List<Profile> searchProfiles(String query) {
        return profileRepository.searchProfiles(query);
    }

    // ================= FILTER (with denomination + location + age range) =================
    public List<Profile> filterProfiles(String gender,
                                        String religion,
                                        String denomination,
                                        Boolean verified,
                                        Integer ageFrom,
                                        Integer ageTo,
                                        String maritalStatus,
                                        String education,
                                        String location) {

        Query query = new Query();

        if (gender != null && !gender.isBlank()) {
            query.addCriteria(Criteria.where("gender").is(gender.trim().toLowerCase()));
        }

        if (religion != null && !religion.isBlank()) {
            query.addCriteria(Criteria.where("religion").is(religion.trim().toLowerCase()));
        }

        if (denomination != null && !denomination.isBlank()) {
            query.addCriteria(Criteria.where("denomination").is(denomination.trim().toLowerCase()));
        }

        if (verified != null) {
            query.addCriteria(Criteria.where("verified").is(verified));
        }

        if (maritalStatus != null && !maritalStatus.isBlank()) {
            query.addCriteria(Criteria.where("maritalStatus").is(maritalStatus));
        }

        if (education != null && !education.isBlank()) {
            query.addCriteria(Criteria.where("education").is(education));
        }

        // Location filter (city/state)
        if (location != null && !location.isBlank()) {
            String loc = location.trim();
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("city").regex(loc, "i"),
                    Criteria.where("state").regex(loc, "i")
            ));
        }

        query.addCriteria(Criteria.where("active").is(true));

        List<Profile> results = mongoTemplate.find(query, Profile.class);

        // Age filtering: safest using calculated age (derived from DOB)
        if (ageFrom != null || ageTo != null) {
            results = results.stream().filter(p -> {
                Integer ca = p.getCalculatedAge();
                if (ca == null) return false;
                if (ageFrom != null && ca < ageFrom) return false;
                if (ageTo != null && ca > ageTo) return false;
                return true;
            }).toList();
        }

        return results;
    }

    // ================= AUTO CREATE AFTER REGISTRATION =================
    public Profile createProfileForUser(User user) {

        Optional<Profile> existing = profileRepository.findByUserId(user.getId());
        if (existing.isPresent()) return existing.get();

        Profile profile = new Profile();

        // Generate profileNumber
        long next = sequenceGeneratorService.generateSequence(PROFILE_SEQ);
        profile.setProfileNumber(PROFILE_PREFIX + next);

        profile.setUserId(user.getId());
        profile.setFirstName(user.getFirstName());
        profile.setLastName(user.getLastName());
        profile.setGender(user.getGender());
        profile.setEmail(user.getEmail());
        profile.setPhone(user.getPhone());

        if (user.getDateOfBirth() != null) {
            profile.setDateOfBirth(user.getDateOfBirth());
            profile.setAge(Period.between(profile.getDateOfBirth(), LocalDate.now()).getYears());
        }

        profile.setActive(true);
        profile.setVerified(false);
        profile.setViewCount(0);
        profile.setCountry("India");

        profile.onCreate();
        return profileRepository.save(profile);
    }

    // ================= HELPERS =================
    private void normalizeProfile(Profile profile) {
        if (profile.getReligion() != null) profile.setReligion(profile.getReligion().trim().toLowerCase());
        if (profile.getDenomination() != null) profile.setDenomination(profile.getDenomination().trim().toLowerCase());
    }

    public void deleteProfile(String id) {
        profileRepository.deleteById(id);
    }

    public List<Profile> getProfilesByPastor(String pastorId) {
        return profileRepository.findByProposedByPastorId(pastorId);
    }
}