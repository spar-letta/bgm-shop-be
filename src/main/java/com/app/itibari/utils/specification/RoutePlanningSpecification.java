package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.RouteStatus;
import com.app.itibari.models.entities.RoutePlanning;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class RoutePlanningSpecification {

    public static Specification<RoutePlanning> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<RoutePlanning> hasVehicle(String vehicle) {

        if (vehicle == null) {
            return null;
        }

        return (root, query, cb) -> cb.equal(root.get("vehicleId"), vehicle);
    }

    // has region
    public static Specification<RoutePlanning> hasRegion(String region) {

        if (region == null) {
            return null;
        }

        return (root, query, cb) -> cb.equal(root.get("routeRegionId"), region);
    }
    // has warehouseId

    public static Specification<RoutePlanning> hasWarehouse(String warehouse) {

        if (warehouse == null) {
            return null;
        }

        return (root, query, cb) -> cb.equal(root.get("warehouseId"), warehouse);
    }

    //deliveryAgent

    public static Specification<RoutePlanning> hasDeliveryAgent(String deliveryAgent) {

        if (deliveryAgent == null) {
            return null;
        }

        return (root, query, cb) -> cb.equal(root.get("deliveryAgentId"), deliveryAgent);
    }

    public static Specification<RoutePlanning> hasRouteWrapper(UUID routeWrapperPublicId) {

        if (routeWrapperPublicId == null) {
            return null;
        }

        return (root, query, cb) -> cb.equal(root.join("routeWrapper").get("publicId"), routeWrapperPublicId);
    }

    // search by warehouseName searchParem

    public static Specification<RoutePlanning> hasWarehouseName(String searchParam) {

        if (searchParam == null) {
            return null;
        }

        return (root, query, cb) -> {
            String pattern = "%" + searchParam.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("warehouseName")), pattern),
                    cb.like(cb.lower(root.get("routeRegionName")), pattern),
                    cb.like(cb.lower(root.get("routeId")), pattern),
                    cb.like(cb.lower(root.get("vehicleName")), pattern),
                    cb.like(cb.lower(root.get("deliveryAgentName")), pattern)
            );
        };
    }

    public static Specification<RoutePlanning> hasDateBetween(String from, String to) {

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

    public static Specification<RoutePlanning> hasStatus(RouteStatus status) {

        if (status == null) {
            return null;
        }

        return (root, query, cb) -> cb.equal(root.get("routeStatus"), status);
    }

    public static Specification<RoutePlanning> hasDate(String date) {

        return (root, query, cb) -> {
            if (date == null) {
                return null;
            }
            return cb.equal(root.get("creationTime").as(LocalDate.class), LocalDate.parse(date));
        };
    }
}
