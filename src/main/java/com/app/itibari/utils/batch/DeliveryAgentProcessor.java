package com.app.itibari.utils.batch;

import com.app.itibari.models.request.DeliveryAgentsRequest;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class DeliveryAgentProcessor implements ItemProcessor<DeliveryAgentsRequest, DeliveryAgentsRequest> {

    @Override
    public DeliveryAgentsRequest process(DeliveryAgentsRequest input) throws Exception {

        DeliveryAgentsRequest deliveryAgentsRequest = new DeliveryAgentsRequest();

        deliveryAgentsRequest.setFirstName(input.getFirstName());
        deliveryAgentsRequest.setLastName(input.getLastName());
        deliveryAgentsRequest.setPhoneNumber(input.getPhoneNumber());
        deliveryAgentsRequest.setResidentialAddress(input.getResidentialAddress());
        deliveryAgentsRequest.setWarehouseId(input.getWarehouseId());
        deliveryAgentsRequest.setIdentificationType(input.getIdentificationType());
        deliveryAgentsRequest.setIdentificationNumber(
                input.getIdentificationNumber().startsWith("+")
                        ? input.getIdentificationNumber()
                        : "+" + input.getIdentificationNumber()
        );
        deliveryAgentsRequest.setBulkUpload(true);

        return deliveryAgentsRequest;
    }
}
