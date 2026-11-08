package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.ReconStatus;
import com.app.itibari.models.entities.ProductRecon;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class ProductReconSpecification {

    public static Specification<ProductRecon> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<ProductRecon> hasWarehousePublicId(String warehousePublicId) {

        return (root, query, cb) -> {
            if (warehousePublicId == null) {
                return null;
            }
            return cb.equal(root.get("warehousePublicId"), warehousePublicId);
        };
    }

    public static Specification<ProductRecon> hasDeliveryId(String deliveryId) {

        return ((root, query, criteriaBuilder) -> {
            if (deliveryId == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get("deliveryId"), deliveryId);
        });
    }

    public static Specification<ProductRecon> hasDeliveryAgent(String deliveryAgentPublicId) {

        return (root, query, cb) -> {
            if (deliveryAgentPublicId == null) {
                return null;
            }
            return cb.equal(root.get("deliveryAgentPublicId"), deliveryAgentPublicId);
        };
    }

    public static Specification<ProductRecon> hasVehicleNumber(String vehicleNumber) {

        return (root, query, cb) -> {
            if (vehicleNumber == null) {
                return null;
            }
            return cb.equal(root.get("vehicleNumber"), vehicleNumber);
        };
    }

    public static Specification<ProductRecon> hasReconciled(Boolean reconciled) {

        return (root, query, cb) -> {
            if (reconciled == null) {
                return null;
            }
            return cb.equal(root.get("reconciled"), reconciled);
        };
    }

    public static Specification<ProductRecon> hasSearchParam(String searchParam) {

        return (root, query, cb) -> {
            if (searchParam == null) {
                return null;
            }
            return cb.or(
                    cb.like(cb.lower(root.get("warehouseName")), "%" + searchParam.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("deliveryAgent")), "%" + searchParam.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("deliveryId")), "%" + searchParam.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("vehicleNumber")), "%" + searchParam.toLowerCase() + "%")
            );
        };
    }

    public static Specification<ProductRecon> hasDateBetween(String from, String to) {

        return (root, query, cb) -> {
            if (from == null || to == null) {
                return null;
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return cb.between(
                    root.get("creationTime"),
                    LocalDate.parse(from, formatter).atStartOfDay(),
                    LocalDate.parse(to, formatter).atTime(23, 59, 59, 999999999)
            );
        };
    }

    public static Specification<ProductRecon> hasStatus(ReconStatus status) {

        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<ProductRecon> hasCreatedBy(UUID createdBy) {

        return (root, query, cb) -> {
            if (createdBy == null) {
                return null;
            }
            return cb.equal(root.get("createdBy").get("publicId"), createdBy);
        };
    }

    public static Specification<ProductRecon> isWalkIn(Boolean walkIn) {
        return (root, query, cb) ->
                walkIn == null ? cb.conjunction() : cb.equal(root.get("walkIn"), walkIn);
    }

}
