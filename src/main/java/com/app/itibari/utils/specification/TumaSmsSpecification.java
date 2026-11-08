package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.SMSStatus;
import com.app.itibari.models.entities.GoodsReceivedNote;
import com.app.itibari.models.entities.TumaSms;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class TumaSmsSpecification {
    public static Specification<TumaSms> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<TumaSms> searchParamMatches(String searchParam) {

        return (root, query, cb) -> {
            if (searchParam == null || searchParam.trim().isEmpty()) {
                return cb.conjunction();
            }

            String formattedSearchParam = "%" + searchParam.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("phoneNumber")), formattedSearchParam),
                    cb.like(cb.lower(root.get("recipientName")), formattedSearchParam)
            );
        };
    }

    public static Specification<TumaSms> hasStatus(SMSStatus status) {

        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            Boolean sentMessages = Boolean.FALSE;
            if (status.equals(SMSStatus.Sent)){
                sentMessages = Boolean.TRUE;
            }
            return cb.equal(root.get("isSuccess"), sentMessages);
        };
    }

    public static Specification<TumaSms> hasDateRange(LocalDate from, LocalDate to) {

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

    public static Specification<TumaSms> hasCampaign(Boolean isCampaign) {
        return (root, query, cb) -> {
            if (isCampaign == null) {
                return null;
            }
            return cb.equal(root.get("isCampaign"), isCampaign);
        };
    }

}
