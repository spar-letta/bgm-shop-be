package com.app.itibari.repository;

import com.app.itibari.models.entities.CorporateCustomerBnplApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CorporateCustomerBnplApprovalRepository extends JpaRepository<CorporateCustomerBnplApproval, Long> {
    Optional<CorporateCustomerBnplApproval> findCorporateCustomerBnplApprovalByPublicIdAndDeletedIsFalse(UUID publicId);
}
