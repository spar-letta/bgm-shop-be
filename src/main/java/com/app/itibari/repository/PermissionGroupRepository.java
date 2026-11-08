package com.app.itibari.repository;

import com.app.itibari.models.entities.PermissionGroup;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PermissionGroupRepository extends JpaRepository<PermissionGroup, UUID>, JpaSpecificationExecutor<PermissionGroup> {

    Optional<PermissionGroup> findByGroupNameAndDeletedIsFalse(String name);

    Optional<PermissionGroup> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Page<PermissionGroup> findAllByDeletedIsFalse(Pageable pageable);

    Optional<PermissionGroup> findByGroupNameIgnoreCaseAndDeletedIsFalse(@NotNull String groupName);

    Optional<PermissionGroup> findByGroupNameIgnoreCaseAndSupplierPublicIdAndDeletedIsFalse(String supplierName, String string);

    @Query("""
        SELECT gp 
        FROM PermissionGroup gp 
        WHERE gp.deleted = false 
          AND LOWER(gp.groupName) LIKE LOWER(CONCAT('%', :searchParam, '%'))
    """)
    Page<PermissionGroup> findBySearchParam(
            @Param("searchParam") String searchParam,
            Pageable pageable
    );
}
