package com.app.itibari.services;

import com.app.itibari.dataTypes.RemittanceStatus;
import com.app.itibari.models.entities.Remittances;
import com.app.itibari.models.request.RemittanceRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RemittanceService {

    Remittances createRemittance(@Valid RemittanceRequest remittanceRequest, String name);

    void updateRemittance(UUID remittancePublicId, @Valid RemittanceRequest remittanceRequest, String name);

    Remittances getRemittanceByPublicId(UUID remittancePublicId);

    Page<Remittances> getRemittances(String invoiceNumber,
                                     String purchaseOrderNumber,
                                     UUID supplier,
                                     RemittanceStatus status,
                                     String paymentMethod,
                                     String to,
                                     String from,
                                     Pageable pageable);

    void deleteRemittance(UUID remittancePublicId, String name);
}
