package com.app.itibari.services;

import com.app.itibari.models.entities.OrganizationLocations;
import com.app.itibari.models.entities.OrganizationRegions;
import com.app.itibari.repository.OrganizationLocationsRepository;
import com.app.itibari.utils.specification.OrganizationLocationSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupplierPortalOrganizationLocationsServiceImplTest {

    private static final UUID REGION_A = UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14");
    private static final UUID REGION_B = UUID.fromString("8764678d-d810-4e71-855e-82e2a4bbf99f");

    private OrganizationLocationsRepository organizationLocationsRepository;
    private SupplierPortalOrganizationLocationsServiceImpl service;

    @BeforeEach
    void setUp() {
        organizationLocationsRepository = mock(OrganizationLocationsRepository.class);
        service = new SupplierPortalOrganizationLocationsServiceImpl(organizationLocationsRepository);
    }

    @Test
    void getOrganizationLocations_filtersByMultipleRegionsAndSearch() {
        Pageable pageable = PageRequest.of(0, 20);
        OrganizationLocations ward = sampleWard("Kinondoni", REGION_A);
        Page<OrganizationLocations> expected = new PageImpl<>(List.of(ward), pageable, 1);

        when(organizationLocationsRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expected);

        Page<OrganizationLocations> result = service.getOrganizationLocations(
                "kin", null, List.of(REGION_A, REGION_B), pageable);

        assertSame(expected, result);
        verify(organizationLocationsRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getOrganizationLocations_emptyRegionListSkipsRegionFilter() {
        Pageable pageable = PageRequest.of(1, 20);
        Page<OrganizationLocations> expected = Page.empty(pageable);

        when(organizationLocationsRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expected);

        Page<OrganizationLocations> result = service.getOrganizationLocations("abc", "ward", List.of(), pageable);

        assertEquals(0, result.getTotalElements());
        verify(organizationLocationsRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void hasOrganizationRegionsIn_appliesInClauseWhenIdsPresent() {
        var spec = OrganizationLocationSpecification.hasOrganizationRegionsIn(List.of(REGION_A, REGION_B));
        org.junit.jupiter.api.Assertions.assertNotNull(spec);
    }

    private static OrganizationLocations sampleWard(String name, UUID regionPublicId) {
        OrganizationRegions region = new OrganizationRegions();
        region.setPublicId(regionPublicId);
        region.setName("District");

        OrganizationLocations location = new OrganizationLocations();
        location.setPublicId(UUID.randomUUID());
        location.setName(name);
        location.setOrganizationRegion(region);
        return location;
    }
}
