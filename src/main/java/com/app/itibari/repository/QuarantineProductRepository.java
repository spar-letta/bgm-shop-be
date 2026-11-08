package com.app.itibari.repository;

import com.app.itibari.dataTypes.QuarantineProductStatus;
import com.app.itibari.models.entities.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface QuarantineProductRepository extends JpaRepository<QuarantineProducts, Long>, JpaSpecificationExecutor<QuarantineProducts> {

    Optional<QuarantineProducts> findByPublicIdAndDeletedIsFalse(UUID quarantineProductId);

    Page<QuarantineProducts> findByQuarantineAndDeletedIsFalse(Quarantine quarantine, Pageable pageable);

    Page<QuarantineProducts> findByDeletedIsFalseAndDisposedStatus(QuarantineProductStatus quarantineProductStatus, Pageable pageable);

    @Query("SELECT q FROM QuarantineProducts q " +
            "WHERE LOWER(q.productName) LIKE LOWER(CONCAT('%', :searchParam, '%')) " +
            "AND q.deleted = false")
    Page<QuarantineProducts> findByProductNameAndDeletedIsFalse(@Param("searchParam") String searchParam,
                                                                 Pageable pageable);
}
