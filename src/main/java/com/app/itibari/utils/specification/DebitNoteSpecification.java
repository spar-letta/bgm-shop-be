package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.DebitReferenceType;
import com.app.itibari.models.entities.DebitNotes;
import com.app.itibari.models.entities.Reconciliation;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class DebitNoteSpecification {

    public static Specification<DebitNotes> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<DebitNotes> hasDeliveryAgentPublicId(UUID deliveryAgentPublicId) {

        return (root, query, cb) -> {
            if (deliveryAgentPublicId == null) {
                return null;
            }
            return cb.and(cb.equal(root.get("referenceId"), deliveryAgentPublicId),
                    cb.equal(root.get("referenceType"), DebitReferenceType.DeliveryAgent));
        };
    }

    public static Specification<DebitNotes> hasReconciliationPublicId(String reconciliationPublicId) {

        return (root, query, cb) -> {
            if (reconciliationPublicId == null || reconciliationPublicId.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("reconciliationPublicId"), reconciliationPublicId);
        };
    }

    public static Specification<DebitNotes> isReconciled(Boolean reconciled) {

        return (root, query, cb) -> {
            if (reconciled == null) {
                return null;
            }
            return cb.equal(root.get("reconciled"), reconciled);
        };
    }

    public static Specification<DebitNotes> hasSearchTerm(String searchParam) {

        return (root, query, cb) -> {
            if (searchParam == null || searchParam.isEmpty()) {
                return null;
            }
            String likePattern = "%" + searchParam.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("description")), likePattern),
                    cb.like(cb.lower(root.get("route")), likePattern),
                    cb.like(cb.lower(root.get("referenceId").as(String.class)), likePattern)
            );
        };
    }

    public static Specification<DebitNotes> hasDateRange(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("creationTime"), from.atStartOfDay()));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.<LocalDateTime>get("creationTime"), to.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<DebitNotes> hasRouteNumber(String routeNumber) {
        return (root, query, cb) -> {
            if (routeNumber == null || routeNumber.isBlank()) {
                return null;
            }
            String normalized = routeNumber.toLowerCase(Locale.ROOT);
            Subquery<Integer> viaReconciliation = query.subquery(Integer.class);
            Root<Reconciliation> reconciliation = viaReconciliation.from(Reconciliation.class);
            viaReconciliation.select(cb.literal(1))
                    .where(cb.and(
                            cb.equal(reconciliation.get("deleted"), false),
                            cb.equal(cb.lower(reconciliation.get("deliveryId")), normalized),
                            cb.equal(
                                    root.get("reconciliationPublicId"),
                                    reconciliation.get("publicId").as(String.class))
                    ));
            return cb.or(
                    cb.equal(cb.lower(root.get("route")), normalized),
                    cb.exists(viaReconciliation)
            );
        };
    }
}
