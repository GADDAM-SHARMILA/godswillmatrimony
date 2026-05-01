package com.godswill.matrimony.repository;

import com.godswill.matrimony.model.Otp;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OtpRepository extends MongoRepository<Otp, String> {
    Optional<Otp> findByEmailAndPurpose(String email, String purpose);
    Optional<Otp> findByEmailAndOtpCode(String email, String otpCode);
    void deleteByEmailAndPurpose(String email, String purpose);
}
