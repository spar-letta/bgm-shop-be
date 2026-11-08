package com.app.itibari.utils.batch;

import com.app.itibari.models.request.SupplierRequest;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class SupplierProcessor implements ItemProcessor<SupplierRequest, SupplierRequest> {

    @Override
    public SupplierRequest process(SupplierRequest input) throws Exception {

        SupplierRequest supplierRequest = new SupplierRequest();
        supplierRequest.setSupplierName(input.getSupplierName());
        supplierRequest.setSupplierAddress(input.getSupplierAddress());
        supplierRequest.setSupplierPointOfContact(input.getSupplierPointOfContact());
        supplierRequest.setPhoneNumber(input.getPhoneNumber());
        supplierRequest.setEmail(input.getEmail());
        supplierRequest.setLeadDays(input.getLeadDays());
        supplierRequest.setCreditTerms(input.getCreditTerms());
        supplierRequest.setTinNumber(input.getTinNumber());
        supplierRequest.setCreditLimit(input.getCreditLimit());
        supplierRequest.setPostalAddress(input.getPostalAddress());
        supplierRequest.setBankName(input.getBankName());
        supplierRequest.setBankAccountName(input.getBankAccountName());
        supplierRequest.setBankAccountNumber(input.getBankAccountNumber());
        return supplierRequest;
    }
}
