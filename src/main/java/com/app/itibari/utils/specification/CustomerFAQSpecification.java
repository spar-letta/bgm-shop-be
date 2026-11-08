package com.app.itibari.utils.specification;

import com.app.itibari.models.entities.CustomerFAQ;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class CustomerFAQSpecification {
    public static Specification<CustomerFAQ> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<CustomerFAQ> searchParamMatches(String searchParam) {

        return (root, query, cb) -> {
            if (searchParam == null || searchParam.trim().isEmpty()) {
                return cb.conjunction();
            }

            String formattedSearchParam = "%" + searchParam.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("department").get("name")), formattedSearchParam)
            );
        };
    }

    public static Specification<CustomerFAQ> hasDateRange(LocalDate from, LocalDate to) {

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
