package com.app.itibari.repository;

import com.app.itibari.models.entities.OrganizationCities;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationCitiesRepository extends JpaRepository<OrganizationCities, Long> {

    Optional<OrganizationCities> findByCodeAndDeletedIsFalse(String code);

    Optional<OrganizationCities> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Page<OrganizationCities> findByNameContainingIgnoreCaseAndDeletedIsFalseOrCodeContainingIgnoreCaseAndDeletedIsFalse(String name,
                                                                                                                        String code,
                                                                                                                        Pageable pageable);

    Page<OrganizationCities> findByDeletedIsFalse(Pageable pageable);

    Optional<OrganizationCities> findByNameAndDeletedIsFalse(String name);
}
