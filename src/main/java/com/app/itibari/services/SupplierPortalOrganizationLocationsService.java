package com.app.itibari.services;

import com.app.itibari.models.entities.OrganizationLocations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Supplier Portal ward lookup — supports filtering by one or many parent districts
 * ({@code organizationRegionId}) without changing the legacy {@code /organizationLocations} contract.
 */
public interface SupplierPortalOrganizationLocationsService {

    Page<OrganizationLocations> getOrganizationLocations(String searchParam,
                                                         String name,
                                                         List<UUID> organizationRegionIds,
                                                         Pageable pageable);
}
