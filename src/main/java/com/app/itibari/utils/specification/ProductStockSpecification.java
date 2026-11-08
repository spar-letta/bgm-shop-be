package com.app.itibari.utils.specification;

import com.app.itibari.models.entities.ProductStock;
import com.app.itibari.models.entities.StockMovementItem;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public class ProductStockSpecification {

    public static Specification<ProductStock> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<ProductStock> belongsToWarehouse(UUID warehousePublicId) {
        return (root, query, cb) -> {
            if (warehousePublicId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.join("warehouse").get("publicId"), warehousePublicId);
        };
    }


    public static Specification<ProductStock> withinDateRange(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return null;
            }
            if (from != null && to != null) {
                return cb.between(root.get("recordToday"), from, to);
            } else if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("recordToday"), from);
            } else {
                return cb.lessThanOrEqualTo(root.get("recordToday"), to);
            }
        };
    }

    public static Specification<ProductStock> searchParamMatches(String searchParam) {

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

    public static Specification<ProductStock> isActiveStatus(boolean isActive) {

        return (root, query, cb) -> cb.equal(root.get("product").get("inActive"), isActive);
    }
}
