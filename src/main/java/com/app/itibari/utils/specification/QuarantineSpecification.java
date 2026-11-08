package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.QuarantineStatus;
import com.app.itibari.dataTypes.TransferType;
import com.app.itibari.models.entities.Quarantine;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public class QuarantineSpecification {

    public static Specification<Quarantine> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<Quarantine> searchParamMatches(String searchParam) {

        return (root, query, cb) -> {
            if (searchParam == null || searchParam.trim().isEmpty()) {
                return cb.conjunction(); // Return true predicate for empty input
            }

            String formattedSearchParam = "%" + searchParam.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("approvedBy")), formattedSearchParam),
                    cb.like(cb.lower(root.get("quarantineWarehouseName")), formattedSearchParam),
                    cb.like(cb.lower(root.get("mainWarehouseName")), formattedSearchParam)
            );
        };
    }

    public static Specification<Quarantine> hasMainWarehousePublicId(UUID mainWarehousePublicId) {

        return (root, query, cb) -> {
            if (mainWarehousePublicId == null) {
                return null;
            }
            return cb.equal(root.get("mainWarehousePublicId"), mainWarehousePublicId);
        };
    }

    public static Specification<Quarantine> hasQuarantineWarehousePublicId(UUID quarantineWarehousePublicId) {

        return (root, query, cb) -> {
            if (quarantineWarehousePublicId == null) {
                return null;
            }
            return cb.equal(root.get("quarantineWarehousePublicId"), quarantineWarehousePublicId);
        };
    }

    public static Specification<Quarantine> hasStatus(QuarantineStatus status) {

        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("quarantineStatus"), status);
        };
    }

    public static Specification<Quarantine> hasTransferType(TransferType transferType) {

        return (root, query, cb) -> {
            if (transferType == null) {
                return null;
            }
            return cb.equal(root.get("transferType"), transferType);
        };
    }

    public static Specification<Quarantine> hasDateRange(LocalDate startDate, LocalDate endDate) {

        return (root, query, cb) -> {
            if (startDate == null || endDate == null) {
                return null;
            }
            return cb.between(root.get("creationTime"), startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
        };
    }
}
