package com.app.itibari.services;

import com.app.itibari.models.entities.TaxCode;
import com.app.itibari.models.request.TaxCodeRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TaxCodeService {
    TaxCode createCreateTaxCode(@Valid TaxCodeRequest taxCodeRequest, String name);

    Page<TaxCode> getTaxCodes(String name, Pageable pageable);

    TaxCode fetchOneTaxCode(UUID taxCodePublicId);

    TaxCode updateTaxCode(UUID taxCodePublicId, @Valid TaxCodeRequest taxCodeRequest, String name);

    void deleteTaxCode(UUID taxCodePublicId, String name);
}
