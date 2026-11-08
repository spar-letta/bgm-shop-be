package com.app.itibari.repository;

import com.app.itibari.models.entities.LocationApproval;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface LocationApprovalRepository extends JpaRepository<LocationApproval, Long>, JpaSpecificationExecutor<LocationApproval> {
    Page<LocationApproval> findAllByDeletedIsFalse(Pageable pageable);

    Optional<LocationApproval> findByPublicIdAndDeletedIsFalse(UUID locationApprovalId);
}
