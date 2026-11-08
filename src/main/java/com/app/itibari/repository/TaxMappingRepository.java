package com.app.itibari.repository;

import com.app.itibari.models.entities.TaxMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface TaxMappingRepository extends JpaRepository<TaxMapping, Long> {

    Optional<TaxMapping> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Optional<TaxMapping> findByTaxNameIgnoreCaseAndDeletedIsFalse(String taxName);

    Page<TaxMapping> findAllByDeletedIsFalse(Pageable pageable);

    Optional<TaxMapping> findByTaxRateAndDeletedIsFalse(BigDecimal taxMappingDecimal);
}
