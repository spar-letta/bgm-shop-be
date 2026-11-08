package com.app.itibari.repository;

import com.app.itibari.models.entities.BnplLead;
import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.entities.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BnplLeadRepository extends JpaRepository<BnplLead, Long>, JpaSpecificationExecutor<BnplLead> {

    Optional<BnplLead> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Optional<BnplLead> findByCustomerPublicIdAndDeletedIsFalse(String string);

    Page<BnplLead> findByAssignedAgentAndDeletedIsFalse(Person financeServiceOfficer, Pageable pageable);

    List<BnplLead> findAllByLoanEligibilityAmountIsNull();

    @Query("SELECT l FROM BnplLead l JOIN l.customer c WHERE l.deleted = false AND (" +
            "c.deleted = true OR c.bnplAccountCreated = true OR c.customerType IS NULL OR " +
            "(l.creationTime <= :inactiveBefore AND NOT EXISTS (" +
            "SELECT o.id FROM SalesOrders o WHERE o.customer = c AND o.deleted = false " +
            "AND o.creationTime >= :inactiveBefore)))")
    List<BnplLead> findStaleLeads(@Param("inactiveBefore") LocalDateTime inactiveBefore);

    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM BnplLead l " +
            "WHERE l.customer = :customer AND l.deleted = true AND l.creationTime <= :inactiveBefore " +
            "AND NOT EXISTS (SELECT o.id FROM SalesOrders o WHERE o.customer = l.customer " +
            "AND o.deleted = false AND o.creationTime >= :inactiveBefore)")
    boolean hasInactiveDeletedLead(@Param("customer") Customers customer,
                                   @Param("inactiveBefore") LocalDateTime inactiveBefore);

    List<BnplLead> findAllByPotentialCreditLimitIsLessThan(BigDecimal bigDecimal);
}
