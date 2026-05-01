package com.godswill.matrimony.repository;

import com.godswill.matrimony.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    // Login by email OR phone
    @Query("{ $or: [ { 'email': ?0 }, { 'phone': ?1 } ] }")
    Optional<User> findByEmailOrPhone(String email, String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
    Optional<User> findByResetToken(String resetToken);
}
