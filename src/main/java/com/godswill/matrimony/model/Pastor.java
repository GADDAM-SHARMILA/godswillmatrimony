package com.godswill.matrimony.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.*;


@Data
@Document(collection = "pastors")
public class Pastor {

    @Id
    private String id;

    private String firstName;
    private String lastName;
    private Integer age;
    private String dateOfBirth;
    private String bornAgain;
    private String baptismDate;
    private String callingDate;
    private String location;
    private String place;
    private String churchName;
    private String denomination;
    private String address;
    private String email;
    private String phoneNumber;
    private String alternatePhoneNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128)
    private String password;

    @NotBlank(message = "Confirm password is required")
    @Size(min = 8, max = 128)
    private String confirmPassword;

    public Pastor() {}



    public boolean passwordsMatch() {
        return password != null && password.equals(confirmPassword);
    }
}
