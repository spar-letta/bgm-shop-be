package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.StockMovementTransactionType;
import com.app.itibari.models.entities.StockMovementItem;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class StockMovementSpecification {
    public static Specification<StockMovementItem> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<StockMovementItem> belongsToWarehouse(UUID warehousePublicId) {
        return (root, query, cb) -> {
            if (warehousePublicId == null) {
                return null;
            }
            return cb.equal(root.get("warehousePublicId"), warehousePublicId.toString());
        };
    }

    public static Specification<StockMovementItem> withinDateRange(LocalDate from, LocalDate to) {
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

    public static Specification<StockMovementItem> searchParamMatches(String searchParam) {

        return (root, query, cb) -> {
            if (searchParam == null || searchParam.isEmpty()) {
                return null;
            }
            String likePattern = "%" + searchParam.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("productName")), likePattern),
                    cb.like(cb.lower(root.get("productSku")), likePattern)
            );
        };
    }

    public static Specification<StockMovementItem> hasTransactionType(StockMovementTransactionType transactionType) {
        return (root, query, cb) -> {
            if (transactionType == null) {
                return null;
            }
            return cb.equal(root.get("transactionType"), transactionType);
        };
    }

    public static Specification<StockMovementItem> hasTransactionTypes(List<StockMovementTransactionType> transactionTypes) {
        return (root, query, cb) -> {
            if (transactionTypes == null || transactionTypes.isEmpty()) {
                return null;
            }
            return root.get("transactionType").in(transactionTypes);
        };
    }
}
