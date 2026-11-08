package com.app.itibari.repository;

import com.app.itibari.dataTypes.DiscountApprovalStatus;
import com.app.itibari.models.entities.DiscountApproval;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiscountApprovalRepository extends JpaRepository<DiscountApproval, Long>, JpaSpecificationExecutor<DiscountApproval> {

    Optional<DiscountApproval> findDiscountApprovalByPublicIdAndDeletedIsFalse(UUID publicId);

    Optional<DiscountApproval> findDiscountApprovalByCustomerPublicIdAndRequestedByPublicIdAndOtpAndDeletedIsFalse(String string,String string1, String otp);

    List<DiscountApproval> findDiscountApprovalByRequestedByAndDeletedIsFalse(String string, Pageable pageable);

    Optional<DiscountApproval> findByCustomerPublicIdAndRequestedByPublicIdAndApprovalStatusAndDeletedIsFalse(String string, String string1, DiscountApprovalStatus discountApprovalStatus);
}
