package com.godswill.matrimony.repository;

import com.godswill.matrimony.model.Profile;
import com.godswill.matrimony.model.Shortlist;
import com.godswill.matrimony.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShortlistRepository extends MongoRepository<Shortlist, String> {

    Shortlist findByUserAndShortlistedProfile(User user, Profile profile);

    List<Shortlist> findByUser(User user);

    void deleteByUserAndShortlistedProfile(User user, Profile profile);

}