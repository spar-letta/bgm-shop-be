package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.LoadingStatusEnum;
import com.app.itibari.dataTypes.PaymentStatusEnum;
import com.app.itibari.dataTypes.SourceChannelEnum;
import com.app.itibari.models.entities.SalesOrders;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class SalesOrdersSpecification {

    public static Specification<SalesOrders> hasSalesOrderNumber(String salesOrderNumber) {

        return (root, query, cb) -> {
            if (salesOrderNumber == null || salesOrderNumber.trim().isEmpty()) {
                return null;
            }
            return cb.like(cb.upper(root.get("salesOrderNumber")), "%" + salesOrderNumber.toUpperCase() + "%");
        };
    }

    public static Specification<SalesOrders> hasCustomer(UUID customerId) {

        return (root, query, cb) -> {
            if (customerId == null) {
                return null;
            }
            return cb.equal(root.get("customer").get("publicId"), customerId);
        };
    }

    public static Specification<SalesOrders> hasWarehouse(UUID warehouseId) {

        return (root, query, cb) -> {
            if (warehouseId == null) {
                return null;
            }
            return cb.equal(root.get("warehouse").get("publicId"), warehouseId);
        };
    }

    public static Specification<SalesOrders> hasOrganizationRegion(UUID organizationRegionId) {

        return (root, query, cb) -> {
            if (organizationRegionId == null) {
                return null;
            }
            return cb.equal(root.get("organizationRegions").get("publicId"), organizationRegionId);
        };
    }

    public static Specification<SalesOrders> hasSourceChannel(SourceChannelEnum sourceChannel) {

        return (root, query, cb) -> {
            if (sourceChannel == null) {
                return null;
            }
            return cb.equal(root.get("sourceChannel"), sourceChannel);
        };
    }

    public static Specification<SalesOrders> hasSalesOrderStatus(LoadingStatusEnum salesOrderStatus) {

        return (root, query, cb) -> {
            if (salesOrderStatus == null) {
                return null;
            }
            return cb.equal(root.get("salesOrderStatus"), salesOrderStatus);
        };
    }

    public static Specification<SalesOrders> hasPaymentStatus(PaymentStatusEnum paymentStatus) {

        return (root, query, cb) -> {
            if (paymentStatus == null) {
                return null;
            }
            return cb.equal(root.get("paymentStatus"), paymentStatus);
        };
    }

    public static Specification<SalesOrders> hasFromToDates(LocalDate startDate, LocalDate endDate) {

        return (root, query, cb) -> {
            if (startDate == null || endDate == null) {
                return null;
            }
            return cb.and(
                    cb.greaterThanOrEqualTo(root.get("creationTime"), startDate.atStartOfDay()),
                    cb.lessThan(root.get("creationTime"), endDate.plusDays(1).atStartOfDay())
            );
        };
    }

    public static Specification<SalesOrders> hasInvoiceNumber(String invoiceNumber) {

        return (root, query, cb) -> {
            if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
                return null;
            }
            return cb.like(cb.upper(root.get("invoiceNumber")), "%" + invoiceNumber.toUpperCase() + "%");
        };
    }

    public static Specification<SalesOrders> searchParamMatches(String searchParam) {

        return (root, query, cb) -> {
            if (searchParam == null || searchParam.trim().length() < 3) {
                return null;
            }
            return cb.or(
                    cb.like(cb.upper(root.get("salesOrderNumber")), "%" + searchParam.toUpperCase() + "%"),
                    cb.like(cb.upper(root.get("invoiceNumber")), "%" + searchParam.toUpperCase() + "%"),
                    cb.like(cb.upper(root.get("customer").get("name")), "%" + searchParam.toUpperCase() + "%"),
                    cb.like(cb.upper(root.get("bookedBy")), "%" + searchParam.toUpperCase() + "%"),
                    cb.like(cb.upper(root.get("deliveryAgent").get("firstName")), "%" + searchParam.toUpperCase() + "%"),
                    cb.like(cb.upper(root.get("deliveryAgent").get("lastName")), "%" + searchParam.toUpperCase() + "%")
            );
        };
    }

    public static Specification<SalesOrders> hasStringDateFromTo(String startDate, String endDate) {

        return (root, query, cb) -> {
            if (startDate == null || endDate == null) {
                return null;
            }
            return cb.between(root.get("creationTime"), LocalDateTime.parse(startDate), LocalDateTime.parse(endDate));
        };
    }

    public static Specification<SalesOrders> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<SalesOrders> hasLocation(UUID location) {

        return (root, query, cb) -> {
            if (location == null) {
                return null;
            }
            return cb.equal(root.get("customer").get("organizationLocation").get("publicId"), location);
        };
    }

    public static Specification<SalesOrders> bookedBySalesAgent(Boolean bookedBySalesAgent) {

        return (root, query, cb) -> {
            if (bookedBySalesAgent == null) {
                return null;
            }
            return cb.equal(root.get("bookedBySalesAgent"), bookedBySalesAgent);
        };
    }

    public static Specification<SalesOrders> hasSalesAgent(UUID salesAgentPublicId) {

        return (root, query, cb) -> {
            if (salesAgentPublicId == null) {
                return null;
            }
            return cb.equal(root.get("salesAgentPublicId"), salesAgentPublicId.toString());
        };
    }

    public static Specification<SalesOrders> hasDeliveryAgent(UUID deliveryAgentPublicId) {
        return (root, query, cb) -> {
            if (deliveryAgentPublicId == null) {
                return null;
            }
            return cb.equal(root.get("deliveryAgent").get("publicId"), deliveryAgentPublicId);
        };
    }

    public static Specification<SalesOrders> hasStatuses(List<LoadingStatusEnum> statuses) {

        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) {
                return null;
            }
            return root.get("salesOrderStatus").in(statuses);
        };
    }

    public static Specification<SalesOrders> isCredit(Boolean isCredit) {
        return (root, query, cb) -> {
            if (isCredit == null) {
                return null;
            }
            return cb.equal(root.get("creditApplied"), isCredit);
        };
    }

    public static Specification<SalesOrders> hasParentCustomer(UUID parentCustomerPublicId) {
        return (root, query, cb) -> {
            if (parentCustomerPublicId == null) {
                return null;
            }

            Join<Object, Object> customerJoin = root.join("customer", JoinType.LEFT);
            Join<Object, Object> parentCustomerJoin = customerJoin.join("parentCustomer", JoinType.LEFT);

            return cb.or(
                    cb.equal(customerJoin.get("publicId"), parentCustomerPublicId),
                    cb.equal(parentCustomerJoin.get("publicId"), parentCustomerPublicId)
            );
        };
    }

    public static Specification<SalesOrders> isWalkIn(Boolean isWalkIn) {
        return (root, query, cb) -> {
            if (isWalkIn == null) {
                return null;
            }
            return cb.equal(root.get("walkIn"), isWalkIn);
        };
    }

    public static Specification<SalesOrders> isBookedByParent(Boolean bookedByParent) {
        return (root, query, cb) -> {
            if (bookedByParent == null) {
                return null;
            }
            return cb.equal(root.get("bookedByParent"), bookedByParent);
        };
    }
}
