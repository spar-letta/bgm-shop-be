package com.app.itibari.repository;

import com.app.itibari.dataTypes.WalletItemStatus;
import com.app.itibari.dataTypes.Batched;
import com.app.itibari.dataTypes.PaymentStatusEnum;
import com.app.itibari.models.entities.CreditLimitItem;
import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.entities.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditLimitItemRepository extends JpaRepository<CreditLimitItem, Long> {
    Page<CreditLimitItem> findByWalletAndDeletedIsFalse(Wallet wallet, Pageable pageable);

    Page<CreditLimitItem> findByDeletedIsFalse(Pageable pageable);

    @Query("""
            SELECT creditItem
            FROM CreditLimitItem creditItem
            WHERE creditItem.deleted = false
              AND (
                  creditItem.wallet.customer = :parentCustomer
                  OR creditItem.wallet.customer.parentCustomer = :parentCustomer
              )
            """)
    Page<CreditLimitItem> findEnterpriseCreditItems(
            @Param("parentCustomer") Customers parentCustomer,
            Pageable pageable);

    @Query("""
            SELECT creditItem
            FROM CreditLimitItem creditItem
            WHERE creditItem.deleted = false
              AND creditItem.batched = :batched
              AND (
                  creditItem.wallet.customer = :parentCustomer
                  OR creditItem.wallet.customer.parentCustomer = :parentCustomer
              )
            """)
    Page<CreditLimitItem> findEnterpriseCreditItemsByBatchStatus(
            @Param("parentCustomer") Customers parentCustomer,
            @Param("batched") Batched batched,
            Pageable pageable);

    Optional<CreditLimitItem> findByOrderNumberAndStatusAndDeletedIsFalse(String salesOrderNumber, WalletItemStatus walletItemStatus);

    Optional<CreditLimitItem> findByOrderNumberAndWalletAndStatusAndDeletedIsFalse(String salesOrderNumber, Wallet wallet, WalletItemStatus walletItemStatus);

    int countByDeletedIsFalse();

    List<CreditLimitItem> findAllByReferenceIdAndDeletedIsFalse(String string);

    List<CreditLimitItem> findAllByReferenceIdInAndDeletedIsFalse(List<String> referenceIds);

    Optional<CreditLimitItem> findByOrderNumberAndDeletedIsFalse(String salesOrderNumber);

    Optional<CreditLimitItem> findByPublicIdAndDeletedIsFalse(UUID creditPublicId);


    @Query(value = """
            SELECT *
            FROM itibari.credit_items ci
            WHERE ci.reference_id = :referenceId
              AND ci.status = :status
              AND ci.payment_status IN (:paymentStatuses)
              AND ci.deleted = false
              AND ci.cancelled = false
            """, nativeQuery = true)
    List<CreditLimitItem> findPendingCreditItemsNative(
            @Param("referenceId") String referenceId,
            @Param("status") String status,
            @Param("paymentStatuses") List<String> paymentStatuses
    );

    @Query(value = """
            SELECT *
            FROM itibari.credit_items ci
            WHERE ci.due_date < CURRENT_DATE
              AND ci.payment_status IN ('NOT_PAID', 'PARTIALLY_PAID')
              AND ci.deleted = false
              AND ci.cancelled = false
            """, nativeQuery = true)
    List<CreditLimitItem> findOverdueUnpaidCreditItemsNative();

    @Query(value = """
            SELECT *
            FROM itibari.credit_items ci
            WHERE ci.due_date <= :targetDate
              AND ci.due_date >= CURRENT_DATE
              AND ci.payment_status IN ('NOT_PAID', 'PARTIALLY_PAID')
              AND ci.deleted = false
              AND ci.cancelled = false
              AND ci.fully_paid = false
            """, nativeQuery = true)
    List<CreditLimitItem> findCreditOrdersDueSoon(@Param("targetDate") LocalDate targetDate);

    @Query(value = """
            SELECT *
            FROM itibari.credit_items ci
            WHERE ci.due_date <= :cutoffDate
              AND ci.payment_status IN ('NOT_PAID', 'PARTIALLY_PAID')
              AND ci.deleted = false
              AND ci.cancelled = false
              AND ci.fully_paid = false
            """, nativeQuery = true)
    List<CreditLimitItem> findOverdueCreditOrders(@Param("cutoffDate") LocalDate cutoffDate);

    @Query("""
            SELECT creditItem
            FROM CreditLimitItem creditItem
            WHERE creditItem.deleted = false
              AND creditItem.cancelled = false
              AND creditItem.fullyPaid = false
              AND creditItem.paymentStatus IN :paymentStatuses
              AND creditItem.dueDate IN :dueDates
            """)
    List<CreditLimitItem> findPaymentReminderCreditItems(
            @Param("dueDates") List<LocalDate> dueDates,
            @Param("paymentStatuses") List<PaymentStatusEnum> paymentStatuses);

    @Query("""
            SELECT creditItem.wallet.customer.id, MAX(creditItem.creationTime)
            FROM CreditLimitItem creditItem
            WHERE creditItem.wallet.customer.id IN :customerIds
              AND creditItem.deleted = false
            GROUP BY creditItem.wallet.customer.id
            """)
    List<Object[]> findLatestCreationTimesByCustomerIds(@Param("customerIds") List<Long> customerIds);

    @Query("""
            SELECT creditItem
            FROM CreditLimitItem creditItem
            WHERE creditItem.deleted = false
              AND creditItem.cancelled = false
              AND creditItem.dueDate IS NOT NULL
              AND creditItem.dueDate BETWEEN :periodStart AND :today
              AND (
                  creditItem.wallet = :fundingWallet
                  OR creditItem.wallet.parentWallet = :fundingWallet
              )
            ORDER BY creditItem.dueDate DESC, creditItem.id DESC
            """)
    Page<CreditLimitItem> findRecentCreditItemsForFundingWallet(
            @Param("fundingWallet") Wallet fundingWallet,
            @Param("periodStart") LocalDate periodStart,
            @Param("today") LocalDate today,
            Pageable pageable);

}
