package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.LocationApprovalStatus;
import com.app.itibari.models.entities.LocationApproval;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

public class LocationApprovalSpecification {
    public static Specification<LocationApproval> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<LocationApproval> searchParamMatches(String searchParam) {
        return (root, query, cb) -> {
            if (searchParam == null || searchParam.isEmpty()) {
                return null;
            }
            String likePattern = "%" + searchParam.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("deliveryAgent")), likePattern),
                    cb.like(cb.lower(root.get("customerPhone")), likePattern),
                    cb.like(cb.lower(root.get("customerName")), likePattern)
            );
        };

    }

    public static Specification<LocationApproval> hasStatuses(List<LocationApprovalStatus> statuses) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) {
                return null;
            }

            return root.get("status").in(statuses);
        };
    }

    public static Specification<LocationApproval> hasDeliveryAgent(String deliveryAgent) {
        return (root, query, cb) -> {
            if (deliveryAgent == null || deliveryAgent.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("deliveryAgent"), deliveryAgent);
        };
    }

    public static Specification<LocationApproval> hasCustomer(String customer) {
        return (root, query, cb) -> {
            if (customer == null || customer.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("customerName"), customer);
        };
    }

    public static Specification<LocationApproval> isBetweenDates(String from, String to) {
        return (root, query, cb) -> {
            if (from == null || to == null) {
                return null;
            }
            LocalDate startDate;
            LocalDate endDate;
            try {
                startDate = LocalDate.parse(from);
                endDate = LocalDate.parse(to);
            } catch (Exception e) {
                return null;
            }
            return cb.and(
                    cb.greaterThanOrEqualTo(root.get("creationTime"), startDate.atStartOfDay()),
                    cb.lessThan(root.get("creationTime"), endDate.plusDays(1).atStartOfDay())
            );
        };
    }
}
