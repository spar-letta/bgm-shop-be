package com.app.itibari.utils.specification;

import com.app.itibari.models.entities.CallStatus;
import org.springframework.data.jpa.domain.Specification;

public class CallStatusSpecification {

    public static Specification<CallStatus> isNotDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static Specification<CallStatus> isActive(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) {
                return null;
            }
            return cb.equal(root.get("active"), active);
        };
    }

    public static Specification<CallStatus> searchParamMatches(String searchParam) {
        return (root, query, cb) -> {
            if (searchParam == null || searchParam.trim().length() < 3) {
                return null;
            }
            String pattern = "%" + searchParam.toUpperCase() + "%";
            return cb.or(
                    cb.like(cb.upper(root.get("code")), pattern),
                    cb.like(cb.upper(root.get("description")), pattern)
            );
        };
    }
}
