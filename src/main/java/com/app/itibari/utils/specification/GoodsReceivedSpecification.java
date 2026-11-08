package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.GoodsReturnStatus;
import com.app.itibari.dataTypes.PurchaseOrderStatus;
import com.app.itibari.models.entities.GoodsReceivedNote;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public class GoodsReceivedSpecification {

    public static Specification<GoodsReceivedNote> orderByCreationTimeDesc() {
        return (root, query, cb) -> {
            query.orderBy(cb.desc(root.get("creationTime")));
            return query.getRestriction();
        };
    }

    public static Specification<GoodsReceivedNote> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<GoodsReceivedNote> hasSupplier(UUID supplierId) {

        return (root, query, cb) -> {
            if (supplierId == null) {
                return null;
            }
            return cb.equal(root.get("supplier").get("publicId"), supplierId);
        };
    }

    public static Specification<GoodsReceivedNote> hasPurchaseOrder(String purchaseOrderNumber) {

        return (root, query, cb) -> {
            if (purchaseOrderNumber == null) {
                return null;
            }

            return cb.equal(root.get("purchaseOrderNumber"), purchaseOrderNumber);
        };
    }

    public static Specification<GoodsReceivedNote> hasStatus(GoodsReturnStatus status) {

        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<GoodsReceivedNote> hasSearchTerm(String search) {

        return (root, query, cb) -> {
            if (search == null || search.isEmpty()) {
                return null;
            }
            String searchPattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("purchaseOrderNumber")), searchPattern),
                    cb.like(cb.lower(root.get("noteNumber")), searchPattern),
                    cb.like(cb.lower(root.get("deliveryNoteNumber")), searchPattern)
            );
        };
    }

    public static Specification<GoodsReceivedNote> hasDateRange(LocalDate from, LocalDate to) {

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

    public static Specification<GoodsReceivedNote> hasHasPoStatus(PurchaseOrderStatus poStatus) {
        return (root, query, cb) -> {
            if (poStatus == null) {
                return null;
            }
            return cb.equal(root.get("purchaseOrderStatus"), poStatus);
        };
    }
}
