package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.EntityType;
import com.app.itibari.models.entities.DocumentRule;
import org.springframework.data.jpa.domain.Specification;

public class DocumentRuleSpecification {
    public static Specification<DocumentRule> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<DocumentRule> hasEntityType(EntityType entityType) {
        return (root, query, cb) -> {
            if (entityType == null) {
                return null;
            }
            return cb.equal(root.get("entityType"), entityType);
        };
    }

    public static Specification<DocumentRule> hasEntitySubType(String entitySubType) {
        return (root, query, cb) -> {
            if (entitySubType == null || entitySubType.isEmpty()) {
                return null;
            }
            return cb.equal(cb.lower(root.get("entitySubType")), entitySubType.toLowerCase());
        };
    }
}
