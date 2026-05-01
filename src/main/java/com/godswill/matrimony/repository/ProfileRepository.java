package com.godswill.matrimony.repository;

import com.godswill.matrimony.model.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends MongoRepository<Profile, String> {

    List<Profile> findByGender(String gender);

    List<Profile> findByVerified(Boolean verified);

    List<Profile> findByActive(Boolean active);

    List<Profile> findByProposedByPastorId(String pastorId);

    List<Profile> findAllByOrderByCreatedAtDesc();

    Optional<Profile> findByUserId(String userId);

    // ── NEW: used for premium discount check in checkout ──────────────────────
    Optional<Profile> findByProfileNumber(String profileNumber);

    // Religion / denomination quick queries
    List<Profile> findByReligion(String religion);

    List<Profile> findByReligionAndDenomination(String religion, String denomination);

    // Search
    @Query("{ $or: [ " +
            "{ 'firstName': { $regex: ?0, $options: 'i' } }, " +
            "{ 'lastName': { $regex: ?0, $options: 'i' } }, " +
            "{ 'city': { $regex: ?0, $options: 'i' } }, " +
            "{ 'profession': { $regex: ?0, $options: 'i' } } " +
            "] }")
    List<Profile> searchProfiles(String query);

    @Query("{ " +
            "  $and: [ " +
            "    { $or: [ { ?0: null }, { gender: ?0 } ] }, " +
            "    { $or: [ { ?1: null }, { religion: ?1 } ] }, " +
            "    { $or: [ { ?2: null }, { denomination: ?2 } ] }, " +
            "    { $or: [ { ?3: null }, { verified: ?3 } ] }, " +
            "    { $or: [ { ?4: null }, { maritalStatus: ?4 } ] }, " +
            "    { $or: [ { ?5: null }, { education: ?5 } ] }, " +
            "    { $or: [ { ?6: null }, { $or: [ " +
            "        { city:  { $regex: ?6, $options: 'i' } }, " +
            "        { state: { $regex: ?6, $options: 'i' } } " +
            "    ] } ] } " +
            "  ] " +
            "}")
    List<Profile> filterProfiles(String gender,
                                 String religion,
                                 String denomination,
                                 Boolean verified,
                                 String maritalStatus,
                                 String education,
                                 String location);
}