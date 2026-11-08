package com.app.itibari.utils.specification;

import com.app.itibari.models.entities.Vehicle;
import org.springframework.data.jpa.domain.Specification;

public class VehicleSpecification {

    public static Specification<Vehicle> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<Vehicle> searchParamMatches(String searchParam) {

        return (root, query, cb) -> {
            if (searchParam == null) {
                return null;
            }
            return cb.or(
                    cb.like(cb.lower(root.get("numberPlate")), "%" + searchParam.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("assignedDriver")), "%" + searchParam.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("owner")), "%" + searchParam.toLowerCase() + "%")
            );
        };
    }
}
