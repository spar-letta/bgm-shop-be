package com.app.itibari.repository;

import com.app.itibari.models.entities.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Permission findByName(String name);

    Optional<Permission> findByNameAndDeletedIsFalse(String permission);

    Optional<Permission> findByPublicIdAndDeletedIsFalse(UUID permissionId);

    Optional<Permission> findByNameIgnoreCaseAndDeletedIsFalse(String permission);
}
