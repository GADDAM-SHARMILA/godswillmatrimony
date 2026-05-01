package com.godswill.matrimony.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "database_sequences")
public class DatabaseSequence {

    @Id
    private String id;   // sequence name, e.g. "profile_sequence"

    private long seq;
}
