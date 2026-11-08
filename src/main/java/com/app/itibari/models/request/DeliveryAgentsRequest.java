package com.app.itibari.models.request;

import com.app.itibari.dataTypes.IdentificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAgentsRequest {

    private String firstName;

    private String lastName;

    private String phoneNumber;

    private String residentialAddress;

    private UUID warehouseId;

    private IdentificationType identificationType;

    private String identificationNumber;

    private Boolean bulkUpload;
}
