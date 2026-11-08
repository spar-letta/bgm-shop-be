package com.app.itibari.models.request;

import com.app.itibari.dataTypes.IdentificationType;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UpdatePersonRequest {

    private String firstName;

    private String lastName;

    private String phoneNumber;

    private String address;

    private String city;

    private String state;

    private String identificationNumber;

    private IdentificationType identificationType;

    private String email;

    private UUID departmentPublicId;
}
