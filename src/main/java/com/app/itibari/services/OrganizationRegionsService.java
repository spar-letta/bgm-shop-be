package com.app.itibari.services;

import com.app.itibari.models.entities.OrganizationRegions;
import com.app.itibari.models.request.OrganizationRegionsRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrganizationRegionsService {

    OrganizationRegions createOrganizationRegion(OrganizationRegionsRequest organizationRegionsRequest, String username);

    void updateOrganizationRegion(UUID organizationRegionPublicId, OrganizationRegionsRequest organizationRegionsRequest, String username);

    OrganizationRegions getOrganizationRegion(UUID organizationRegionPublicId);

    Page<OrganizationRegions> getAllOrganizationRegions(String searchParam, Pageable pageable);

    void deleteOrganizationRegion(UUID organizationRegionPublicId, String username);
}
