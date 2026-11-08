package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.TransactionType;
import com.app.itibari.models.entities.AvcoItems;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public class AvcoSpecification {

    public static Specification<AvcoItems> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<AvcoItems> hasOrderNumber(String orderNumber) {

        return (root, query, cb) -> {
            if (orderNumber == null) {
                return null;
            }
            return cb.equal(root.get("purchaseOrderNumber"), orderNumber);
        };
    }

    public static Specification<AvcoItems> hasProduct(UUID productId) {

        return (root, query, cb) -> {
            if (productId == null) {
                return null;
            }
            return cb.equal(root.get("productPublicId"), productId.toString());
        };
    }

    public static Specification<AvcoItems> hasTransactionType(TransactionType transactionType) {

        return (root, query, cb) -> {
            if (transactionType == null) {
                return null;
            }
            return cb.equal(root.get("transactionType"), transactionType);
        };
    }

    public static Specification<AvcoItems> hasFromToDates(LocalDate fromDate, LocalDate toDate) {

        return (root, query, cb) -> {
            if (fromDate == null || toDate == null) {
                return null;
            }
            return cb.between(root.get("transactionDate"), fromDate, toDate);
        };
    }

    public static Specification<AvcoItems> searchParamMatches(String searchParam) {

        return (root, query, cb) -> {
            if (searchParam == null) {
                return null;
            }
            return cb.or(
                    cb.like(cb.lower(root.get("purchaseOrderNumber")), "%" + searchParam.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("productSku")), "%" + searchParam.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("productName")), "%" + searchParam.toLowerCase() + "%")
            );
        };
    }
}
