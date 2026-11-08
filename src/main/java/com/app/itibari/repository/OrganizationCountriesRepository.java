package com.app.itibari.repository;

import com.app.itibari.models.entities.OrganizationCountries;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationCountriesRepository extends JpaRepository<OrganizationCountries, Long> {

    Optional<OrganizationCountries> findByCodeAndDeletedIsFalse(String code);

    Optional<OrganizationCountries> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Page<OrganizationCountries> findByNameContainingIgnoreCaseAndDeletedIsFalseOrCodeContainingIgnoreCaseAndDeletedIsFalse(String name,
                                                                                                                           String code,
                                                                                                                           Pageable pageable);

    Page<OrganizationCountries> findByDeletedIsFalse(Pageable pageable);

    Optional<OrganizationCountries> findByNameAndDeletedIsFalse(String name);
}
