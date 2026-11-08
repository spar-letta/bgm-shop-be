package com.app.itibari.repository;

import com.app.itibari.dataTypes.BnplCreditLimitAdjustmentType;
import com.app.itibari.models.entities.BnplCreditLimitAdjustment;
import com.app.itibari.models.entities.Customers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public interface BnplCreditLimitAdjustmentRepository extends JpaRepository<BnplCreditLimitAdjustment, Long> {

    @EntityGraph(attributePaths = {"customer", "wallet"})
    @Query("""
            select a from BnplCreditLimitAdjustment a
            where a.deleted = false
              and (cast(:startDate as timestamp) is null or a.creationTime >= :startDate)
              and (cast(:endDate as timestamp) is null or a.creationTime < :endDate)
              and (cast(:customerPublicId as uuid) is null or a.customer.publicId = :customerPublicId)
              and (cast(:searchParam as string) is null
                   or lower(a.customer.name) like :searchParam
                   or lower(a.customer.firstName) like :searchParam
                   or lower(a.customer.lastName) like :searchParam
                   or lower(a.customer.storeName) like :searchParam
                   or lower(a.customer.phoneNumber) like :searchParam)
            """)
    Page<BnplCreditLimitAdjustment> findAdjustments(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("customerPublicId") UUID customerPublicId,
            @Param("searchParam") String searchParam,
            Pageable pageable);

    boolean existsByCustomerAndAdjustmentTypeAndPeriodStartAndPeriodEndAndDeletedIsFalse(
            Customers customer,
            BnplCreditLimitAdjustmentType adjustmentType,
            LocalDate periodStart,
            LocalDate periodEnd);
}
