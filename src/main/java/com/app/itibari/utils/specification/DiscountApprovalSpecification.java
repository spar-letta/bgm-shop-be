package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.DiscountApprovalStatus;
import com.app.itibari.models.entities.DiscountApproval;
import com.app.itibari.models.entities.Discounts;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

public class DiscountApprovalSpecification {

    public static Specification<DiscountApproval> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<DiscountApproval> hasSalesAgent(UUID salesAgentPublicId) {

        return (root, query, cb) -> {
            if (salesAgentPublicId == null) {
                return null;
            }
            return cb.equal(root.get("requestedByPublicId"), salesAgentPublicId.toString());
        };
    }

    public static Specification<DiscountApproval> hasCustomer(UUID customerPublicId) {

        return (root, query, cb) -> {
            if (customerPublicId == null) {
                return null;
            }
            return cb.equal(root.get("customerPublicId"), customerPublicId.toString());
        };
    }

    public static Specification<DiscountApproval> hasStatus(DiscountApprovalStatus status) {

        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("approvalStatus"), status);
        };
    }

    public static Specification<DiscountApproval> searchParamMatches(String searchParam) {

        return (root, query, cb) -> {
            if (searchParam == null || searchParam.isEmpty()) {
                return null;
            }
            String likePattern = "%" + searchParam.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("requestedBy")), likePattern),
                    cb.like(cb.lower(root.get("customer")), likePattern)
            );
        };
    }

    public static Specification<DiscountApproval> hasDateFromTo(LocalDateTime startDate, LocalDateTime endDate) {

        return (root, query, cb) -> {
            if (startDate == null || endDate == null) {
                return null;
            }
            return cb.between(root.get("creationTime"), startDate, endDate);
        };
    }
}
