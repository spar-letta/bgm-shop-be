package com.app.itibari.repository;

import com.app.itibari.models.entities.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID>, JpaSpecificationExecutor<Role> {

    Optional<Role> findByRoleNameAndDeletedIsFalse(String name);

    Optional<Role> findByRoleNameIgnoreCaseAndDeletedIsFalse(String roleName);

    Page<Role> findAllByDeletedIsFalse(Pageable pageable);

    Page<Role> findAllByRoleNameContainingAndDeletedIsFalse(String searchParam, Pageable pageable);

    Optional<Role> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Optional<Role> findFirstBySupplierPublicIdAndSupplierRoleTrueAndDeletedIsFalse(String supplierPublicId);
}
