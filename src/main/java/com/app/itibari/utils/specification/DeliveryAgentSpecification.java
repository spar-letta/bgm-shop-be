package com.app.itibari.utils.specification;

import com.app.itibari.models.entities.DeliveryAgents;
import org.springframework.data.jpa.domain.Specification;

public class DeliveryAgentSpecification {

    public static Specification<DeliveryAgents> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<DeliveryAgents> searchParamMatches(String searchParam) {

        return (root, query, cb) -> {
            if (searchParam == null || searchParam.trim().isEmpty()) {
                return cb.conjunction();
            }

            String formattedSearchParam = "%" + searchParam.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("firstName")), formattedSearchParam),
                    cb.like(cb.lower(root.get("lastName")), formattedSearchParam),
                    cb.like(cb.lower(root.get("phoneNumber")), formattedSearchParam)
            );
        };
    }
}
