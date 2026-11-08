package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.CustomerActiveStatus;
import com.app.itibari.dataTypes.CustomerType;
import com.app.itibari.dataTypes.LoadingStatusEnum;
import com.app.itibari.dataTypes.OrderType;
import com.app.itibari.dataTypes.SourceChannelEnum;
import com.app.itibari.models.entities.BnplLead;
import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.entities.SalesAgents;
import com.app.itibari.models.entities.SalesOrders;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class CustomerSpecification {

    public static Specification<Customers> hasNameLike(String name) {

        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return null;
            }
            return cb.like(cb.upper(root.get("name")), "%" + name.toUpperCase() + "%");
        };
    }

    public static Specification<Customers> hasEmailLike(String email) {

        return (root, query, cb) -> {
            if (email == null || email.trim().isEmpty()) {
                return null;
            }
            return cb.like(cb.upper(root.get("email")), "%" + email.toUpperCase() + "%");
        };
    }

    public static Specification<Customers> hasNationalIdLike(String nationalId) {

        return (root, query, cb) -> {
            if (nationalId == null || nationalId.trim().isEmpty()) {
                return null;
            }
            return cb.like(cb.upper(root.get("nationalId")), "%" + nationalId.toUpperCase() + "%");
        };
    }

    public static Specification<Customers> hasPhoneNumberLike(String phoneNumber) {

        return (root, query, cb) -> {
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return null;
            }
            return cb.like(cb.upper(root.get("phoneNumber")), "%" + phoneNumber.toUpperCase() + "%");
        };
    }

    public static Specification<Customers> hasSourceChannel(SourceChannelEnum sourceChannel) {

        return (root, query, cb) -> {
            if (sourceChannel == null) {
                return null;
            }
            return cb.equal(root.get("sourceChannel"), sourceChannel);
        };
    }

    public static Specification<Customers> hasOrganizationLocation(UUID organizationLocationId) {

        return (root, query, cb) -> {
            if (organizationLocationId == null) {
                return null;
            }
            return cb.equal(root.get("organizationLocation").get("publicId"), organizationLocationId);
        };
    }

    public static Specification<Customers> hasSalesAgent(UUID salesAgentId) {

        return (root, query, cb) -> {
            if (salesAgentId == null) {
                return null;
            }
            return cb.equal(root.get("agentPublicId"), salesAgentId);
        };
    }

    public static Specification<Customers> registrationDateBetween(LocalDate startDate, LocalDate endDate) {

        return (root, query, cb) -> {
            if (startDate == null || endDate == null) {
                return null;
            }
            return cb.between(root.get("registrationDate"), startDate, endDate);
        };
    }

    // New searchParamMatches method for flexible search across name, email, phoneNumber, and nationalId
    public static Specification<Customers> searchParamMatches(String searchParam) {

        return (root, query, cb) -> {
            if (searchParam == null || searchParam.trim().length() < 3) {
                return null;
            }
            String searchParamUpper = "%" + searchParam.toUpperCase() + "%";
            return cb.or(
                    cb.like(cb.upper(root.get("name")), searchParamUpper),
                    cb.like(cb.upper(root.get("email")), searchParamUpper),
                    cb.like(cb.upper(root.get("phoneNumber")), searchParamUpper),
                    cb.like(cb.upper(root.get("shopKeeperNumber")), searchParamUpper),
//                    cb.like(cb.upper(root.get("registeredBy")), searchParamUpper),
//                    cb.like(cb.upper(root.get("nationalId")), searchParamUpper),
                    cb.like(cb.upper(root.get("firstName")), searchParamUpper),
                    cb.like(cb.upper(root.get("lastName")), searchParamUpper),
                    cb.like(cb.upper(root.get("storeName")), searchParamUpper)
            );
        };
    }

    public static Specification<Customers> hasOrganizationRegion(UUID organizationRegionId) {

        return (root, query, cb) -> {
            if (organizationRegionId == null) {
                return null;
            }
            return cb.equal(root.get("organizationRegions").get("publicId"), organizationRegionId);
        };
    }

    public static Specification<Customers> hasRegisteredBy(UUID registeredBy) {

        return (root, query, cb) -> {
            if (registeredBy == null) {
                return null;
            }
            return cb.equal(root.get("createdBy").get("publicId"), registeredBy);
        };
    }

    public static Specification<Customers> hasOrganizationLocations(List<UUID> locations) {

        return (root, query, cb) -> {
            if (locations == null || locations.isEmpty()) {
                return null;
            }
            return root.get("organizationLocation").get("publicId").in(locations);
        };
    }

    public static Specification<Customers> isNotDeleted() {

        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static Specification<Customers> hasServedBy(SalesAgents salesAgents) {
        return (root, query, cb) -> {
            if (salesAgents == null) {
                return null;
            }
            return cb.equal(root.get("servedBy"), salesAgents);
        };
    }

    public static Specification<Customers> isActive(boolean active) {
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }


    public static Specification<Customers> hasCustomerType(CustomerType customerType) {
        if(customerType == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("customerType"), customerType);
    }

    public static Specification<Customers> hasPublicId(UUID customerPublicId) {
        return (root, query, cb) -> {
            if (customerPublicId == null) {
                return null;
            }
            return cb.equal(root.get("publicId"), customerPublicId);
        };
    }

    public static Specification<Customers> hasEverPlacedCreditOrderButNoRecentCreditOrder(LocalDateTime cutoffDateTime) {
        return (root, query, cb) -> {
            var creditOrderExists = query.subquery(Long.class);
            var creditOrder = creditOrderExists.from(SalesOrders.class);
            creditOrderExists.select(cb.literal(1L))
                    .where(
                            cb.equal(creditOrder.get("customer"), root),
                            cb.isFalse(creditOrder.get("deleted")),
                            cb.equal(creditOrder.get("orderType"), OrderType.CREDIT)
                    );

            var recentCreditOrderExists = query.subquery(Long.class);
            var recentCreditOrder = recentCreditOrderExists.from(SalesOrders.class);
            recentCreditOrderExists.select(cb.literal(1L))
                    .where(
                            cb.equal(recentCreditOrder.get("customer"), root),
                            cb.isFalse(recentCreditOrder.get("deleted")),
                            cb.equal(recentCreditOrder.get("orderType"), OrderType.CREDIT),
                            cb.greaterThanOrEqualTo(recentCreditOrder.get("creationTime"), cutoffDateTime)
                    );

            return cb.and(cb.exists(creditOrderExists), cb.not(cb.exists(recentCreditOrderExists)));
        };
    }

    public static Specification<Customers> hasNotOnboardedCustomerWithThreeOrdersInEachRecentThirtyDayPeriod(LocalDateTime periodEnd) {
        return (root, query, cb) -> {
            LocalDateTime firstPeriodStart = periodEnd.minusDays(30);
            LocalDateTime secondPeriodStart = periodEnd.minusDays(60);
            LocalDateTime thirdPeriodStart = periodEnd.minusDays(90);

            Subquery<Long> firstPeriodOrders = orderCountForPeriod(root, query, cb, firstPeriodStart, periodEnd);
            Subquery<Long> secondPeriodOrders = orderCountForPeriod(root, query, cb, secondPeriodStart, firstPeriodStart);
            Subquery<Long> thirdPeriodOrders = orderCountForPeriod(root, query, cb, thirdPeriodStart, secondPeriodStart);

            return cb.and(
                    cb.isFalse(root.get("bnplAccountCreated")),
                    cb.ge(firstPeriodOrders, 3L),
                    cb.ge(secondPeriodOrders, 3L),
                    cb.ge(thirdPeriodOrders, 3L)
            );
        };
    }

    public static Specification<Customers> hasActiveBnplLead() {
        return (root, query, cb) -> {
            Subquery<Long> leadExists = query.subquery(Long.class);
            Root<BnplLead> lead = leadExists.from(BnplLead.class);
            leadExists.select(cb.literal(1L))
                    .where(
                            cb.equal(lead.get("customer"), root),
                            cb.isFalse(lead.get("deleted"))
                    );
            return cb.exists(leadExists);
        };
    }

    public static Specification<Customers> hasHundredThousandAndAbovePotentialCredit() {
        return (root, query, cb) -> cb.and(
                cb.isNotNull(root.get("preliminaryValue")),
                cb.greaterThanOrEqualTo(
                        cb.prod(root.get("preliminaryValue"), BigDecimal.valueOf(0.40)),
                        BigDecimal.valueOf(100_000)
                )
        );
    }

    public static Specification<Customers> hasCustomerInactive(CustomerActiveStatus customerActiveStatus) {
        if(customerActiveStatus == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("customerActiveStatus"), customerActiveStatus);
    }

    public static Specification<Customers> hasParentCustomer(UUID parentCustomerPublicId) {
        return (root, query, cb) -> {
            if (parentCustomerPublicId == null) {
                return null;
            }
            return cb.equal(root.get("parentCustomer").get("publicId"), parentCustomerPublicId);
        };
    }

    private static Subquery<Long> orderCountForPeriod(Root<Customers> customerRoot, CriteriaQuery<?> query,
                                                       CriteriaBuilder cb, LocalDateTime periodStart,
                                                       LocalDateTime periodEnd) {
        Subquery<Long> orderCount = query.subquery(Long.class);
        Root<SalesOrders> order = orderCount.from(SalesOrders.class);
        orderCount.select(cb.count(order))
                .where(
                        cb.equal(order.get("customer"), customerRoot),
                        cb.isFalse(order.get("deleted")),
                        cb.equal(order.get("salesOrderStatus"), LoadingStatusEnum.DELIVERED),
                        cb.greaterThanOrEqualTo(order.get("creationTime"), periodStart),
                        cb.lessThan(order.get("creationTime"), periodEnd)
                );
        return orderCount;
    }
}
