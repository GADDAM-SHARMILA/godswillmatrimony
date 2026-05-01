package com.godswill.matrimony.service;

import com.godswill.matrimony.model.Pastor;
import com.godswill.matrimony.repository.PastorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PastorService {

    @Autowired
    private PastorRepository pastorRepository;

    // Save pastor
    public Pastor savePastor(Pastor pastor) {
        return pastorRepository.save(pastor);
    }

    // Find pastor by email (for login)
    public Pastor findByEmail(String email) {
        return pastorRepository.findByEmail(email);
    }
}
