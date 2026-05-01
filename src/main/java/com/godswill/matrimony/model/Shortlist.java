package com.godswill.matrimony.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import java.time.LocalDateTime;


@Data
@Document(collection = "shortlist")
public class Shortlist {

    @Id
    private String id;

    @DBRef
    private User user;

    @DBRef
    private Profile shortlistedProfile;

    private LocalDateTime createdAt;

    public Shortlist() {}

    public Shortlist(User user, Profile profile) {
        this.user = user;
        this.shortlistedProfile = profile;
        this.createdAt = LocalDateTime.now();
    }


}

