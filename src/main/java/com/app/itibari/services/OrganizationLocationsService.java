package com.app.itibari.services;

import com.app.itibari.models.entities.OrganizationLocations;
import com.app.itibari.models.request.OrganizationLocationsRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrganizationLocationsService {

    OrganizationLocations createOrganizationLocation(OrganizationLocationsRequest organizationLocationsRequest, String username);

    void updateOrganizationLocation(UUID organizationLocationPublicId,
                                    OrganizationLocationsRequest organizationLocationsRequest,
                                    String username);

    OrganizationLocations getOrganizationLocation(UUID organizationLocationPublicId);

    Page<OrganizationLocations> getAllOrganizationLocations(String searchParam, String name, UUID organizationRegionId, Pageable pageable);

    void deleteOrganizationLocation(UUID organizationLocationPublicId, String username);
}
