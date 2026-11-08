package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.VanSaleStatus;
import com.app.itibari.models.entities.VanSales;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VanSalesSpecification {

    public static Specification<VanSales> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<VanSales> hasDeliveryAgent(UUID deliveryAgent) {

        return (root, query, cb) -> {
            if (deliveryAgent == null) {
                return cb.conjunction(); // Always true predicate
            }
            return cb.equal(root.get("deliveryAgents").get("publicId"), deliveryAgent);
        };
    }

    public static Specification<VanSales> hasFromToDates(String from, String to) {

        return (root, query, cb) -> {
            if (from == null || to == null) {
                return null;
            }
            return cb.between(root.get("creationTime"), from, to);
        };
    }

    public static Specification<VanSales> hasStatus(VanSaleStatus status) {

        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<VanSales> searchParamMatches(String searchParam) {

        return (root, query, cb) -> {
            if (searchParam == null || searchParam.trim().length() < 3) {
                return null;
            }

            String formattedSearchParam = "%" + searchParam.toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.like(cb.lower(root.get("deliveryAgents").get("firstName")), formattedSearchParam));
            predicates.add(cb.like(cb.lower(root.get("deliveryAgents").get("lastName")), formattedSearchParam));
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
}
