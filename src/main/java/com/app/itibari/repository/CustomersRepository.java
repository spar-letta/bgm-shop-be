package com.app.itibari.repository;

import com.app.itibari.dataTypes.CustomerActiveStatus;
import com.app.itibari.dataTypes.PaymentStatusEnum;
import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.entities.OrganizationRegions;
import com.app.itibari.models.entities.SalesAgents;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomersRepository extends JpaRepository<Customers, Long>, JpaSpecificationExecutor<Customers> {

    // Find customer by publicId (unique identifier for customer)
    Optional<Customers> findByPublicIdAndDeletedIsFalse(UUID publicId);

    // Find customer by email and not deleted
    Optional<Customers> findByEmailAndDeletedIsFalse(String email);

    // Find customer by phone number and not deleted
    Optional<Customers> findByPhoneNumberAndDeletedIsFalse(String phoneNumber);

    boolean existsByPhoneNumberAndDeletedIsFalseAndParentCustomerIsNull(String phoneNumber);

    boolean existsByShopKeeperNumberAndDeletedIsFalse(String shopKeeperNumber);

    Optional<Customers> findByPhoneNumberIgnoreCaseAndDeletedIsFalse(String phoneNumber);

    // Find customer by name and not deleted
    Optional<Customers> findByNameAndDeletedIsFalse(String name);

    // Find all customers with a deleted status false
    Page<Customers> findByDeletedIsFalse(Pageable pageable);


    @Query("SELECT c.organizationRegions FROM Customers c WHERE c.publicId = :publicId")
    OrganizationRegions findOrganizationRegionByCustomerPublicId(UUID publicId);

    @Query("SELECT c FROM Customers c WHERE c.deleted = false")
    List<Customers> findAllByDeletedIsFalse();

    @Query(value = "SELECT phone_number " +
            "FROM itibari.customers c " +
            "WHERE RIGHT(REPLACE(REPLACE(REPLACE(c.phone_number, '+', ''), '-', ''), ' ', ''), 9) IN (" +
            "    SELECT RIGHT(REPLACE(REPLACE(REPLACE(c2.phone_number, '+', ''), '-', ''), ' ', ''), 9) " +
            "    FROM itibari.customers c2 " +
            "    GROUP BY RIGHT(REPLACE(REPLACE(REPLACE(c2.phone_number, '+', ''), '-', ''), ' ', ''), 9) " +
            "    HAVING COUNT(*) > 1" +
            ")",
            nativeQuery = true)
    List<String> findDuplicatePhoneNumbers();

    @Query(value = "SELECT * FROM itibari.customers c " +
            "WHERE RIGHT(REPLACE(REPLACE(REPLACE(c.phone_number, '+', ''), '-', ''), ' ', ''), 9) = :lastNineDigits",
            nativeQuery = true)
    List<Customers> findByPhoneNumberLastNine(@Param("lastNineDigits") String lastNineDigits);

    List<Customers> findByServedBy(SalesAgents salesAgentToReceiveCustomers);

    Page<Customers> findByOrganizationRegionsAndCreationTimeBetween(OrganizationRegions organizationRegions, LocalDateTime localStartDate, LocalDateTime localEndDate, Pageable pageable);

    Page<Customers> findByCreationTimeBetween(LocalDateTime localStartDate, LocalDateTime localEndDate, Pageable pageable);


    List<Customers> findAllByBnplAccountCreatedIsFalseAndDeletedIsFalseAndPreliminaryValueIs(BigDecimal zero);

    List<Customers> findAllByBnplAccountCreatedIsFalseAndDeletedIsFalse();

    boolean existsByTinAndDeletedIsFalse(String tin);

    Page<Customers> findCustomersByCustomerActiveStatusAndDeletedIsFalse(CustomerActiveStatus customerActiveStatus, Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = """
                UPDATE itibari.customers c
                SET customer_active_status = 
                    CASE 
                        WHEN EXISTS (
                            SELECT 1 FROM itibari.sales_orders so 
                            WHERE so.sales_order_customer_fk = c.id 
                            AND so.deleted = false 
                            AND so.creation_time >= :thirtyDaysAgo
                        ) THEN :activeStatus
                        WHEN NOT EXISTS (
                            SELECT 1 FROM itibari.sales_orders so 
                            WHERE so.sales_order_customer_fk = c.id 
                            AND so.deleted = false 
                            AND so.creation_time >= :thirtyOneDaysAgo
                        ) AND EXISTS (
                            SELECT 1 FROM itibari.sales_orders so 
                            WHERE so.sales_order_customer_fk = c.id 
                            AND so.deleted = false
                        ) THEN :inactiveStatus
                        ELSE :newStatus
                    END 
                WHERE c.deleted = false
            """, nativeQuery = true)
    int updateAllCustomersActiveStatus(@Param("thirtyDaysAgo") LocalDateTime thirtyDaysAgo,
                                       @Param("thirtyOneDaysAgo") LocalDateTime thirtyOneDaysAgo,
                                       @Param("activeStatus") String activeStatus,
                                       @Param("inactiveStatus") String inactiveStatus,
                                       @Param("newStatus") String newStatus);

    List<Customers> findByDeletedIsFalseAndPhoneNumberIgnoreCase(String phoneNumber);

    @Query(value = """
        SELECT c.* 
        FROM itibari.customers c
        WHERE c.deleted = false
        AND NOT EXISTS (
            SELECT 1 
            FROM itibari.sales_orders so
            WHERE so.sales_order_customer_fk = c.id
            AND so.deleted = false
            AND so.creation_time >= :startDateTime
            AND so.creation_time < :endDateTime
        )
        """, nativeQuery = true)
    Page<Customers> findChurnedCustomers(@Param("startDateTime") LocalDateTime startDateTime,
                                         @Param("endDateTime") LocalDateTime endDateTime,
                                         Pageable pageable);

    @Modifying
    @Query("UPDATE Customers c SET c.previousOrderDate = c.lastOrderDate, c.lastOrderDate = :orderDate WHERE c.publicId = :customerId")
    void updateLastOrderDate(@Param("customerId") UUID customerId,
                             @Param("orderDate") LocalDateTime orderDate);

    Page<Customers> findByDeletedIsFalseAndActiveIsTrue(Pageable pageable);

    List<Customers> findAllByParentCustomerIsNotNull();

    List<Customers> findAllByParentCustomerAndDeletedIsFalse(Customers savedCustomer);

    @Query("""
            SELECT parent
            FROM Customers parent
            WHERE parent.deleted = false
              AND parent.parentCustomer IS NULL
              AND EXISTS (
                  SELECT child.id
                  FROM Customers child
                  WHERE child.parentCustomer = parent
                    AND child.deleted = false
              )
              AND EXISTS (
                  SELECT creditItem.id
                  FROM CreditLimitItem creditItem
                  WHERE creditItem.deleted = false
                    AND creditItem.cancelled = false
                    AND creditItem.paymentStatus IN :paymentStatuses
                    AND (
                        creditItem.wallet.customer = parent
                        OR creditItem.wallet.customer.parentCustomer = parent
                    )
              )
            """)
    Page<Customers> findEnterpriseCustomersWithOutstandingCreditItems(
            @Param("paymentStatuses") List<PaymentStatusEnum> paymentStatuses,
            Pageable pageable);
}
