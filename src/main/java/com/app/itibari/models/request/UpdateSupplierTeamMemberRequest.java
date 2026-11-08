package com.app.itibari.models.request;

import com.app.itibari.dataTypes.IdentificationType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSupplierTeamMemberRequest {

    private String email;

    private String phoneNumber;

    private String address;

    private String city;

    private String state;

    private IdentificationType identificationType;

    private String identificationNumber;
}
