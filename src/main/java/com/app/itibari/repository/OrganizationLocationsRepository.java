package com.app.itibari.repository;

import com.app.itibari.models.entities.OrganizationLocations;
import com.app.itibari.models.entities.OrganizationRegions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationLocationsRepository
        extends JpaRepository<OrganizationLocations, Long>, JpaSpecificationExecutor<OrganizationLocations> {

    Optional<OrganizationLocations> findByCodeAndDeletedIsFalse(String code);

    Optional<OrganizationLocations> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Page<OrganizationLocations> findByDeletedIsFalse(Pageable pageable);

    Optional<OrganizationLocations> findByNameAndDeletedIsFalse(String name);

    Optional<OrganizationLocations> findByOrganizationRegionAndPublicIdAndDeletedIsFalse(OrganizationRegions organizationRegion,
                                                                                         UUID locationId);

    List<OrganizationLocations> findByOrganizationRegionAndDeletedIsFalse(OrganizationRegions organizationRegion);
}
