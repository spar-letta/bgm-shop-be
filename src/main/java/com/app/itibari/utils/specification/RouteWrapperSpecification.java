package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.WrapperStatus;
import com.app.itibari.models.entities.RouteWrapper;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class RouteWrapperSpecification {

    public static Specification<RouteWrapper> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<RouteWrapper> hasRouteWrapperId(String searchParam) {

        if (searchParam == null) {
            return null;
        }

        return (root, query, cb) -> cb.like(cb.lower(root.get("routeWrapperId")),
                "%" + searchParam.toLowerCase() + "%");
    }

    public static Specification<RouteWrapper> hasDateBetween(String from, String to) {

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

    public static Specification<RouteWrapper> hasDate(String date) {

        return (root, query, cb) -> {
            if (date == null) {
                return null;
            }
            return cb.equal(root.get("creationTime").as(LocalDate.class), LocalDate.parse(date));
        };
    }

    public static Specification<RouteWrapper> hasStatus(WrapperStatus status) {

        if (status == null) {
            return null;
        }

        return (root, query, cb) -> cb.equal(root.get("wrapperStatus"), status);
    }
}
