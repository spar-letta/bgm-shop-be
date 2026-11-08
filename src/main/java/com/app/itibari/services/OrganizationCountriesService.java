package com.app.itibari.services;

import com.app.itibari.models.entities.OrganizationCountries;
import com.app.itibari.models.request.OrganizationCountriesRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrganizationCountriesService {

    OrganizationCountries createOrganizationCountry(OrganizationCountriesRequest organizationCountriesRequest, String name);

    void updateOrganizationCountry(UUID organizationCountryPublicId,
                                   OrganizationCountriesRequest organizationCountriesRequest,
                                   String name);

    OrganizationCountries getOrganizationCountry(UUID publicId);

    Page<OrganizationCountries> getAllOrganizationCountries(String searchParam, Pageable pageable);

    void deleteOrganizationCountry(UUID organizationCountriesPublicId, String name);
}
