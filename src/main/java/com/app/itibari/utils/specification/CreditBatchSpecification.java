package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.BatchStatus;
import com.app.itibari.models.entities.CreditLimitBatches;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class CreditBatchSpecification {
    public static Specification<CreditLimitBatches> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<CreditLimitBatches> hasSearchParam(String searchParam) {
        return (root, query, cb) -> {
            if (searchParam == null) {
                return null;
            }
            return cb.or(
                    cb.like(cb.lower(root.get("parentCustomer").get("phoneNumber")), "%" + searchParam.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("parentCustomer").get("name")), "%" + searchParam.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("batchNumber")), "%" + searchParam.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("batchStatus")), "%" + searchParam.toLowerCase() + "%")
            );
        };
    }

    public static Specification<CreditLimitBatches> hasStatuses(List<BatchStatus> statuses) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) {
                return null;
            }
            return root.get("batchStatus").in(statuses);
        };
    }

    public static Specification<CreditLimitBatches> hasFromToDates(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return null;
            }
            if (from != null && to != null) {
                return cb.between(root.get("creationTime"), from.atStartOfDay(), to.atTime(23, 59, 59));
            } else if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("creationTime"), from.atStartOfDay());
            } else {
                return cb.lessThanOrEqualTo(root.get("creationTime"), to.atTime(23, 59, 59));
            }
        };
    }

    public static Specification<CreditLimitBatches> hasParentCustomer(UUID parentCustomerPublicId) {
        return (root, query, cb) -> {
            if (parentCustomerPublicId == null) {
                return null;
            }
            return cb.equal(root.get("parentCustomer").get("publicId"), parentCustomerPublicId);
        };
    }
}
