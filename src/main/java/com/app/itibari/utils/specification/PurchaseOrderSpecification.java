package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.PurchaseOrderStatus;
import com.app.itibari.models.entities.PurchaseOrder;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class PurchaseOrderSpecification {

    public static Specification<PurchaseOrder> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<PurchaseOrder> hasSupplier(UUID supplierId) {

        return (root, query, cb) -> {
            if (supplierId == null) {
                return null;
            }
            return cb.equal(root.get("supplier").get("publicId"), supplierId);
        };
    }

    public static Specification<PurchaseOrder> hasStatus(PurchaseOrderStatus status) {

        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<PurchaseOrder> hasOrderNumber(String orderNumber) {

        return (root, query, cb) -> {
            if (orderNumber == null) {
                return null;
            }
            return cb.equal(root.get("purchaseOrderNumber"), orderNumber);
        };
    }

    public static Specification<PurchaseOrder> hasFromToDates(LocalDate fromDate, LocalDate toDate) {

        return (root, query, cb) -> {
            if (fromDate == null || toDate == null) {
                return null;
            }
            return cb.between(root.get("purchaseOrderDate"), fromDate, toDate);
        };
    }

    public static Specification<PurchaseOrder> searchParamMatches(String searchParam) {

        return (root, query, cb) -> {
            if (searchParam == null) {
                return null;
            }
            return cb.or(
                    cb.like(cb.lower(root.get("purchaseOrderNumber")), "%" + searchParam.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("supplier").get("supplierName")), "%" + searchParam.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("status")), "%" + searchParam.toLowerCase() + "%")
            );
        };
    }

    public static Specification<PurchaseOrder> hasStatuses(List<PurchaseOrderStatus> statuses) {

        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) {
                return null;
            }
            return root.get("status").in(statuses);
        };
    }

    public static Specification<PurchaseOrder> isRebate(Boolean rebate) {

        return (root, query, cb) -> {
            if (rebate == null) {
                return null;
            }
            return cb.equal(root.get("rebate"), rebate);
        };
    }

    public static Specification<PurchaseOrder> isReplacement(Boolean replacement) {
        return (root, query, cb) -> {
            if (replacement == null) {
                return null;
            }
            return cb.equal(root.get("replacementPurchaseOrder"), replacement);
        };
    }
}
