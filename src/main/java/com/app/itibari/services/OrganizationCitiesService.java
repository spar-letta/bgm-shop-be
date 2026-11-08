package com.app.itibari.services;

import com.app.itibari.models.entities.OrganizationCities;
import com.app.itibari.models.request.OrganizationCitiesRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrganizationCitiesService {

    OrganizationCities createOrganizationCity(OrganizationCitiesRequest organizationCitiesRequest, String username);

    void updateOrganizationCity(UUID organizationCityPublicId, OrganizationCitiesRequest organizationCitiesRequest, String username);

    OrganizationCities getOrganizationCity(UUID organizationCityPublicId);

    Page<OrganizationCities> getAllOrganizationCities(String searchParam, Pageable pageable);

    void deleteOrganizationCity(UUID organizationCityPublicId, String username);
}
