package com.godswill.matrimony.repository;

import com.godswill.matrimony.model.SuccessStory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SuccessStoryRepository extends MongoRepository<SuccessStory, String> {

    List<SuccessStory> findAllByOrderByMarriageDateDesc();
    List<SuccessStory> findTop4ByOrderByMarriageDateDesc();
    List<SuccessStory> findByLocation(String location);

    List<SuccessStory> findAllByOrderByCreatedAtDesc();

    List<SuccessStory> findTop4ByOrderByCreatedAtDesc();
}
