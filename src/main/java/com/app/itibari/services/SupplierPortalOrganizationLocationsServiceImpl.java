package com.app.itibari.services;

import com.app.itibari.models.entities.OrganizationLocations;
import com.app.itibari.repository.OrganizationLocationsRepository;
import com.app.itibari.utils.specification.OrganizationLocationSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SupplierPortalOrganizationLocationsServiceImpl implements SupplierPortalOrganizationLocationsService {

    private final OrganizationLocationsRepository organizationLocationsRepository;

    public SupplierPortalOrganizationLocationsServiceImpl(OrganizationLocationsRepository organizationLocationsRepository) {
        this.organizationLocationsRepository = organizationLocationsRepository;
    }

    @Override
    public Page<OrganizationLocations> getOrganizationLocations(String searchParam,
                                                                String name,
                                                                List<UUID> organizationRegionIds,
                                                                Pageable pageable) {
        Specification<OrganizationLocations> spec = Specification
                .where(OrganizationLocationSpecification.searchParamMatches(searchParam))
                .and(OrganizationLocationSpecification.hasNameLike(name))
                .and(OrganizationLocationSpecification.hasOrganizationRegionsIn(organizationRegionIds))
                .and(OrganizationLocationSpecification.isNotDeleted());

        return organizationLocationsRepository.findAll(spec, pageable);
    }
}
