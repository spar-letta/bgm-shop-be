package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.ReferenceType;
import com.app.itibari.models.entities.FileData;
import org.springframework.data.jpa.domain.Specification;

public class FileDataSpecification {
    public static Specification<FileData> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<FileData> hasReferenceId(String referenceId) {
        return (root, query, cb) -> {
            if (referenceId == null || referenceId.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("referenceId"), referenceId);
        };
    }

    public static Specification<FileData> hasReferenceType(ReferenceType referenceType) {
        return (root, query, cb) -> {
            if (referenceType == null) {
                return null;
            }
            return cb.equal(root.get("referenceType"), referenceType.name());
        };
    }

    public static Specification<FileData> hasReceipt(String receipt) {
        return (root, query, cb) -> {
            if (receipt == null || receipt.isEmpty()) {
                return null;
            }
            return cb.equal(cb.lower(root.get("receipt")), receipt.toLowerCase());
        };
    }

    public static Specification<FileData> hasDescription(String orderNumber) {
        return (root, query, cb) -> {
            if (orderNumber == null || orderNumber.isEmpty()) {
                return null;
            }
            return cb.equal(cb.lower(root.get("description")), orderNumber.toLowerCase());
        };
    }
}
