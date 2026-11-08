package com.app.itibari.utils.specification;

import com.app.itibari.models.entities.QuarantineProducts;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class DisposedQuarantineProductSpecification {
    public static Specification<QuarantineProducts> orderByCreationTimeDesc() {
        return (root, query, cb) -> {
            query.orderBy(cb.desc(root.get("creationTime")));
            return query.getRestriction();
        };
    }

    public static Specification<QuarantineProducts> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<QuarantineProducts> hasSearchTerm(String search) {

        return (root, query, cb) -> {
            if (search == null || search.isEmpty()) {
                return null;
            }
            String searchPattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("productName")), searchPattern),
                    cb.like(cb.lower(root.get("productCode")), searchPattern)
            );
        };
    }

    public static Specification<QuarantineProducts> hasDateRange(LocalDate from, LocalDate to) {

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
}
