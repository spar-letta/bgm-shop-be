package com.app.itibari.repository;

import com.app.itibari.models.entities.TaxCode;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaxCodeRepository extends JpaRepository<TaxCode, Long> {
    Optional<TaxCode> findByCodeNameIgnoreCase(String codeName);

    List<TaxCode> findByDeletedIsFalse();

    long countByDeletedIsFalse();

    Optional<TaxCode> findByPublicId(UUID taxCodePublicId);

    List<TaxCode> findAllByDeletedIsFalseOrderByCodeAsc();
}
