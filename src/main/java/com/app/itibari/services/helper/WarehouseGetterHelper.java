package com.app.itibari.services.helper;

import com.app.itibari.models.entities.OrganizationRegions;
import com.app.itibari.models.entities.RegionWarehouseMapping;
import com.app.itibari.models.entities.Warehouse;
import com.app.itibari.repository.RegionWarehouseMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WarehouseGetterHelper {

    private final RegionWarehouseMappingRepository regionWarehouseMappingRepository;

    public Warehouse getWarehouse(OrganizationRegions regions) {

        if (regions == null) {
            return null;
        }

        Optional<RegionWarehouseMapping> optionalRegionWarehouseMapping = regionWarehouseMappingRepository.findByMappedRegionsAndDeletedIsFalse(
                Collections.singletonList(regions));

        if (optionalRegionWarehouseMapping.isPresent()) {
            RegionWarehouseMapping regionWarehouseMapping = optionalRegionWarehouseMapping.get();
            return regionWarehouseMapping.getWarehouse();
        } else {
            return null;
        }
    }
}
