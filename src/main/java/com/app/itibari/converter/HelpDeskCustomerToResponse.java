package com.app.itibari.converter;

import com.app.itibari.models.entities.HelpDeskCustomer;
import com.app.itibari.models.response.HelpDeskCustomerDTOResponse;
import org.springframework.stereotype.Component;

@Component
public class HelpDeskCustomerToResponse implements Converter<HelpDeskCustomer, HelpDeskCustomerDTOResponse> {
    @Override
    public HelpDeskCustomerDTOResponse convert(HelpDeskCustomer helpDeskCustomer) {

        if (helpDeskCustomer == null){
            return null;
        }

        long size = helpDeskCustomer.getHelpDesks().isEmpty() ? 0L : helpDeskCustomer.getHelpDesks().size();

        HelpDeskCustomerDTOResponse.District district = HelpDeskCustomerDTOResponse.District.builder()
                .publicId(helpDeskCustomer.getDistrictId())
                .name(helpDeskCustomer.getDistrict()).build();

        HelpDeskCustomerDTOResponse.Ward ward = HelpDeskCustomerDTOResponse.Ward.builder()
                .publicId(helpDeskCustomer.getWardId())
                .name(helpDeskCustomer.getWard()).build();


        return HelpDeskCustomerDTOResponse.builder()
                .customerName(helpDeskCustomer.getName())
                .phone(helpDeskCustomer.getPhoneNumber())
                .customerType(helpDeskCustomer.getCustomerType().name())
                .district(district)
                .ward(ward)
                .publicId(helpDeskCustomer.getPublicId())
                .ticketSize(size)
                .storeName(helpDeskCustomer.getStoreName())
                .existingCustomerPublicId(helpDeskCustomer.getExistingCustomerPublicId())
                .build();

    }
}
