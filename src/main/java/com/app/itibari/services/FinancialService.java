package com.app.itibari.services;

import com.app.itibari.dataTypes.RefundAction;
import com.app.itibari.models.entities.TaxMapping;
import com.app.itibari.models.request.PurchaseOrderApprovalRequest;
import com.app.itibari.models.request.TaxMappingRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FinancialService {

    TaxMapping getTaxMappingByTaxId(UUID publicId);

    void accountantApprovePurchaseOrder(UUID purchaseOrderPublicId,
                                        @Valid PurchaseOrderApprovalRequest purchaseOrderApprovalRequest,
                                        String name);

    TaxMapping createTaxMapping(@Valid TaxMappingRequest taxMappingRequest, String name);

    void updateTaxMapping(UUID taxMappingPublicId, @Valid TaxMappingRequest taxMappingRequest, String name);

    Page<TaxMapping> getTaxMappings(String taxName, String taxRate, String taxType, Pageable pageable);

    void deleteTaxMapping(UUID taxMappingPublicId, String name);

    void accountantUpdatePurchaseOrderPayment(UUID purchaseOrderPublicId, @Valid PurchaseOrderApprovalRequest purchaseOrderApprovalRequest, String name);

    void processPurchaseOrderRefund(UUID purchaseOrderPublicId, RefundAction action,UUID rejectionCodePublicId,String name);
}
