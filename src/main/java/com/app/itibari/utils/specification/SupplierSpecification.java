package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.SupplierStatus;
import com.app.itibari.models.entities.Suppliers;
import org.springframework.data.jpa.domain.Specification;

public class SupplierSpecification {

    public static Specification<Suppliers> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<Suppliers> searchParamMatches(String searchParam) {

        return (root, query, cb) -> {
            if (searchParam == null || searchParam.trim().isEmpty()) {
                return cb.conjunction(); // Return true predicate for empty input
            }

            String formattedSearchParam = "%" + searchParam.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("supplierName")), formattedSearchParam),
                    cb.like(cb.lower(root.get("tinNumber")), formattedSearchParam)
            );
        };
    }

    public static Specification<Suppliers> hasStatus(SupplierStatus supplierStatus) {

        return (root, query, cb) -> {
            if (supplierStatus == null) {
                return cb.conjunction(); // Return true predicate for null status
            }
            return cb.equal(root.get("status"), supplierStatus);
        };
    }
}
