package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.CustomerReturnStatus;
import com.app.itibari.dataTypes.GoodsReturnStatus;
import com.app.itibari.models.entities.CustomerReturn;
import com.app.itibari.models.entities.GoodsReceivedNote;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public class CustomerReturnSpecification {
    public static Specification<CustomerReturn> orderByCreationTimeDesc() {
        return (root, query, cb) -> {
            query.orderBy(cb.desc(root.get("creationTime")));
            return query.getRestriction();
        };
    }

    public static Specification<CustomerReturn> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<CustomerReturn> hasDeliveryAgent(UUID deliveryAgentPublicId) {

        return (root, query, cb) -> {
            if (deliveryAgentPublicId == null) {
                return null;
            }
            return cb.equal(root.get("deliveryAgent").get("publicId"), deliveryAgentPublicId);
        };
    }

    public static Specification<CustomerReturn> hasStatus(CustomerReturnStatus status) {

        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<CustomerReturn> hasSearchTerm(String search) {

        return (root, query, cb) -> {
            if (search == null || search.isEmpty()) {
                return null;
            }
            String searchPattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("salesOrder").get("salesOrderNumber")), searchPattern)
            );
        };
    }

    public static Specification<CustomerReturn> hasDateRange(LocalDate from, LocalDate to) {

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
}
