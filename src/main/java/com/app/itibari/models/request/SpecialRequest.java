package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class SpecialRequest {

    private UUID customerId;

    private List<RequestItemDTO> items;

    private Boolean hasDeliveryAgent;

    private String  deliveryAgent;

    private String  deliveryAgentPublicId;


    public Boolean getHasDeliveryAgent() {
        return hasDeliveryAgent != null ? hasDeliveryAgent : false;
    }
}
