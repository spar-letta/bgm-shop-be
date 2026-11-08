package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.Module;
import com.app.itibari.models.entities.RejectionsCode;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class RejectionsCodeSpecification {
    public static Specification<RejectionsCode> isNotDeleted() {

        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static Specification<RejectionsCode> hasModule(Module module) {

        return (root, query, cb) -> {
            if (module == null) {
                return null;
            }
            return cb.equal(root.get("module"), module);
        };
    }

    public static Specification<RejectionsCode> searchParamMatches(String searchParam) {

        return (root, query, cb) -> {
            if (searchParam == null || searchParam.trim().length() < 3) {
                return null;
            }
            return cb.or(
                    cb.like(cb.upper(root.get("code")), "%" + searchParam.toUpperCase() + "%"),
                    cb.like(cb.upper(root.get("description")), "%" + searchParam.toUpperCase() + "%")
            );
        };
    }

    public static Specification<RejectionsCode> hasDepartment(UUID departmentPublicId) {
        return (root, query, cb) -> {
            if (departmentPublicId == null) {
                return null;
            }
            return cb.equal(root.get("department").get("publicId"), departmentPublicId);
        };
    }
}
