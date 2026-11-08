package com.app.itibari.utils.specification;

import com.app.itibari.models.entities.OrderCutOffTime;
import org.springframework.data.jpa.domain.Specification;

public class OrderCutOffSpecification {

    public static Specification<OrderCutOffTime> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<OrderCutOffTime> searchParamMatches(String searchParam) {

        return (root, query, cb) -> {
            if (searchParam == null) {
                return null;
            }
            return cb.like(root.get("warehouse").get("warehouseName"), "%" + searchParam + "%");
        };
    }
}
