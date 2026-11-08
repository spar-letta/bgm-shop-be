package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.BnplFunnelStage;
import com.app.itibari.dataTypes.BnplLeadStatus;
import com.app.itibari.models.entities.BnplLead;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.UUID;

public class LeadSpecification {
    public static Specification<BnplLead> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<BnplLead> searchParamMatches(String searchParam) {
        return (root, query, cb) -> {
            if (searchParam == null) {
                return null;
            }
            return cb.or(
                    cb.like(cb.lower(root.get("phoneNumber")), "%" + searchParam.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("customer").get("name")), "%" + searchParam.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("customer").get("storeName")), "%" + searchParam.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("leadStatus")), "%" + searchParam.toLowerCase() + "%")
            );
        };

    }

    public static Specification<BnplLead> hasFunnelStage(BnplFunnelStage funnelStage) {
        return (root, query, cb) -> {
            if (funnelStage == null) {
                return null;
            }
            return cb.equal(root.get("funnelStage"), funnelStage);
        };
    }

    public static Specification<BnplLead> hasLeadStatus(BnplLeadStatus leadStatus) {
        return (root, query, cb) -> {
            if (leadStatus == null) {
                return null;
            }
            return cb.equal(root.get("leadStatus"), leadStatus);
        };
    }

    public static Specification<BnplLead> hasCallStatus(UUID callStatus) {
        return (root, query, cb) -> {
            if (callStatus == null) {
                return null;
            }
            return cb.equal(root.get("callStatusPublicId"), callStatus.toString());
        };
    }

    public static Specification<BnplLead> hasLocation(UUID locationId) {
        return (root, query, cb) -> {
            if (locationId == null) {
                return null;
            }
            return cb.equal(root.get("customer").get("organizationRegions").get("publicId"), locationId);
        };
    }

    public static Specification<BnplLead> hasWard(UUID wardId) {
        return (root, query, cb) -> {
            if (wardId == null) {
                return null;
            }
            return cb.equal(root.get("customer").get("organizationLocation").get("publicId"), wardId);
        };
    }
    public static Specification<BnplLead> isOverOneMillion(boolean vip) {
        return (root, query, cb) -> {
            if (!vip) {
                return cb.lessThanOrEqualTo(root.get("potentialCreditLimit"), 999999);
            }
            return cb.greaterThan(root.get("potentialCreditLimit"), 1000000);
        };
    }

    public static Specification<BnplLead> hasHundredThousandAndAbovePotentialCredit() {
        return (root, query, cb) -> cb.and(
                cb.isNotNull(root.get("potentialCreditLimit")),
                cb.greaterThanOrEqualTo(root.get("potentialCreditLimit"), BigDecimal.valueOf(100_000))
        );
    }

}
