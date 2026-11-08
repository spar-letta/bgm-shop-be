package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.RemittanceStatus;
import com.app.itibari.models.entities.Remittances;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class RemittanceSpecification {

    public static Specification<Remittances> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<Remittances> hasSupplier(UUID supplierId) {

        return (root, query, cb) -> {
            if (supplierId == null) {
                return null;
            }
            return cb.equal(root.get("supplier").get("publicId"), supplierId);
        };
    }

    public static Specification<Remittances> hasStatus(RemittanceStatus status) {

        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Remittances> hasOrderNumber(String orderNumber) {

        return (root, query, cb) -> {
            if (orderNumber == null) {
                return null;
            }
            return cb.equal(root.get("purchaseOrderNumber"), orderNumber);
        };
    }

    public static Specification<Remittances> hasPaymentMethod(String paymentMethod) {

        return (root, query, cb) -> {
            if (paymentMethod == null) {
                return null;
            }
            return cb.equal(root.get("paymentMethod"), paymentMethod);
        };
    }

    public static Specification<Remittances> hasInvoiceNumber(String invoiceNumber) {

        return (root, query, cb) -> {
            if (invoiceNumber == null) {
                return null;
            }
            return cb.equal(root.get("invoiceNumber"), invoiceNumber);
        };
    }

    public static Specification<Remittances> hasFromToDates(String from, String to) {

        return (root, query, cb) -> {
            if (from == null || to == null) {
                return null;
            }
            return cb.between(root.get("creationTime"), from, to);
        };
    }
}
