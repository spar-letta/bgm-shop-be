package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.CreditNoteStatus;
import com.app.itibari.models.entities.CreditLimitBatches;
import com.app.itibari.models.entities.CreditNote;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class CreditNoteSpecification {
    public static Specification<CreditNote> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<CreditNote> hasSupplier(UUID supplierId) {

        return (root, query, cb) -> {
            if (supplierId == null) {
                return null;
            }
            return cb.equal(root.get("supplier").get("publicId"), supplierId);
        };
    }


    public static Specification<CreditNote> hasFromToDates(LocalDate fromDate, LocalDate toDate) {

        return (root, query, cb) -> {
            if (fromDate == null || toDate == null) {
                return null;
            }
            return cb.between(root.get("creditNoteDate"), fromDate, toDate);
        };
    }


    public static Specification<CreditNote> hasStatuses(List<CreditNoteStatus> statuses) {

        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) {
                return null;
            }
            return root.get("status").in(statuses);
        };
    }

    public static Specification<CreditNote> isTagged(Boolean tagged) {

        return (root, query, cb) -> {
            if (tagged == null) {
                return null;
            }
            return cb.equal(root.get("tagged"), tagged);
        };
    }

    public static Specification<CreditNote> hasSearchParam(String searchParam) {
        return (root, query, cb) -> {
            if (searchParam == null) {
                return null;
            }
            return cb.or(
                    cb.like(cb.lower(root.get("creditNoteNumber")), "%" + searchParam.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("supplier").get("supplierName")), "%" + searchParam.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("status")), "%" + searchParam.toLowerCase() + "%")
            );
        };
    }

    public static Specification<CreditNote> hasPurchaseOrder(UUID purchaseOrderPublicId) {
        return (root, query, cb) -> {
            if (purchaseOrderPublicId == null) {
                return null;
            }
            return cb.equal(root.get("taggedPurchaseOrderPublicId"), purchaseOrderPublicId.toString());
        };
    }

    public static Specification<CreditNote> hasOriginalPurchaseOrder(UUID purchaseOrderPublicId) {
        return (root, query, cb) -> {
            if (purchaseOrderPublicId == null) {
                return null;
            }
            return cb.equal(root.get("purchaseOrderPublicId"), purchaseOrderPublicId.toString());
        };
    }
}
