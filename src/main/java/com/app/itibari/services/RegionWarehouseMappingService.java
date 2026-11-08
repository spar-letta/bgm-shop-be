package com.app.itibari.services;

import com.app.itibari.models.entities.RegionWarehouseMapping;
import com.app.itibari.models.request.RegionMappingUpdateRequest;
import com.app.itibari.models.request.RegionWarehouseMappingRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RegionWarehouseMappingService {

    RegionWarehouseMapping createRegionWarehouseMapping(@Valid RegionWarehouseMappingRequest regionWarehouseMappingRequest,
                                                        String createdBy);

    void updateRegionWarehouseMapping(UUID publicId, @Valid RegionMappingUpdateRequest regionWarehouseMappingRequest, String name);

    RegionWarehouseMapping getRegionWarehouseMappingByPublicId(UUID publicId, String name);

    Page<RegionWarehouseMapping> getAllRegionWarehouseMappings(Pageable pageable, String name);

    void deleteRegionWarehouseMapping(UUID publicId, String name);
}
