package com.app.itibari.repository;

import com.app.itibari.models.entities.CreditLimitBatches;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CreditLimitBatchRepository extends JpaRepository<CreditLimitBatches, Integer>, JpaSpecificationExecutor<CreditLimitBatches> {
    Optional<CreditLimitBatches> findCreditLimitBatchesByPublicIdAndDeletedIsFalse(UUID publicId);
}
