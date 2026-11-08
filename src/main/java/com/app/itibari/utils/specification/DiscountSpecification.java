package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.DiscountStatus;
import com.app.itibari.dataTypes.DiscountType;
import com.app.itibari.models.entities.Discounts;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class DiscountSpecification {

    public static Specification<Discounts> isNotDeleted() {

        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static Specification<Discounts> hasDiscountType(DiscountType discountType) {

        return (root, query, cb) -> {
            if (discountType == null) {
                return null;
            }
            return cb.equal(root.get("discountType"), discountType);
        };
    }

    public static Specification<Discounts> hasStatus(DiscountStatus status) {

        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Discounts> hasDateFromTo(LocalDateTime startDate, LocalDateTime endDate) {

        return (root, query, cb) -> {
            if (startDate == null || endDate == null) {
                return null;
            }
            return cb.between(root.get("creationTime"), startDate, endDate);
        };
    }

    public static Specification<Discounts> searchParamMatches(String searchParam) {

        return (root, query, cb) -> {
            if (searchParam == null || searchParam.trim().length() < 3) {
                return null;
            }
            return cb.or(
                    cb.like(cb.upper(root.get("discountName")), "%" + searchParam.toUpperCase() + "%")
//                    cb.like(cb.upper(root.get("invoiceNumber")), "%" + searchParam.toUpperCase() + "%"),
            );
        };
    }
}
