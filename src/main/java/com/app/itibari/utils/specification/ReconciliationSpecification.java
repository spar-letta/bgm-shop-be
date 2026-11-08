package com.app.itibari.utils.specification;

import com.app.itibari.models.entities.Reconciliation;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class ReconciliationSpecification {

    public static Specification<Reconciliation> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<Reconciliation> hasVehicle(String vehicle) {

        if (vehicle == null) {
            return null;
        }

        return (root, query, cb) -> cb.equal(root.get("vehicleNumber"), vehicle);
    }

    public static Specification<Reconciliation> hasDeliveryAgentPublicId(String deliveryAgentPublicId) {

        if (deliveryAgentPublicId == null) {
            return null;
        }

        return (root, query, cb) -> cb.equal(root.get("deliveryAgentPublicId"), deliveryAgentPublicId);
    }

    public static Specification<Reconciliation> hasDeliveryStatus(String deliveryStatus) {

        if (deliveryStatus == null) {
            return null;
        }

        return (root, query, cb) -> cb.equal(root.get("deliveryStatus"), deliveryStatus);
    }

    public static Specification<Reconciliation> isReconciled(Boolean reconciled) {

        if (reconciled == null) {
            return null;
        }

        return (root, query, cb) -> cb.equal(root.get("reconciled"), reconciled);
    }

    public static Specification<Reconciliation> isWalkIn(Boolean walkIn) {

        if (walkIn == null) {
            return null;
        }

        return (root, query, cb) -> cb.equal(root.get("walkIn"), walkIn);
    }

    public static Specification<Reconciliation> hasSearchTerm(String searchTerm) {

        if (searchTerm == null || searchTerm.length() < 4) {
            return null;
        }

        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("vehicleNumber")), "%" + searchTerm.toLowerCase() + "%"),
                cb.like(cb.lower(root.get("deliveryId")), "%" + searchTerm.toLowerCase() + "%"),
                cb.like(cb.lower(root.get("deliveryAgent")), "%" + searchTerm.toLowerCase() + "%")
        );
    }

    public static Specification<Reconciliation> hasDeliveryId(String deliveryId) {

        if (deliveryId == null) {
            return null;
        }

        return (root, query, cb) -> cb.equal(root.get("deliveryId"), deliveryId);
    }

    public static Specification<Reconciliation> hasDateRange(LocalDate from, LocalDate to) {

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
