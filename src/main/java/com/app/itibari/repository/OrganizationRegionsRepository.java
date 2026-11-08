package com.app.itibari.repository;

import com.app.itibari.models.entities.OrganizationRegions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRegionsRepository extends JpaRepository<OrganizationRegions, Long> {

    Optional<OrganizationRegions> findByCodeAndDeletedIsFalse(String code);

    Optional<OrganizationRegions> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Page<OrganizationRegions> findByNameContainingIgnoreCaseAndDeletedIsFalseOrCodeContainingIgnoreCaseAndDeletedIsFalse(String name,
                                                                                                                         String code,
                                                                                                                         Pageable pageable);

    Page<OrganizationRegions> findByDeletedIsFalse(Pageable pageable);

    Optional<OrganizationRegions> findByNameAndDeletedIsFalse(String name);
}
