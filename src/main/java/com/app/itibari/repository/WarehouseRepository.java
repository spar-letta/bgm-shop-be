package com.app.itibari.repository;

import com.app.itibari.dataTypes.WarehouseType;
import com.app.itibari.models.entities.Warehouse;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    Optional<Warehouse> findByWarehouseNameIgnoreCaseAndDeletedIsFalse(@NotNull String warehouseName);

    Optional<Warehouse> findByPublicIdAndDeletedIsFalse(UUID warehousePublicId);

    Page<Warehouse> findByWarehouseNameContainingIgnoreCaseAndWarehouseTypeAndDeletedIsFalse(String searchParam,
                                                                                             WarehouseType warehouseType,
                                                                                             Pageable pageable);

    Page<Warehouse> findByWarehouseNameContainingIgnoreCaseAndDeletedIsFalse(String searchParam, Pageable pageable);

    Page<Warehouse> findByWarehouseTypeAndDeletedIsFalse(WarehouseType warehouseType, Pageable pageable);

    Page<Warehouse> findByDeletedIsFalse(Pageable pageable);

    @Query("SELECT w FROM Warehouse w WHERE w.organizationRegion.publicId = :publicId")
    Set<Warehouse> findByOrganizationRegionPublicId(UUID publicId);

    Page<Warehouse> findByParentWarehouseAndDeletedIsFalse(Warehouse warehouse, Pageable pageable);

    Optional<Warehouse> findBySuperWarehouseIsTrueAndDeletedIsFalse();

    List<Warehouse> findAllByDeletedIsFalse();

    Page<Warehouse> findByParentWarehouseAndWarehouseTypeAndDeletedIsFalse(Warehouse warehouse,
                                                                           WarehouseType warehouseType,
                                                                           Pageable pageable);

    List<Warehouse> findAllByWarehouseType(WarehouseType warehouseType);
}
