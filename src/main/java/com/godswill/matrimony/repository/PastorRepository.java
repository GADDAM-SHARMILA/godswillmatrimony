package com.godswill.matrimony.repository;

import com.godswill.matrimony.model.Pastor;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PastorRepository extends MongoRepository<Pastor, String> {

    // Find pastor by email for login
    Pastor findByEmail(String email);

}
