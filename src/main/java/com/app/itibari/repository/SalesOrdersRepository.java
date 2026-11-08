package com.app.itibari.repository;

import com.app.itibari.dataTypes.LoadingStatusEnum;
import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.entities.DeliveryAgents;
import com.app.itibari.models.entities.OrganizationRegions;
import com.app.itibari.models.entities.SalesOrders;
import com.app.itibari.services.supplierportal.SupplierPortalGeographyFilter;
import com.app.itibari.services.supplierportal.SupplierPortalProductFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalesOrdersRepository extends JpaRepository<SalesOrders, Long>, JpaSpecificationExecutor<SalesOrders> {

//    @EntityGraph(attributePaths = {}, type = EntityGraph.EntityGraphType.FETCH)
//    @Override
//    Page<SalesOrders> findAll(Specification<SalesOrders> spec, Pageable pageable);

    Optional<SalesOrders> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Page<SalesOrders> findByDeletedIsFalse(Pageable pageable);

    @Query("SELECT COUNT(s) FROM SalesOrders s WHERE s.customer.publicId = :customerId")
    long countByCustomerId(UUID customerId);

    @Query("""
            SELECT c.publicId, COUNT(so)
            FROM SalesOrders so
            JOIN so.customer c
            WHERE c.publicId IN :customerIds
            AND so.deleted = false
            AND so.salesOrderStatus = :status
            GROUP BY c.publicId
            """)
    List<Object[]> countOrdersByCustomerPublicIdsAndStatus(@Param("customerIds") Collection<UUID> customerIds,
                                                           @Param("status") LoadingStatusEnum status);

    // Calculate the average order value for a given customer ID, for cash purchases only
//    @Query("SELECT AVG(s.totalAmount) FROM SalesOrders s WHERE s.customer.publicId = :customerId AND s.deleted = false AND s.salesOrderStatus = 'DELIVERED' AND s.creditApplied = false")
//    BigDecimal calculateAverageOrderValue(UUID customerId);

    List<SalesOrders> findSalesOrdersBySalesOrderStatus(LoadingStatusEnum loadingStatusEnum);

    @Query("SELECT s FROM SalesOrders s WHERE s.customer.publicId = :customerId AND s.deleted = false")
    List<SalesOrders> findByCustomerAndDeletedIsFalse(UUID customerId);

    List<SalesOrders> findAllByCreationTimeBetweenAndSalesOrderStatusAndDeletedIsFalse(LocalDateTime localDateTime,
                                                                                       LocalDateTime localDateTime1,
                                                                                       LoadingStatusEnum loadingStatusEnum);

    List<SalesOrders> findAllByCreationTimeBetweenAndDeletedIsFalse(LocalDateTime creationTime, LocalDateTime creationTime2);

    Page<SalesOrders> findAllByCreationTimeBetweenAndSalesOrderStatusAndDeletedIsFalse(LocalDateTime localDateTime,
                                                                                       LocalDateTime localDateTime1,
                                                                                       LoadingStatusEnum statusEnum,
                                                                                       Pageable pageable);

    List<SalesOrders> findByCustomerAndSalesOrderStatusInAndDeletedIsFalse(Customers customer, List<LoadingStatusEnum> activeStatuses);

    @Modifying
    @Transactional
    @Query("UPDATE SalesOrders so SET so.salesOrderStatus = :newStatus WHERE so.salesOrderStatus = :oldStatus")
    int bulkUpdateStatus(@Param("oldStatus") LoadingStatusEnum oldStatus,
                         @Param("newStatus") LoadingStatusEnum newStatus);

    List<SalesOrders> findAllByCustomerAndSalesOrderStatusAndDeletedIsFalse(Customers customer, LoadingStatusEnum loadingStatusEnum);

    SalesOrders findByPublicId(UUID publicId);


    Page<SalesOrders> findAllByOrganizationRegionsAndCreationTimeBetweenAndSalesOrderStatusAndDeletedIsFalse(OrganizationRegions organizationRegions, LocalDateTime localDateTime, LocalDateTime localDateTime1, LoadingStatusEnum loadingStatusEnum, Pageable pageable);


    Optional<SalesOrders> findBySalesOrderNumberAndDeletedIsFalse(String orderNumber);

    List<SalesOrders> findSalesOrdersBySalesOrderStatusAndCreditAppliedIsTrueAndDeletedIsFalse(LoadingStatusEnum loadingStatusEnum);

    @Query("SELECT COUNT(so) " +
            "FROM SalesOrders so " +
            "JOIN so.customer c " +
            "JOIN so.salesOrderItems soi " +
            "JOIN soi.product p " +
            "JOIN p.suppliers s " +
            "WHERE s.publicId = :supplierId " +
            "AND so.salesOrderStatus = 'DELIVERED' " +
            "AND (COALESCE(:regionId, c.organizationRegions.publicId) = c.organizationRegions.publicId) " +
            "AND so.creationTime >= COALESCE(CAST(:startDateTime AS timestamp), so.creationTime) " +
            "AND so.creationTime <= COALESCE(CAST(:endDateTime AS timestamp), so.creationTime)")
    Long countDeliveredOrdersBySupplierAndRegion(@Param("supplierId") UUID supplierId,
                                                 @Param("regionId") UUID regionId,
                                                 @Param("startDateTime") LocalDateTime startDateTime,
                                                 @Param("endDateTime") LocalDateTime endDateTime);


    @Query("SELECT SUM(p.weight * soi.quantityDelivered) " +
            "FROM SalesOrders so " +
            "JOIN so.customer c " +
            "JOIN so.salesOrderItems soi " +
            "JOIN soi.product p " +
            "JOIN p.suppliers s " +
            "WHERE s.publicId = :supplierId " +
            "AND so.salesOrderStatus = 'DELIVERED' " +
            "AND (:regionId IS NULL OR c.organizationRegions.publicId = :regionId) " +
            "AND so.creationTime >= COALESCE(:startDateTime, so.creationTime) " +
            "AND so.creationTime <= COALESCE(:endDateTime, so.creationTime)")
    BigDecimal totalWeightDelivered(@Param("supplierId") UUID supplierId,
                                    @Param("regionId") UUID regionId,
                                    @Param("startDateTime") LocalDateTime startDateTime,
                                    @Param("endDateTime") LocalDateTime endDateTime);


    @Query("SELECT COUNT(c) " +
            "FROM SalesOrders so " +
            "JOIN so.customer c " +
            "JOIN so.salesOrderItems soi " +
            "JOIN soi.product p " +
            "JOIN p.suppliers s " +
            "WHERE s.publicId = :supplierId " +
            "AND so.salesOrderStatus IN ('DELIVERED', 'ORDER_PLACED', 'IN_TRANSIT') " +
            "AND c.active = TRUE " +
            "AND (:regionId IS NULL OR c.organizationRegions.publicId = :regionId) " +
            "AND so.creationTime >= COALESCE(:startDateTime, so.creationTime) " +
            "AND so.creationTime <= COALESCE(:endDateTime, so.creationTime)")
    Long countActiveCustomers(@Param("supplierId") UUID supplierId,
                              @Param("regionId") UUID regionId,
                              @Param("startDateTime") LocalDateTime startDateTime,
                              @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT COUNT(so) " +
            "FROM SalesOrders so " +
            "JOIN so.customer c " +
            "JOIN so.salesOrderItems soi " +
            "JOIN soi.product p " +
            "JOIN p.suppliers s " +
            "WHERE s.publicId = :supplierId " +
            "AND so.salesOrderStatus = 'DELIVERED' " +
            "AND (COALESCE(:regionId, c.organizationRegions.publicId) = c.organizationRegions.publicId) " +
            "AND (:locationId IS NULL OR c.organizationLocation.publicId = :locationId) " +
            "AND so.creationTime >= COALESCE(CAST(:startDateTime AS timestamp), so.creationTime) " +
            "AND so.creationTime <= COALESCE(CAST(:endDateTime AS timestamp), so.creationTime)")
    Long countDeliveredOrdersBySupplierRegionAndLocation(@Param("supplierId") UUID supplierId,
                                                          @Param("regionId") UUID regionId,
                                                          @Param("locationId") UUID locationId,
                                                          @Param("startDateTime") LocalDateTime startDateTime,
                                                          @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT COALESCE(SUM(COALESCE((" +
            " SELECT SUM(p.weight * soi.quantityDelivered / 1000.0) FROM SalesOrderItems soi" +
            " JOIN soi.product p JOIN p.suppliers s WHERE soi.salesOrder = so AND s.publicId = :supplierId" +
            "), 0)), 0) " +
            "FROM SalesOrders so " +
            "JOIN so.customer c " +
            "WHERE so.salesOrderStatus = 'DELIVERED' " +
            "AND EXISTS (" +
            "    SELECT 1 FROM SalesOrderItems sox JOIN sox.product px JOIN px.suppliers sx " +
            "    WHERE sox.salesOrder = so AND sx.publicId = :supplierId" +
            ") " +
            "AND (COALESCE(:regionId, c.organizationRegions.publicId) = c.organizationRegions.publicId) " +
            "AND (:locationId IS NULL OR c.organizationLocation.publicId = :locationId) " +
            "AND so.creationTime >= COALESCE(CAST(:startDateTime AS timestamp), so.creationTime) " +
            "AND so.creationTime <= COALESCE(CAST(:endDateTime AS timestamp), so.creationTime)")
    BigDecimal sumWeightDeliveredTonnesBySupplierRegionAndLocation(@Param("supplierId") UUID supplierId,
                                                               @Param("regionId") UUID regionId,
                                                               @Param("locationId") UUID locationId,
                                                               @Param("startDateTime") LocalDateTime startDateTime,
                                                               @Param("endDateTime") LocalDateTime endDateTime);

    /**
     * Supplier Portal V2 COGS: {@code SUM(COALESCE(product.buyingPrice, 0) * quantityDelivered)} for
     * delivered lines whose product belongs to this supplier. Uses {@code product.buying_price} (current value —
     * not a snapshot at order time). Item-scoped (not {@code SalesOrders#totalAmount}).
     */
    @Query("SELECT COALESCE(SUM(COALESCE(p.buyingPrice, 0) * soi.quantityDelivered), 0) " +
            "FROM SalesOrders so " +
            "JOIN so.customer c " +
            "JOIN so.salesOrderItems soi " +
            "JOIN soi.product p " +
            "JOIN p.suppliers s " +
            "WHERE s.publicId = :supplierId " +
            "AND so.salesOrderStatus = 'DELIVERED' " +
            "AND soi.quantityDelivered > 0 " +
            "AND (:regionFilterDisabled = true OR c.organizationRegions.publicId IN :regionIds) " +
            "AND (:locationFilterDisabled = true OR c.organizationLocation.publicId IN :locationIds) " +
            "AND (:productFilterDisabled = true OR p.publicId IN :productIds) " +
            "AND so.creationTime >= COALESCE(CAST(:startDateTime AS timestamp), so.creationTime) " +
            "AND so.creationTime <= COALESCE(CAST(:endDateTime AS timestamp), so.creationTime)")
    BigDecimal sumSupplierPortalCogsBySupplierRegionAndLocation(@Param("supplierId") UUID supplierId,
                                                                @Param("regionIds") List<UUID> regionIds,
                                                                @Param("regionFilterDisabled") boolean regionFilterDisabled,
                                                                @Param("locationIds") List<UUID> locationIds,
                                                                @Param("locationFilterDisabled") boolean locationFilterDisabled,
                                                                @Param("productIds") List<UUID> productIds,
                                                                @Param("productFilterDisabled") boolean productFilterDisabled,
                                                                @Param("startDateTime") LocalDateTime startDateTime,
                                                                @Param("endDateTime") LocalDateTime endDateTime);

    /**
     * Supplier Portal V2 dashboard summary-metrics: all four headline scalars in one scan.
     * <p>Column order: active_customers, total_orders, tonnes, cogs_tzs.</p>
     * <ul>
     *   <li>active_customers — {@code COUNT(DISTINCT customer)} with at least one DELIVERED supplier line
     *       where {@code quantity_delivered > 0}; same semantics as
     *       {@link #countDistinctDeliveredSupplierPortalCustomers}.</li>
     *   <li>total_orders — {@code COUNT(order_id)} per supplier line row (equivalent to
     *       {@link #countDeliveredOrdersBySupplierRegionAndLocation}); no {@code quantity_delivered > 0} filter.</li>
     *   <li>tonnes — {@code SUM(product.weight * quantity_delivered / 1000.0)} — weight stored in kg, result in tonnes.</li>
     *   <li>cogs_tzs — {@code SUM(product.buying_price * quantity_delivered)} where {@code quantity_delivered > 0}.
     *       Uses current {@code products.buying_price}, not the sell-price snapshot on the order item.</li>
     * </ul>
     */
    @Query(
            value = """
                    SELECT COUNT(DISTINCT CASE
                               WHEN so.loading_status = 'DELIVERED' AND soi.quantity_delivered > 0
                               THEN c.public_id
                           END),
                           COUNT(CASE WHEN so.loading_status = 'DELIVERED' THEN so.id END),
                           COALESCE(SUM(CASE
                               WHEN so.loading_status = 'DELIVERED'
                               THEN COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0
                           END), 0),
                           COALESCE(SUM(CASE
                               WHEN so.loading_status = 'DELIVERED' AND soi.quantity_delivered > 0
                               THEN COALESCE(pr.buying_price, 0) * soi.quantity_delivered
                           END), 0)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so
                        ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c
                        ON c.id = so.sales_order_customer_fk AND c.deleted IS NOT TRUE
                    INNER JOIN itibari.organization_regions r
                        ON r.id = c.customer_organization_region_fk
                    LEFT JOIN itibari.organization_locations ol
                        ON ol.id = c.customer_organization_location_fk
                    INNER JOIN itibari.products pr
                        ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND (:regionId IS NULL OR r.public_id = :regionId)
                    AND (:locationId IS NULL OR ol.public_id = :locationId)
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                        SELECT 1 FROM itibari.product_suppliers ps
                        INNER JOIN itibari.suppliers sup
                            ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                        WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    """,
            nativeQuery = true)
    Object[] aggregateSupplierPortalDashboardSummaryMetricsV2(
            @Param("supplierId") UUID supplierId,
            @Param("regionId") UUID regionId,
            @Param("locationId") UUID locationId,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    /**
     * Same supplier-portal line revenue as {@link #sumSupplierPortalCogsBySupplierRegionAndLocation},
     * grouped by calendar year/month of the order {@code creationTime}.
     */
    @Query(value = "SELECT COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0)," +
            "EXTRACT(YEAR FROM so.creation_time), " +
            "EXTRACT(MONTH FROM so.creation_time) " +
            "FROM itibari.sales_order_items soi " +
            "JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE " +
            "JOIN itibari.customers c ON c.id = so.sales_order_customer_fk " +
            "JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk " +
            "LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk " +
            "JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE " +
            "WHERE soi.deleted IS NOT TRUE " +
            "AND so.loading_status = 'DELIVERED' " +
            "AND soi.quantity_delivered > 0 " +
            "AND (:regionId IS NULL OR r.public_id = :regionId) " +
            "AND (:locationId IS NULL OR ol.public_id = :locationId) " +
            "AND so.creation_time >= :fromTs " +
            "AND so.creation_time <= :toTs " +
            "AND EXISTS (" +
            "  SELECT 1 FROM itibari.product_suppliers ps " +
            "  JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE " +
            "  WHERE ps.product_id = pr.id AND sup.public_id = :supplierId" +
            ") " +
            "GROUP BY EXTRACT(YEAR FROM so.creation_time), EXTRACT(MONTH FROM so.creation_time)",
            nativeQuery = true)
    List<Object[]> aggregateSupplierPortalLineRevenueByOrderCreationMonth(
            @Param("supplierId") UUID supplierId,
            @Param("regionId") UUID regionId,
            @Param("locationId") UUID locationId,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    /**
     * Supplier Portal V2 product distribution: aggregates delivered lines for this supplier's SKUs only.
     * Ranking is computed in the service as {@code pageOffset + rowIndex + 1}; query order is fixed
     * descending by delivered units ({@code SUM(quantity_delivered)}), then {@code pr.public_id} ascending for ties.
     * Settled tonnes: {@code SUM(product.weight * quantity_delivered / 1000.0)} — {@code product.weight} is stored in kg; divided by 1000 to convert to tonnes.
     * COGS column: {@code SUM(product.buying_price * quantity_delivered)} — uses current {@code products.buying_price},
     * consistent with portal COGS aggregates. Not a sell-price snapshot.
     * Geography matches other portal V2 filters: customer's {@code organization_regions}, optional customer's {@code organization_locations}.
     */
    @Query(
            value = """
                    SELECT pr.public_id, pr.product_name, pr.sku, pr.unit_of_measure,
                           SUM(soi.quantity_delivered),
                           COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0),
                           COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                    AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY pr.public_id, pr.product_name, pr.sku, pr.unit_of_measure, pr.weight
                    ORDER BY SUM(soi.quantity_delivered) DESC, pr.public_id ASC
                    """,
            countQuery = """
                    SELECT COUNT(*) FROM (
                       SELECT pr.public_id
                       FROM itibari.sales_order_items soi
                       INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                       INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                       INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                       LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                       INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                       WHERE soi.deleted IS NOT TRUE
                       AND so.loading_status = 'DELIVERED'
                       AND soi.quantity_delivered > 0
                       AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                       AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                       AND so.creation_time >= :fromTs
                       AND so.creation_time <= :toTs
                       AND EXISTS (
                          SELECT 1 FROM itibari.product_suppliers ps
                          INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                          WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                       )
                       GROUP BY pr.public_id, pr.product_name, pr.sku, pr.unit_of_measure, pr.weight
                    ) grouped_products
                    """,
            nativeQuery = true)
    Page<Object[]> pageSupplierPortalProductDistributionV2(
            @Param("supplierId") UUID supplierId,
            @Param("regionIds") List<UUID> regionIds,
            @Param("regionFilterDisabled") boolean regionFilterDisabled,
            @Param("locationIds") List<UUID> locationIds,
            @Param("locationFilterDisabled") boolean locationFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs,
            Pageable pageable);

    /**
     * Unpaged variant of {@link #pageSupplierPortalProductDistributionV2} for previous-window trend merge.
     */
    @Query(
            value = """
                    SELECT pr.public_id, pr.product_name, pr.sku, pr.unit_of_measure,
                           SUM(soi.quantity_delivered),
                           COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0),
                           COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                    AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY pr.public_id, pr.product_name, pr.sku, pr.unit_of_measure, pr.weight
                    ORDER BY SUM(soi.quantity_delivered) DESC, pr.public_id ASC
                    """,
            nativeQuery = true)
    List<Object[]> listSupplierPortalProductDistributionV2(
            @Param("supplierId") UUID supplierId,
            @Param("regionIds") List<UUID> regionIds,
            @Param("regionFilterDisabled") boolean regionFilterDisabled,
            @Param("locationIds") List<UUID> locationIds,
            @Param("locationFilterDisabled") boolean locationFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    /**
     * Product Analysis V2 all-products product ranking by portal COGS descending.
     * Same supplier-scoped product aggregates as {@link #pageSupplierPortalProductDistributionV2} with COGS-first ordering.
     */
    @Query(
            value = """
                    SELECT pr.public_id, pr.product_name, pr.sku, pr.unit_of_measure,
                           SUM(soi.quantity_delivered),
                           COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                    AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY pr.public_id, pr.product_name, pr.sku, pr.unit_of_measure
                    ORDER BY COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0) DESC,
                             pr.public_id ASC
                    """,
            countQuery = """
                    SELECT COUNT(*) FROM (
                       SELECT pr.public_id
                       FROM itibari.sales_order_items soi
                       INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                       INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                       INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                       LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                       INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                       WHERE soi.deleted IS NOT TRUE
                       AND so.loading_status = 'DELIVERED'
                       AND soi.quantity_delivered > 0
                       AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                       AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                       AND so.creation_time >= :fromTs
                       AND so.creation_time <= :toTs
                       AND EXISTS (
                          SELECT 1 FROM itibari.product_suppliers ps
                          INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                          WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                       )
                       GROUP BY pr.public_id, pr.product_name, pr.sku, pr.unit_of_measure
                    ) grouped_products
                    """,
            nativeQuery = true)
    Page<Object[]> pageSupplierPortalAllProductsProductRankingByCogs(
            @Param("supplierId") UUID supplierId,
            @Param("regionIds") List<UUID> regionIds,
            @Param("regionFilterDisabled") boolean regionFilterDisabled,
            @Param("locationIds") List<UUID> locationIds,
            @Param("locationFilterDisabled") boolean locationFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs,
            Pageable pageable);

    /**
     * Unpaged variant of {@link #pageSupplierPortalAllProductsProductRankingByCogs} for previous-window COGS trend merge.
     */
    @Query(
            value = """
                    SELECT pr.public_id, pr.product_name, pr.sku, pr.unit_of_measure,
                           SUM(soi.quantity_delivered),
                           COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                    AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY pr.public_id, pr.product_name, pr.sku, pr.unit_of_measure
                    ORDER BY COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0) DESC,
                             pr.public_id ASC
                    """,
            nativeQuery = true)
    List<Object[]> listSupplierPortalAllProductsProductRankingByCogs(
            @Param("supplierId") UUID supplierId,
            @Param("regionIds") List<UUID> regionIds,
            @Param("regionFilterDisabled") boolean regionFilterDisabled,
            @Param("locationIds") List<UUID> locationIds,
            @Param("locationFilterDisabled") boolean locationFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    /**
     * 1-based best-selling rank for one product among supplier SKUs in the window.
     * Filter and ordering parity with {@link #pageSupplierPortalAllProductsProductRankingByCogs}:
     * DELIVERED, quantity_delivered &gt; 0, supplier EXISTS, region/location, creation_time window;
     * rank by portal COGS DESC, tie-break pr.public_id ASC.
     * Returns null when the product has no qualifying lines in the window (not in grouped set).
     */
    @Query(
            value = """
                    SELECT ranked.sales_rank FROM (
                       SELECT pr.public_id,
                              RANK() OVER (
                                  ORDER BY COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0) DESC,
                                           pr.public_id ASC
                              ) AS sales_rank
                       FROM itibari.sales_order_items soi
                       INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                       INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                       INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                       LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                       INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                       WHERE soi.deleted IS NOT TRUE
                       AND so.loading_status = 'DELIVERED'
                       AND soi.quantity_delivered > 0
                       AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                       AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                       AND so.creation_time >= :fromTs
                       AND so.creation_time <= :toTs
                       AND EXISTS (
                          SELECT 1 FROM itibari.product_suppliers ps
                          INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                          WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                       )
                       GROUP BY pr.public_id, pr.product_name, pr.sku, pr.unit_of_measure, pr.weight
                    ) ranked
                    WHERE ranked.public_id = :productId
                    """,
            nativeQuery = true)
    Integer findSupplierPortalProductBestSellingRankByCogs(
            @Param("supplierId") UUID supplierId,
            @Param("productId") UUID productId,
            @Param("regionIds") List<UUID> regionIds,
            @Param("regionFilterDisabled") boolean regionFilterDisabled,
            @Param("locationIds") List<UUID> locationIds,
            @Param("locationFilterDisabled") boolean locationFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    /**
     * Supplier Portal V2 dashboard district-performance — region (district) grouping by {@code organization_regions}.
     * Used when {@code regionId} and {@code locationId} are both absent. Column order matches {@link #listSupplierPortalDistrictPerformanceV2}.
     */
    @Query(
            value = """
                    SELECT r.public_id, COALESCE(r.name, 'UNKNOWN'),
                           COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0),
                           COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0),
                           COUNT(DISTINCT c.public_id),
                           COUNT(DISTINCT so.public_id)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND (:regionId IS NULL OR r.public_id = :regionId)
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY r.public_id, r.name
                    ORDER BY COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0) DESC
                    """,
            nativeQuery = true)
    List<Object[]> listSupplierPortalDistrictPerformanceByRegionV2(
            @Param("supplierId") UUID supplierId,
            @Param("regionId") UUID regionId,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    /**
     * Paged variant of {@link #listSupplierPortalDistrictPerformanceByRegionV2}.
     */
    @Query(
            value = """
                    SELECT r.public_id, COALESCE(r.name, 'UNKNOWN'),
                           COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0),
                           COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0),
                           COUNT(DISTINCT c.public_id),
                           COUNT(DISTINCT so.public_id)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND (:regionId IS NULL OR r.public_id = :regionId)
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY r.public_id, r.name
                    ORDER BY COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0) DESC
                    """,
            countQuery = """
                    SELECT COUNT(*) FROM (
                       SELECT r.public_id
                       FROM itibari.sales_order_items soi
                       INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                       INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                       INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                       INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                       WHERE soi.deleted IS NOT TRUE
                       AND so.loading_status = 'DELIVERED'
                       AND soi.quantity_delivered > 0
                       AND (:regionId IS NULL OR r.public_id = :regionId)
                       AND so.creation_time >= :fromTs
                       AND so.creation_time <= :toTs
                       AND EXISTS (
                          SELECT 1 FROM itibari.product_suppliers ps
                          INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                          WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                       )
                       GROUP BY r.public_id, r.name
                    ) grouped_regions
                    """,
            nativeQuery = true)
    Page<Object[]> pageSupplierPortalDistrictPerformanceByRegionV2(
            @Param("supplierId") UUID supplierId,
            @Param("regionId") UUID regionId,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs,
            Pageable pageable);

    /**
     * Supplier Portal V2 "district" performance — provisional grouping keyed by {@code customers.organization_locations}
     * (ward / organization location on the customer). Used when {@code regionId} and/or {@code locationId} is present.
     * Ordered by settled tonnes descending (same tonne rule as portal V2 aggregates).
     */
    @Query(
            value = """
                    SELECT ol.public_id, COALESCE(ol.name, 'UNKNOWN'),
                           COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0),
                           COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0),
                           COUNT(DISTINCT c.public_id),
                           COUNT(DISTINCT so.public_id)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                    AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY ol.public_id, ol.name
                    ORDER BY COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0) DESC
                    """,
            nativeQuery = true)
    List<Object[]> listSupplierPortalDistrictPerformanceV2(
            @Param("supplierId") UUID supplierId,
            @Param("regionId") UUID regionId,
            @Param("locationId") UUID locationId,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    /**
     * Paged variant of {@link #listSupplierPortalDistrictPerformanceV2}. Fixed DB order: settled tonnes descending.
     * Pageable is limited to page index and size — client sort is ignored to keep ranking stable.
     */
    @Query(
            value = """
                    SELECT ol.public_id, COALESCE(ol.name, 'UNKNOWN'),
                           COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0),
                           COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0),
                           COUNT(DISTINCT c.public_id),
                           COUNT(DISTINCT so.public_id)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                    AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY ol.public_id, ol.name
                    ORDER BY COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0) DESC
                    """,
            countQuery = """
                    SELECT COUNT(*) FROM (
                       SELECT ol.public_id
                       FROM itibari.sales_order_items soi
                       INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                       INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                       INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                       LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                       INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                       WHERE soi.deleted IS NOT TRUE
                       AND so.loading_status = 'DELIVERED'
                       AND soi.quantity_delivered > 0
                       AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                       AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                       AND so.creation_time >= :fromTs
                       AND so.creation_time <= :toTs
                       AND EXISTS (
                          SELECT 1 FROM itibari.product_suppliers ps
                          INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                          WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                       )
                       GROUP BY ol.public_id, ol.name
                    ) grouped_districts
                    """,
            nativeQuery = true)
    Page<Object[]> pageSupplierPortalDistrictPerformanceV2(
            @Param("supplierId") UUID supplierId,
            @Param("regionId") UUID regionId,
            @Param("locationId") UUID locationId,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs,
            Pageable pageable);

    /**
     * Supplier Portal V2 sales calendar: delivered aggregates grouped by order {@code creation_time} calendar day.
     * Columns: day (DATE), line revenue TZS, settled tonnes, distinct delivered order count.
     */
    @Query(
            value = """
                    SELECT DATE(so.creation_time),
                           COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0),
                           COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0),
                           COUNT(DISTINCT so.public_id)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                    AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY DATE(so.creation_time)
                    ORDER BY DATE(so.creation_time)
                    """,
            nativeQuery = true)
    List<Object[]> aggregateSupplierPortalSalesCalendarByDay(
            @Param("supplierId") UUID supplierId,
            @Param("regionIds") List<UUID> regionIds,
            @Param("regionFilterDisabled") boolean regionFilterDisabled,
            @Param("locationIds") List<UUID> locationIds,
            @Param("locationFilterDisabled") boolean locationFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    @Query("SELECT COUNT(DISTINCT c) " +
            "FROM SalesOrders so " +
            "JOIN so.customer c " +
            "JOIN so.salesOrderItems soi " +
            "JOIN soi.product p " +
            "JOIN p.suppliers s " +
            "WHERE s.publicId = :supplierId " +
            "AND p.publicId = :productId " +
            "AND so.salesOrderStatus IN ('DELIVERED', 'ORDER_PLACED', 'IN_TRANSIT') " +
            "AND c.active = TRUE " +
            "AND (:regionId IS NULL OR c.organizationRegions.publicId = :regionId) " +
            "AND (:locationId IS NULL OR c.organizationLocation.publicId = :locationId) " +
            "AND so.creationTime >= COALESCE(CAST(:startDateTime AS timestamp), so.creationTime) " +
            "AND so.creationTime <= COALESCE(CAST(:endDateTime AS timestamp), so.creationTime)")
    Long countActiveCustomersBySupplierProductRegionAndLocation(@Param("supplierId") UUID supplierId,
                                                                @Param("productId") UUID productId,
                                                                @Param("regionId") UUID regionId,
                                                                @Param("locationId") UUID locationId,
                                                                @Param("startDateTime") LocalDateTime startDateTime,
                                                                @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT COUNT(DISTINCT so) " +
            "FROM SalesOrders so " +
            "JOIN so.customer c " +
            "JOIN so.salesOrderItems soi " +
            "JOIN soi.product p " +
            "JOIN p.suppliers s " +
            "WHERE s.publicId = :supplierId " +
            "AND p.publicId = :productId " +
            "AND so.salesOrderStatus = 'DELIVERED' " +
            "AND soi.quantityDelivered > 0 " +
            "AND (COALESCE(:regionId, c.organizationRegions.publicId) = c.organizationRegions.publicId) " +
            "AND (:locationId IS NULL OR c.organizationLocation.publicId = :locationId) " +
            "AND so.creationTime >= COALESCE(CAST(:startDateTime AS timestamp), so.creationTime) " +
            "AND so.creationTime <= COALESCE(CAST(:endDateTime AS timestamp), so.creationTime)")
    Long countDeliveredOrdersBySupplierProductRegionAndLocation(@Param("supplierId") UUID supplierId,
                                                                @Param("productId") UUID productId,
                                                                @Param("regionId") UUID regionId,
                                                                @Param("locationId") UUID locationId,
                                                                @Param("startDateTime") LocalDateTime startDateTime,
                                                                @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT COALESCE(SUM(COALESCE(p.weight, 0) * soi.quantityDelivered / 1000.0), 0) " +
            "FROM SalesOrders so " +
            "JOIN so.customer c " +
            "JOIN so.salesOrderItems soi " +
            "JOIN soi.product p " +
            "JOIN p.suppliers s " +
            "WHERE s.publicId = :supplierId " +
            "AND p.publicId = :productId " +
            "AND so.salesOrderStatus = 'DELIVERED' " +
            "AND soi.quantityDelivered > 0 " +
            "AND (COALESCE(:regionId, c.organizationRegions.publicId) = c.organizationRegions.publicId) " +
            "AND (:locationId IS NULL OR c.organizationLocation.publicId = :locationId) " +
            "AND so.creationTime >= COALESCE(CAST(:startDateTime AS timestamp), so.creationTime) " +
            "AND so.creationTime <= COALESCE(CAST(:endDateTime AS timestamp), so.creationTime)")
    BigDecimal sumSettledTonnesBySupplierProductRegionAndLocation(@Param("supplierId") UUID supplierId,
                                                                  @Param("productId") UUID productId,
                                                                  @Param("regionId") UUID regionId,
                                                                  @Param("locationId") UUID locationId,
                                                                  @Param("startDateTime") LocalDateTime startDateTime,
                                                                  @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT COALESCE(SUM(COALESCE(p.buyingPrice, 0) * soi.quantityDelivered), 0) " +
            "FROM SalesOrders so " +
            "JOIN so.customer c " +
            "JOIN so.salesOrderItems soi " +
            "JOIN soi.product p " +
            "JOIN p.suppliers s " +
            "WHERE s.publicId = :supplierId " +
            "AND p.publicId = :productId " +
            "AND so.salesOrderStatus = 'DELIVERED' " +
            "AND soi.quantityDelivered > 0 " +
            "AND (COALESCE(:regionId, c.organizationRegions.publicId) = c.organizationRegions.publicId) " +
            "AND (:locationId IS NULL OR c.organizationLocation.publicId = :locationId) " +
            "AND so.creationTime >= COALESCE(CAST(:startDateTime AS timestamp), so.creationTime) " +
            "AND so.creationTime <= COALESCE(CAST(:endDateTime AS timestamp), so.creationTime)")
    BigDecimal sumSupplierPortalCogsBySupplierProductRegionAndLocation(@Param("supplierId") UUID supplierId,
                                                                       @Param("productId") UUID productId,
                                                                       @Param("regionId") UUID regionId,
                                                                       @Param("locationId") UUID locationId,
                                                                       @Param("startDateTime") LocalDateTime startDateTime,
                                                                       @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT COUNT(DISTINCT c) " +
            "FROM SalesOrders so " +
            "JOIN so.customer c " +
            "JOIN so.salesOrderItems soi " +
            "JOIN soi.product p " +
            "JOIN p.suppliers s " +
            "WHERE s.publicId = :supplierId " +
            "AND p.publicId = :productId " +
            "AND so.salesOrderStatus IN ('DELIVERED', 'ORDER_PLACED', 'IN_TRANSIT') " +
            "AND c.active = TRUE " +
            "AND (:regionFilterDisabled = true OR c.organizationRegions.publicId IN :regionIds) " +
            "AND (:locationFilterDisabled = true OR c.organizationLocation.publicId IN :locationIds) " +
            "AND so.creationTime >= COALESCE(CAST(:startDateTime AS timestamp), so.creationTime) " +
            "AND so.creationTime <= COALESCE(CAST(:endDateTime AS timestamp), so.creationTime)")
    Long countActiveCustomersBySupplierProductAndGeography(@Param("supplierId") UUID supplierId,
                                                           @Param("productId") UUID productId,
                                                           @Param("regionIds") List<UUID> regionIds,
                                                           @Param("regionFilterDisabled") boolean regionFilterDisabled,
                                                           @Param("locationIds") List<UUID> locationIds,
                                                           @Param("locationFilterDisabled") boolean locationFilterDisabled,
                                                           @Param("startDateTime") LocalDateTime startDateTime,
                                                           @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT COUNT(DISTINCT so) " +
            "FROM SalesOrders so " +
            "JOIN so.customer c " +
            "JOIN so.salesOrderItems soi " +
            "JOIN soi.product p " +
            "JOIN p.suppliers s " +
            "WHERE s.publicId = :supplierId " +
            "AND p.publicId = :productId " +
            "AND so.salesOrderStatus = 'DELIVERED' " +
            "AND soi.quantityDelivered > 0 " +
            "AND (:regionFilterDisabled = true OR c.organizationRegions.publicId IN :regionIds) " +
            "AND (:locationFilterDisabled = true OR c.organizationLocation.publicId IN :locationIds) " +
            "AND so.creationTime >= COALESCE(CAST(:startDateTime AS timestamp), so.creationTime) " +
            "AND so.creationTime <= COALESCE(CAST(:endDateTime AS timestamp), so.creationTime)")
    Long countDeliveredOrdersBySupplierProductAndGeography(@Param("supplierId") UUID supplierId,
                                                           @Param("productId") UUID productId,
                                                           @Param("regionIds") List<UUID> regionIds,
                                                           @Param("regionFilterDisabled") boolean regionFilterDisabled,
                                                           @Param("locationIds") List<UUID> locationIds,
                                                           @Param("locationFilterDisabled") boolean locationFilterDisabled,
                                                           @Param("startDateTime") LocalDateTime startDateTime,
                                                           @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT COALESCE(SUM(COALESCE(p.weight, 0) * soi.quantityDelivered / 1000.0), 0) " +
            "FROM SalesOrders so " +
            "JOIN so.customer c " +
            "JOIN so.salesOrderItems soi " +
            "JOIN soi.product p " +
            "JOIN p.suppliers s " +
            "WHERE s.publicId = :supplierId " +
            "AND p.publicId = :productId " +
            "AND so.salesOrderStatus = 'DELIVERED' " +
            "AND soi.quantityDelivered > 0 " +
            "AND (:regionFilterDisabled = true OR c.organizationRegions.publicId IN :regionIds) " +
            "AND (:locationFilterDisabled = true OR c.organizationLocation.publicId IN :locationIds) " +
            "AND so.creationTime >= COALESCE(CAST(:startDateTime AS timestamp), so.creationTime) " +
            "AND so.creationTime <= COALESCE(CAST(:endDateTime AS timestamp), so.creationTime)")
    BigDecimal sumSettledTonnesBySupplierProductAndGeography(@Param("supplierId") UUID supplierId,
                                                             @Param("productId") UUID productId,
                                                             @Param("regionIds") List<UUID> regionIds,
                                                             @Param("regionFilterDisabled") boolean regionFilterDisabled,
                                                             @Param("locationIds") List<UUID> locationIds,
                                                             @Param("locationFilterDisabled") boolean locationFilterDisabled,
                                                             @Param("startDateTime") LocalDateTime startDateTime,
                                                             @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT COALESCE(SUM(COALESCE(p.buyingPrice, 0) * soi.quantityDelivered), 0) " +
            "FROM SalesOrders so " +
            "JOIN so.customer c " +
            "JOIN so.salesOrderItems soi " +
            "JOIN soi.product p " +
            "JOIN p.suppliers s " +
            "WHERE s.publicId = :supplierId " +
            "AND p.publicId = :productId " +
            "AND so.salesOrderStatus = 'DELIVERED' " +
            "AND soi.quantityDelivered > 0 " +
            "AND (:regionFilterDisabled = true OR c.organizationRegions.publicId IN :regionIds) " +
            "AND (:locationFilterDisabled = true OR c.organizationLocation.publicId IN :locationIds) " +
            "AND so.creationTime >= COALESCE(CAST(:startDateTime AS timestamp), so.creationTime) " +
            "AND so.creationTime <= COALESCE(CAST(:endDateTime AS timestamp), so.creationTime)")
    BigDecimal sumSupplierPortalCogsBySupplierProductAndGeography(@Param("supplierId") UUID supplierId,
                                                                   @Param("productId") UUID productId,
                                                                   @Param("regionIds") List<UUID> regionIds,
                                                                   @Param("regionFilterDisabled") boolean regionFilterDisabled,
                                                                   @Param("locationIds") List<UUID> locationIds,
                                                                   @Param("locationFilterDisabled") boolean locationFilterDisabled,
                                                                   @Param("startDateTime") LocalDateTime startDateTime,
                                                                   @Param("endDateTime") LocalDateTime endDateTime);

    /**
     * Product-scoped daily aggregates for sales-trend and sales-calendar.
     * Column order: cogs_tzs, day, delivered_order_count, distinct_customer_count.
     */
    @Query(value = "SELECT COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0), " +
            "CAST(so.creation_time AS date), " +
            "COUNT(DISTINCT so.public_id), " +
            "COUNT(DISTINCT c.public_id) " +
            "FROM itibari.sales_order_items soi " +
            "JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE " +
            "JOIN itibari.customers c ON c.id = so.sales_order_customer_fk " +
            "JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk " +
            "LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk " +
            "JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE " +
            "WHERE soi.deleted IS NOT TRUE " +
            "AND so.loading_status = 'DELIVERED' " +
            "AND soi.quantity_delivered > 0 " +
            "AND pr.public_id = :productId " +
            "AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds)) " +
            "AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds)) " +
            "AND so.creation_time >= :fromTs " +
            "AND so.creation_time <= :toTs " +
            "AND EXISTS (" +
            "  SELECT 1 FROM itibari.product_suppliers ps " +
            "  JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE " +
            "  WHERE ps.product_id = pr.id AND sup.public_id = :supplierId" +
            ") " +
            "GROUP BY CAST(so.creation_time AS date) " +
            "ORDER BY CAST(so.creation_time AS date)",
            nativeQuery = true)
    List<Object[]> aggregateSupplierPortalLineRevenueByOrderCreationDayForProduct(
            @Param("supplierId") UUID supplierId,
            @Param("productId") UUID productId,
            @Param("regionIds") List<UUID> regionIds,
            @Param("regionFilterDisabled") boolean regionFilterDisabled,
            @Param("locationIds") List<UUID> locationIds,
            @Param("locationFilterDisabled") boolean locationFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    /**
     * Product-scoped monthly aggregates for sales-trend year-only mode.
     * Column order: cogs_tzs, year, month, delivered_order_count, distinct_customer_count.
     */
    @Query(value = "SELECT COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0), " +
            "CAST(EXTRACT(YEAR FROM so.creation_time) AS integer), " +
            "CAST(EXTRACT(MONTH FROM so.creation_time) AS integer), " +
            "COUNT(DISTINCT so.public_id), " +
            "COUNT(DISTINCT c.public_id) " +
            "FROM itibari.sales_order_items soi " +
            "JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE " +
            "JOIN itibari.customers c ON c.id = so.sales_order_customer_fk " +
            "JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk " +
            "LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk " +
            "JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE " +
            "WHERE soi.deleted IS NOT TRUE " +
            "AND so.loading_status = 'DELIVERED' " +
            "AND soi.quantity_delivered > 0 " +
            "AND pr.public_id = :productId " +
            "AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds)) " +
            "AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds)) " +
            "AND so.creation_time >= :fromTs " +
            "AND so.creation_time <= :toTs " +
            "AND EXISTS (" +
            "  SELECT 1 FROM itibari.product_suppliers ps " +
            "  JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE " +
            "  WHERE ps.product_id = pr.id AND sup.public_id = :supplierId" +
            ") " +
            "GROUP BY EXTRACT(YEAR FROM so.creation_time), EXTRACT(MONTH FROM so.creation_time) " +
            "ORDER BY EXTRACT(YEAR FROM so.creation_time), EXTRACT(MONTH FROM so.creation_time)",
            nativeQuery = true)
    List<Object[]> aggregateSupplierPortalLineRevenueByOrderCreationMonthForProduct(
            @Param("supplierId") UUID supplierId,
            @Param("productId") UUID productId,
            @Param("regionIds") List<UUID> regionIds,
            @Param("regionFilterDisabled") boolean regionFilterDisabled,
            @Param("locationIds") List<UUID> locationIds,
            @Param("locationFilterDisabled") boolean locationFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    /**
     * Product Analysis ward buckets for one SKU — grouped by {@code customers.organization_locations}.
     * Ordered by settled tonnes descending (rank assigned in service).
     */
    @Query(
            value = """
                    SELECT ol.public_id, COALESCE(ol.name, 'UNKNOWN'),
                           COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND pr.public_id = :productId
                    AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                    AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY ol.public_id, ol.name
                    ORDER BY COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0) DESC
                    """,
            nativeQuery = true)
    List<Object[]> listSupplierPortalProductWardRankingBySettledTonnes(
            @Param("supplierId") UUID supplierId,
            @Param("productId") UUID productId,
            @Param("regionIds") List<UUID> regionIds,
            @Param("regionFilterDisabled") boolean regionFilterDisabled,
            @Param("locationIds") List<UUID> locationIds,
            @Param("locationFilterDisabled") boolean locationFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    /**
     * Product Analysis region (district) buckets for one SKU — grouped by {@code organization_regions} when no filter.
     */
    @Query(
            value = """
                    SELECT r.public_id, COALESCE(r.name, 'UNKNOWN'),
                           COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND pr.public_id = :productId
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY r.public_id, r.name
                    ORDER BY COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0) DESC
                    """,
            nativeQuery = true)
    List<Object[]> listSupplierPortalProductRegionRankingBySettledTonnes(
            @Param("supplierId") UUID supplierId,
            @Param("productId") UUID productId,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    /**
     * Product Analysis ward COGS buckets for one SKU — grouped by {@code customers.organization_locations}.
     * Ordered by portal COGS descending (limit applied in service).
     */
    @Query(
            value = """
                    SELECT ol.public_id, COALESCE(ol.name, 'UNKNOWN'),
                           COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND pr.public_id = :productId
                    AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                    AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY ol.public_id, ol.name
                    ORDER BY COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0) DESC
                    """,
            nativeQuery = true)
    List<Object[]> listSupplierPortalProductWardCogsByTotalCogs(
            @Param("supplierId") UUID supplierId,
            @Param("productId") UUID productId,
            @Param("regionIds") List<UUID> regionIds,
            @Param("regionFilterDisabled") boolean regionFilterDisabled,
            @Param("locationIds") List<UUID> locationIds,
            @Param("locationFilterDisabled") boolean locationFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    /**
     * Product Analysis region (district) COGS buckets for one SKU — grouped by {@code organization_regions} when no filter.
     */
    @Query(
            value = """
                    SELECT r.public_id, COALESCE(r.name, 'UNKNOWN'),
                           COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND pr.public_id = :productId
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY r.public_id, r.name
                    ORDER BY COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0) DESC
                    """,
            nativeQuery = true)
    List<Object[]> listSupplierPortalProductRegionCogsByTotalCogs(
            @Param("supplierId") UUID supplierId,
            @Param("productId") UUID productId,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    /**
     * Product Analysis paged ward performance for one SKU — grouped by {@code customers.organization_locations}.
     * Fixed order: settled tonnes descending, ward name ascending (tie-break).
     */
    @Query(
            value = """
                    SELECT ol.public_id, COALESCE(ol.name, 'UNKNOWN'),
                           COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0),
                           COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0),
                           COUNT(DISTINCT c.public_id),
                           COUNT(DISTINCT so.public_id)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND pr.public_id = :productId
                    AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                    AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY ol.public_id, ol.name
                    ORDER BY COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0) DESC,
                             COALESCE(ol.name, 'UNKNOWN') ASC
                    """,
            countQuery = """
                    SELECT COUNT(*) FROM (
                       SELECT ol.public_id
                       FROM itibari.sales_order_items soi
                       INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                       INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                       INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                       LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                       INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                       WHERE soi.deleted IS NOT TRUE
                       AND so.loading_status = 'DELIVERED'
                       AND soi.quantity_delivered > 0
                       AND pr.public_id = :productId
                       AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                       AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                       AND so.creation_time >= :fromTs
                       AND so.creation_time <= :toTs
                       AND EXISTS (
                          SELECT 1 FROM itibari.product_suppliers ps
                          INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                          WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                       )
                       GROUP BY ol.public_id, ol.name
                    ) grouped_wards
                    """,
            nativeQuery = true)
    Page<Object[]> pageSupplierPortalProductWardPerformanceV2(
            @Param("supplierId") UUID supplierId,
            @Param("productId") UUID productId,
            @Param("regionIds") List<UUID> regionIds,
            @Param("regionFilterDisabled") boolean regionFilterDisabled,
            @Param("locationIds") List<UUID> locationIds,
            @Param("locationFilterDisabled") boolean locationFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs,
            Pageable pageable);

    /**
     * Unpaged variant of {@link #pageSupplierPortalProductWardPerformanceV2} for previous-window trend merge.
     */
    @Query(
            value = """
                    SELECT ol.public_id, COALESCE(ol.name, 'UNKNOWN'),
                           COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0),
                           COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0),
                           COUNT(DISTINCT c.public_id),
                           COUNT(DISTINCT so.public_id)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND pr.public_id = :productId
                    AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                    AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY ol.public_id, ol.name
                    ORDER BY COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0) DESC,
                             COALESCE(ol.name, 'UNKNOWN') ASC
                    """,
            nativeQuery = true)
    List<Object[]> listSupplierPortalProductWardPerformanceV2(
            @Param("supplierId") UUID supplierId,
            @Param("productId") UUID productId,
            @Param("regionIds") List<UUID> regionIds,
            @Param("regionFilterDisabled") boolean regionFilterDisabled,
            @Param("locationIds") List<UUID> locationIds,
            @Param("locationFilterDisabled") boolean locationFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    @Query("SELECT COUNT(DISTINCT so) " +
            "FROM SalesOrders so " +
            "JOIN so.customer c " +
            "JOIN so.salesOrderItems soi " +
            "JOIN soi.product p " +
            "JOIN p.suppliers s " +
            "WHERE s.publicId = :supplierId " +
            "AND so.salesOrderStatus = 'DELIVERED' " +
            "AND soi.quantityDelivered > 0 " +
            "AND (:regionFilterDisabled = true OR c.organizationRegions.publicId IN :regionIds) " +
            "AND (:locationFilterDisabled = true OR c.organizationLocation.publicId IN :locationIds) " +
            "AND (:productFilterDisabled = true OR p.publicId IN :productIds) " +
            "AND so.creationTime >= COALESCE(CAST(:startDateTime AS timestamp), so.creationTime) " +
            "AND so.creationTime <= COALESCE(CAST(:endDateTime AS timestamp), so.creationTime)")
    Long countDeliveredOrdersForAllSupplierProductsByRegionAndLocation(@Param("supplierId") UUID supplierId,
                                                                       @Param("regionIds") List<UUID> regionIds,
                                                                       @Param("regionFilterDisabled") boolean regionFilterDisabled,
                                                                       @Param("locationIds") List<UUID> locationIds,
                                                                       @Param("locationFilterDisabled") boolean locationFilterDisabled,
                                                                       @Param("productIds") List<UUID> productIds,
                                                                       @Param("productFilterDisabled") boolean productFilterDisabled,
                                                                       @Param("startDateTime") LocalDateTime startDateTime,
                                                                       @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT COALESCE(SUM(COALESCE(p.weight, 0) * soi.quantityDelivered / 1000.0), 0) " +
            "FROM SalesOrders so " +
            "JOIN so.customer c " +
            "JOIN so.salesOrderItems soi " +
            "JOIN soi.product p " +
            "JOIN p.suppliers s " +
            "WHERE s.publicId = :supplierId " +
            "AND so.salesOrderStatus = 'DELIVERED' " +
            "AND soi.quantityDelivered > 0 " +
            "AND (:regionFilterDisabled = true OR c.organizationRegions.publicId IN :regionIds) " +
            "AND (:locationFilterDisabled = true OR c.organizationLocation.publicId IN :locationIds) " +
            "AND (:productFilterDisabled = true OR p.publicId IN :productIds) " +
            "AND so.creationTime >= COALESCE(CAST(:startDateTime AS timestamp), so.creationTime) " +
            "AND so.creationTime <= COALESCE(CAST(:endDateTime AS timestamp), so.creationTime)")
    BigDecimal sumSettledTonnesForAllSupplierProductsByRegionAndLocation(@Param("supplierId") UUID supplierId,
                                                                         @Param("regionIds") List<UUID> regionIds,
                                                                         @Param("regionFilterDisabled") boolean regionFilterDisabled,
                                                                         @Param("locationIds") List<UUID> locationIds,
                                                                         @Param("locationFilterDisabled") boolean locationFilterDisabled,
                                                                         @Param("productIds") List<UUID> productIds,
                                                                         @Param("productFilterDisabled") boolean productFilterDisabled,
                                                                         @Param("startDateTime") LocalDateTime startDateTime,
                                                                         @Param("endDateTime") LocalDateTime endDateTime);

    /**
     * All-products daily aggregates for sales-trend and sales-calendar.
     * Column order: cogs_tzs, day, delivered_order_count, distinct_customer_count.
     * COGS uses line SUM(price × qty); orders/customers use COUNT(DISTINCT …) per calendar day.
     */
    @Query(value = "SELECT COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0), " +
            "CAST(so.creation_time AS date), " +
            "COUNT(DISTINCT so.public_id), " +
            "COUNT(DISTINCT c.public_id) " +
            "FROM itibari.sales_order_items soi " +
            "JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE " +
            "JOIN itibari.customers c ON c.id = so.sales_order_customer_fk " +
            "JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk " +
            "LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk " +
            "JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE " +
            "WHERE soi.deleted IS NOT TRUE " +
            "AND so.loading_status = 'DELIVERED' " +
            "AND soi.quantity_delivered > 0 " +
            "AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds)) " +
            "AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds)) " +
            "AND so.creation_time >= :fromTs " +
            "AND so.creation_time <= :toTs " +
            "AND EXISTS (" +
            "  SELECT 1 FROM itibari.product_suppliers ps " +
            "  JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE " +
            "  WHERE ps.product_id = pr.id AND sup.public_id = :supplierId" +
            ") " +
            "GROUP BY CAST(so.creation_time AS date) " +
            "ORDER BY CAST(so.creation_time AS date)",
            nativeQuery = true)
    List<Object[]> aggregateSupplierPortalLineRevenueByOrderCreationDayForAllSupplierProducts(
            @Param("supplierId") UUID supplierId,
            @Param("regionIds") List<UUID> regionIds,
            @Param("regionFilterDisabled") boolean regionFilterDisabled,
            @Param("locationIds") List<UUID> locationIds,
            @Param("locationFilterDisabled") boolean locationFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    /**
     * All-products monthly aggregates for sales-trend year-only mode.
     * Column order: cogs_tzs, year, month, delivered_order_count, distinct_customer_count.
     */
    @Query(value = "SELECT COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0), " +
            "CAST(EXTRACT(YEAR FROM so.creation_time) AS integer), " +
            "CAST(EXTRACT(MONTH FROM so.creation_time) AS integer), " +
            "COUNT(DISTINCT so.public_id), " +
            "COUNT(DISTINCT c.public_id) " +
            "FROM itibari.sales_order_items soi " +
            "JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE " +
            "JOIN itibari.customers c ON c.id = so.sales_order_customer_fk " +
            "JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk " +
            "LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk " +
            "JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE " +
            "WHERE soi.deleted IS NOT TRUE " +
            "AND so.loading_status = 'DELIVERED' " +
            "AND soi.quantity_delivered > 0 " +
            "AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds)) " +
            "AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds)) " +
            "AND so.creation_time >= :fromTs " +
            "AND so.creation_time <= :toTs " +
            "AND EXISTS (" +
            "  SELECT 1 FROM itibari.product_suppliers ps " +
            "  JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE " +
            "  WHERE ps.product_id = pr.id AND sup.public_id = :supplierId" +
            ") " +
            "GROUP BY EXTRACT(YEAR FROM so.creation_time), EXTRACT(MONTH FROM so.creation_time) " +
            "ORDER BY EXTRACT(YEAR FROM so.creation_time), EXTRACT(MONTH FROM so.creation_time)",
            nativeQuery = true)
    List<Object[]> aggregateSupplierPortalLineRevenueByOrderCreationMonthForAllSupplierProducts(
            @Param("supplierId") UUID supplierId,
            @Param("regionIds") List<UUID> regionIds,
            @Param("regionFilterDisabled") boolean regionFilterDisabled,
            @Param("locationIds") List<UUID> locationIds,
            @Param("locationFilterDisabled") boolean locationFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    /**
     * Delivered-order counts by calendar month for customer-analysis orders-trend year-only mode.
     * Column order: year, month, distinct_delivered_order_count.
     */
    @Query(value = "SELECT CAST(EXTRACT(YEAR FROM so.creation_time) AS integer), " +
            "CAST(EXTRACT(MONTH FROM so.creation_time) AS integer), " +
            "COUNT(DISTINCT so.public_id) " +
            "FROM itibari.sales_order_items soi " +
            "JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE " +
            "JOIN itibari.customers c ON c.id = so.sales_order_customer_fk " +
            "JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk " +
            "LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk " +
            "JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE " +
            "WHERE soi.deleted IS NOT TRUE " +
            "AND so.loading_status = 'DELIVERED' " +
            "AND soi.quantity_delivered > 0 " +
            "AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds)) " +
            "AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds)) " +
            "AND (:productFilterDisabled = true OR pr.public_id IN (:productIds)) " +
            "AND so.creation_time >= :fromTs " +
            "AND so.creation_time <= :toTs " +
            "AND EXISTS (" +
            "  SELECT 1 FROM itibari.product_suppliers ps " +
            "  JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE " +
            "  WHERE ps.product_id = pr.id AND sup.public_id = :supplierId" +
            ") " +
            "GROUP BY EXTRACT(YEAR FROM so.creation_time), EXTRACT(MONTH FROM so.creation_time) " +
            "ORDER BY EXTRACT(YEAR FROM so.creation_time), EXTRACT(MONTH FROM so.creation_time)",
            nativeQuery = true)
    List<Object[]> aggregateSupplierPortalDeliveredOrdersByCreationMonth(
            @Param("supplierId") UUID supplierId,
            @Param("regionIds") List<UUID> regionIds,
            @Param("regionFilterDisabled") boolean regionFilterDisabled,
            @Param("locationIds") List<UUID> locationIds,
            @Param("locationFilterDisabled") boolean locationFilterDisabled,
            @Param("productIds") List<UUID> productIds,
            @Param("productFilterDisabled") boolean productFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    @Query(
            value = """
                    SELECT ol.public_id, COALESCE(ol.name, 'UNKNOWN'),
                           COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0),
                           COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0),
                           COUNT(DISTINCT so.public_id)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                    AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY ol.public_id, ol.name
                    ORDER BY COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0) DESC
                    """,
            nativeQuery = true)
    List<Object[]> listSupplierPortalAllProductsWardRankingBySettledTonnes(
            @Param("supplierId") UUID supplierId,
            @Param("regionIds") List<UUID> regionIds,
            @Param("regionFilterDisabled") boolean regionFilterDisabled,
            @Param("locationIds") List<UUID> locationIds,
            @Param("locationFilterDisabled") boolean locationFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    /**
     * All-products region (district) ranking — grouped by {@code organization_regions} when no region/ward filter.
     */
    @Query(
            value = """
                    SELECT r.public_id, COALESCE(r.name, 'UNKNOWN'),
                           COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0),
                           COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0),
                           COUNT(DISTINCT so.public_id)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY r.public_id, r.name
                    ORDER BY COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0) DESC
                    """,
            nativeQuery = true)
    List<Object[]> listSupplierPortalAllProductsRegionRankingBySettledTonnes(
            @Param("supplierId") UUID supplierId,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    @Query(
            value = """
                    SELECT ol.public_id, COALESCE(ol.name, 'UNKNOWN'),
                           COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                    AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                    AND (:productFilterDisabled = true OR pr.public_id IN (:productIds))
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY ol.public_id, ol.name
                    ORDER BY COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0) DESC
                    """,
            nativeQuery = true)
    List<Object[]> listSupplierPortalAllProductsWardCogsByTotalCogs(
            @Param("supplierId") UUID supplierId,
            @Param("regionIds") List<UUID> regionIds,
            @Param("regionFilterDisabled") boolean regionFilterDisabled,
            @Param("locationIds") List<UUID> locationIds,
            @Param("locationFilterDisabled") boolean locationFilterDisabled,
            @Param("productIds") List<UUID> productIds,
            @Param("productFilterDisabled") boolean productFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    /**
     * Product Analysis region (district) COGS buckets — all supplier products, grouped by {@code organization_regions}.
     */
    @Query(
            value = """
                    SELECT r.public_id, COALESCE(r.name, 'UNKNOWN'),
                           COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY r.public_id, r.name
                    ORDER BY COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0) DESC
                    """,
            nativeQuery = true)
    List<Object[]> listSupplierPortalAllProductsRegionCogsByTotalCogs(
            @Param("supplierId") UUID supplierId,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    @Query(
            value = """
                    SELECT ol.public_id, COALESCE(ol.name, 'UNKNOWN'),
                           COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0),
                           COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0),
                           COUNT(DISTINCT c.public_id),
                           COUNT(DISTINCT so.public_id)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                    AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY ol.public_id, ol.name
                    ORDER BY COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0) DESC,
                             COALESCE(ol.name, 'UNKNOWN') ASC
                    """,
            countQuery = """
                    SELECT COUNT(*) FROM (
                       SELECT ol.public_id
                       FROM itibari.sales_order_items soi
                       INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                       INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                       INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                       LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                       INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                       WHERE soi.deleted IS NOT TRUE
                       AND so.loading_status = 'DELIVERED'
                       AND soi.quantity_delivered > 0
                       AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                       AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                       AND so.creation_time >= :fromTs
                       AND so.creation_time <= :toTs
                       AND EXISTS (
                          SELECT 1 FROM itibari.product_suppliers ps
                          INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                          WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                       )
                       GROUP BY ol.public_id, ol.name
                    ) grouped_wards
                    """,
            nativeQuery = true)
    Page<Object[]> pageSupplierPortalAllProductsWardPerformanceV2(
            @Param("supplierId") UUID supplierId,
            @Param("regionIds") List<UUID> regionIds,
            @Param("regionFilterDisabled") boolean regionFilterDisabled,
            @Param("locationIds") List<UUID> locationIds,
            @Param("locationFilterDisabled") boolean locationFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs,
            Pageable pageable);

    /**
     * Unpaged variant of {@link #pageSupplierPortalAllProductsWardPerformanceV2} for previous-window trend merge.
     */
    @Query(
            value = """
                    SELECT ol.public_id, COALESCE(ol.name, 'UNKNOWN'),
                           COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0),
                           COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0),
                           COUNT(DISTINCT c.public_id),
                           COUNT(DISTINCT so.public_id)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                    AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY ol.public_id, ol.name
                    ORDER BY COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0) DESC,
                             COALESCE(ol.name, 'UNKNOWN') ASC
                    """,
            nativeQuery = true)
    List<Object[]> listSupplierPortalAllProductsWardPerformanceV2(
            @Param("supplierId") UUID supplierId,
            @Param("regionIds") List<UUID> regionIds,
            @Param("regionFilterDisabled") boolean regionFilterDisabled,
            @Param("locationIds") List<UUID> locationIds,
            @Param("locationFilterDisabled") boolean locationFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    @Query("SELECT COALESCE(c.organizationLocation.name, 'UNKNOWN') AS zoneName, " +
            "ROUND(AVG(COALESCE(p.buyingPrice, 0))) AS averageTotalAmount " +
            "FROM SalesOrders o " +
            "JOIN o.customer c " +
            "JOIN o.salesOrderItems i " +
            "JOIN i.product p " +
            "WHERE o.salesOrderStatus = 'DELIVERED' " +
            "AND p.publicId = :productId " +
            "AND o.creationTime BETWEEN :startDate AND :endDate " +
            "GROUP BY c.organizationLocation.name")
    List<ZoneAverageProjection> findProductPerformanceByProduct(
            @Param("productId") UUID productId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(c.organizationLocation.name, 'UNKNOWN') AS zoneName, " +
            "ROUND(AVG(COALESCE(p.buyingPrice, 0))) AS averageTotalAmount " +
            "FROM SalesOrders o " +
            "JOIN o.customer c " +
            "JOIN o.salesOrderItems i " +
            "JOIN i.product p " +
            "WHERE o.salesOrderStatus = 'DELIVERED' " +
            "AND c.organizationLocation.publicId IN :organizationLocationIds " +
            "AND o.creationTime BETWEEN :startDate AND :endDate " +
            "GROUP BY c.organizationLocation.name")
    List<ZoneAverageProjection> findProductPerformanceByRegion(
            @Param("organizationLocationIds") List<UUID> organizationLocationIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(c.organizationLocation.name, 'UNKNOWN') AS zoneName, " +
            "ROUND(AVG(COALESCE(p.buyingPrice, 0))) AS averageTotalAmount " +
            "FROM SalesOrders o " +
            "JOIN o.customer c " +
            "JOIN o.salesOrderItems i " +
            "JOIN i.product p " +
            "WHERE o.salesOrderStatus = 'DELIVERED' " +
            "AND o.creationTime BETWEEN :startDate AND :endDate " +
            "GROUP BY c.organizationLocation.name")
    List<ZoneAverageProjection> findProductPerformance(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT so FROM SalesOrders so WHERE so.receiptFailed = true AND so.deleted = false")
    List<SalesOrders> findAllByReceiptFailedIsTrueAndDeletedIsFalse();





    /*
       Qualifying orders: not deleted, loading_status = 'DELIVERED', credit_applied = false, within the last 90 days.

       --- INACTIVE (2026-06): former 3-orders-per-month × 3-months gate ---
       1. The inner subquery calculated distinct months with at least 3 qualifying orders in the last 90 days.
       2. The outer query required that count >= 3 before returning AVG(total_amount) over the same window; otherwise 0.
       "It should capture all clients who have monthly average purchases that are minimum of 3 purchases,
        this means that in 3months / 90days we will consider a client who have purchased 9times with us."
       --- END INACTIVE ---

       Active rule: at least one qualifying order in the last 90 days, then average order value over that window; otherwise 0.
     */

    @Query(value = "SELECT CASE " +
            "WHEN EXISTS ( " +
            "SELECT 1 FROM itibari.sales_orders so_gate " +
            "JOIN itibari.customers c_gate ON c_gate.id = so_gate.sales_order_customer_fk " +
            "WHERE c_gate.public_id = :customerId " +
            "AND so_gate.deleted IS NOT TRUE " +
            "AND so_gate.loading_status = 'DELIVERED' " +
            "AND so_gate.credit_applied = false " +
            "AND so_gate.creation_time >= (CURRENT_TIMESTAMP - INTERVAL '90 days') " +
            ") THEN COALESCE(( " +
            "SELECT AVG(so.total_amount) FROM itibari.sales_orders so " +
            "JOIN itibari.customers c ON c.id = so.sales_order_customer_fk " +
            "WHERE c.public_id = :customerId " +
            "AND so.deleted IS NOT TRUE " +
            "AND so.loading_status = 'DELIVERED' " +
            "AND so.credit_applied = false " +
            "AND so.creation_time >= (CURRENT_TIMESTAMP - INTERVAL '90 days') " +
            "), 0) " +
            "ELSE 0 END",
            nativeQuery = true)
    BigDecimal calculateAverageOrder(@Param("customerId") UUID customerId);

    // Require at least three qualifying orders in each of the last three completed calendar months.
    @Query(value = "SELECT CASE WHEN COUNT(*) = 3 THEN true ELSE false END " +
            "FROM ( " +
            "SELECT DATE_TRUNC('month', so.creation_time) AS month_bucket, " +
            "COUNT(*) AS order_count " +
            "FROM itibari.sales_orders so " +
            "JOIN itibari.customers c ON c.id = so.sales_order_customer_fk " +
            "WHERE c.public_id = :customerId " +
            "AND c.deleted = false " +
            "AND so.deleted = false " +
            "AND so.loading_status = 'DELIVERED' " +
            "AND so.credit_applied = false " +
            "AND so.creation_time >= DATE_TRUNC('month', CURRENT_TIMESTAMP AT TIME ZONE 'Africa/Nairobi') - INTERVAL '3 months' " +
            "AND so.creation_time < DATE_TRUNC('month', CURRENT_TIMESTAMP AT TIME ZONE 'Africa/Nairobi') " +
            "GROUP BY DATE_TRUNC('month', so.creation_time) " +
            "HAVING COUNT(*) >= 3 " +
            ") monthly_counts",
            nativeQuery = true)
    Boolean customerQualifiesForAOV(@Param("customerId") UUID customerId);

//    // Query to calculate average
    @Query(value = "SELECT COALESCE(AVG(so.total_amount), 0) " +
            "FROM itibari.sales_orders so " +
            "JOIN itibari.customers c ON c.id = so.sales_order_customer_fk " +
            "WHERE c.public_id = :customerId " +
            "AND so.deleted = false " +
            "AND so.loading_status = 'DELIVERED' " +
            "AND so.credit_applied = false " +
            "AND so.creation_time >= (CURRENT_TIMESTAMP - INTERVAL '90 days')",
            nativeQuery = true)
    BigDecimal calculateAverageOrderValue(UUID customerId);

    List<SalesOrders> findAllByCreditAppliedIsTrueAndDeletedIsFalse();

    Optional<SalesOrders> findTopByCustomerAndDeletedFalseOrderByCreationTimeDesc(Customers customer);

    List<SalesOrders> findTop5ByCustomerAndDeletedFalseOrderByCreationTimeDesc(Customers customer);

    List<SalesOrders> findBySmsNotifyInPlanningIsFalseAndSalesOrderStatusAndDeletedIsFalse(LoadingStatusEnum loadingStatusEnum);

    List<SalesOrders> findBySmsNotifyInTransitIsFalseAndSalesOrderStatusAndDeletedIsFalse(LoadingStatusEnum loadingStatusEnum);

    /**
     * Customer Analysis V2 — supplier-attributed COGS grouped by customer.
     * Column order: customer_public_id, total_cogs_tzs.
     * Same filters as {@link #pageCustomerAnalysisCustomersV2}.
     */
    @Query(
            value = """
                    SELECT c.public_id,
                           COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND (COALESCE(:regionId, r.public_id) = r.public_id)
                    AND (:locationId IS NULL OR ol.public_id = :locationId)
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY c.public_id
                    """,
            nativeQuery = true)
    List<Object[]> listCustomerAnalysisSupplierCogsByCustomerV2(
            @Param("supplierId") UUID supplierId,
            @Param("regionId") UUID regionId,
            @Param("locationId") UUID locationId,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    /**
     * Canonical supplier-portal active customer count: {@code COUNT(DISTINCT customers)} with at least one
     * DELIVERED supplier order line ({@code quantity_delivered > 0}) in the window, scoped via
     * {@code product_suppliers}. Shared by Dashboard, Product Analysis (all-products), and Customer Analysis
     * summary-metrics for identical geography + date filters.
     */
    @Query(
            value = """
                    SELECT COUNT(DISTINCT c.public_id)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                    AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                    AND (:productFilterDisabled = true OR pr.public_id IN (:productIds))
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    """,
            nativeQuery = true)
    Long countDistinctDeliveredSupplierPortalCustomers(
            @Param("supplierId") UUID supplierId,
            @Param("regionIds") List<UUID> regionIds,
            @Param("regionFilterDisabled") boolean regionFilterDisabled,
            @Param("locationIds") List<UUID> locationIds,
            @Param("locationFilterDisabled") boolean locationFilterDisabled,
            @Param("productIds") List<UUID> productIds,
            @Param("productFilterDisabled") boolean productFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    /**
     * Customer Analysis V2 — customers whose earliest delivered supplier order (same geography filters)
     * has {@code MIN(so.creation_time)} within the window.
     */
    @Query(
            value = """
                    SELECT COUNT(*) FROM (
                       SELECT c.public_id
                       FROM itibari.sales_order_items soi
                       INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                       INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                       INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                       LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                       INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                       WHERE soi.deleted IS NOT TRUE
                       AND so.loading_status = 'DELIVERED'
                       AND soi.quantity_delivered > 0
                       AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                       AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                    AND (:productFilterDisabled = true OR pr.public_id IN (:productIds))
                       AND EXISTS (
                          SELECT 1 FROM itibari.product_suppliers ps
                          INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                          WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                       )
                       GROUP BY c.public_id
                       HAVING MIN(so.creation_time) >= :fromTs AND MIN(so.creation_time) <= :toTs
                    ) first_time_customers
                    """,
            nativeQuery = true)
    Long countNewDeliveredCustomersForCustomerAnalysis(
            @Param("supplierId") UUID supplierId,
            @Param("regionIds") List<UUID> regionIds,
            @Param("regionFilterDisabled") boolean regionFilterDisabled,
            @Param("locationIds") List<UUID> locationIds,
            @Param("locationFilterDisabled") boolean locationFilterDisabled,
            @Param("productIds") List<UUID> productIds,
            @Param("productFilterDisabled") boolean productFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    /**
     * Customer Analysis V2 — frequent customers: distinct customers with at least two delivered supplier
     * orders ({@code COUNT(DISTINCT so.public_id) >= 2}) in the filtered window.
     * Same filters as {@link #countNewDeliveredCustomersForCustomerAnalysis}.
     */
    @Query(
            value = """
                    SELECT COUNT(*) FROM (
                       SELECT c.public_id
                       FROM itibari.sales_order_items soi
                       INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                       INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                       INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                       LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                       INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                       WHERE soi.deleted IS NOT TRUE
                       AND so.loading_status = 'DELIVERED'
                       AND soi.quantity_delivered > 0
                       AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                       AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                    AND (:productFilterDisabled = true OR pr.public_id IN (:productIds))
                       AND so.creation_time >= :fromTs
                       AND so.creation_time <= :toTs
                       AND EXISTS (
                          SELECT 1 FROM itibari.product_suppliers ps
                          INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                          WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                       )
                       GROUP BY c.public_id
                       HAVING COUNT(DISTINCT so.public_id) >= 2
                    ) frequent_customers
                    """,
            nativeQuery = true)
    Long countFrequentDeliveredCustomersForCustomerAnalysis(
            @Param("supplierId") UUID supplierId,
            @Param("regionIds") List<UUID> regionIds,
            @Param("regionFilterDisabled") boolean regionFilterDisabled,
            @Param("locationIds") List<UUID> locationIds,
            @Param("locationFilterDisabled") boolean locationFilterDisabled,
            @Param("productIds") List<UUID> productIds,
            @Param("productFilterDisabled") boolean productFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    /**
     * Customer Analysis V2 district ranking — grouped by ward ({@code organization_location}), ordered by distinct customers.
     */
    @Query(
            value = """
                    SELECT ol.public_id, COALESCE(ol.name, 'UNKNOWN'),
                           COUNT(DISTINCT c.public_id),
                           COUNT(DISTINCT so.public_id),
                           COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                    AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                    AND (:productFilterDisabled = true OR pr.public_id IN (:productIds))
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY ol.public_id, ol.name
                    ORDER BY COUNT(DISTINCT c.public_id) DESC, COALESCE(ol.name, 'UNKNOWN') ASC
                    """,
            nativeQuery = true)
    List<Object[]> listCustomerAnalysisDistrictRankingV2(
            @Param("supplierId") UUID supplierId,
            @Param("regionIds") List<UUID> regionIds,
            @Param("regionFilterDisabled") boolean regionFilterDisabled,
            @Param("locationIds") List<UUID> locationIds,
            @Param("locationFilterDisabled") boolean locationFilterDisabled,
            @Param("productIds") List<UUID> productIds,
            @Param("productFilterDisabled") boolean productFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    /**
     * Customer Analysis V2 region (district) ranking — grouped by {@code organization_regions} when no region/ward filter.
     * Same metrics and order as {@link #listCustomerAnalysisDistrictRankingV2}; tie-break uses region name.
     */
    @Query(
            value = """
                    SELECT r.public_id, COALESCE(r.name, 'UNKNOWN'),
                           COUNT(DISTINCT c.public_id),
                           COUNT(DISTINCT so.public_id),
                           COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND (:productFilterDisabled = true OR pr.public_id IN (:productIds))
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY r.public_id, r.name
                    ORDER BY COUNT(DISTINCT c.public_id) DESC, COALESCE(r.name, 'UNKNOWN') ASC
                    """,
            nativeQuery = true)
    List<Object[]> listCustomerAnalysisRegionRankingV2(
            @Param("supplierId") UUID supplierId,
            @Param("productIds") List<UUID> productIds,
            @Param("productFilterDisabled") boolean productFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    /**
     * Customer Analysis V2 region (district) COGS — grouped by {@code organization_regions} when no filter.
     */
    @Query(
            value = """
                    SELECT r.public_id, COALESCE(r.name, 'UNKNOWN'),
                           COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND (:productFilterDisabled = true OR pr.public_id IN (:productIds))
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY r.public_id, r.name
                    ORDER BY COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0) DESC
                    """,
            nativeQuery = true)
    List<Object[]> listCustomerAnalysisRegionCogsV2(
            @Param("supplierId") UUID supplierId,
            @Param("productIds") List<UUID> productIds,
            @Param("productFilterDisabled") boolean productFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    /**
     * Customer Analysis V2 paged customer table — fixed order totalCogsTzs DESC, shopName ASC.
     * shop_name = store_name when non-blank; else first_name + ' ' + last_name; else customers.name.
     * Single-pass CTE (no per-row correlated subqueries) for product names, purchase quantities, and dates.
     */
    @Query(
            value = """
                    WITH base AS (
                       SELECT c.public_id,
                              COALESCE(
                                 NULLIF(BTRIM(c.store_name), ''),
                                 NULLIF(BTRIM(CONCAT_WS(' ',
                                    NULLIF(BTRIM(c.first_name), ''),
                                    NULLIF(BTRIM(c.last_name), '')
                                 )), ''),
                                 NULLIF(BTRIM(c.name), '')
                              ) AS shop_name,
                              r.public_id AS region_public_id,
                              r.name AS region_name,
                              ol.public_id AS location_public_id,
                              COALESCE(ol.name, 'UNKNOWN') AS location_name,
                              c.landmark,
                              so.public_id AS order_public_id,
                              pr.product_name,
                              COALESCE(pr.weight, 0) AS product_weight,
                              soi.quantity_delivered,
                              so.creation_time,
                              COALESCE(pr.buying_price, 0) AS buying_price
                       FROM itibari.sales_order_items soi
                       INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                       INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                       INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                       LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                       INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                       WHERE soi.deleted IS NOT TRUE
                       AND so.loading_status = 'DELIVERED'
                       AND soi.quantity_delivered > 0
                       AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                       AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                       AND (:productFilterDisabled = true OR pr.public_id IN (:productIds))
                       AND so.creation_time >= :fromTs
                       AND so.creation_time <= :toTs
                       AND (:searchPattern IS NULL OR LOWER(COALESCE(
                                 NULLIF(BTRIM(c.store_name), ''),
                                 NULLIF(BTRIM(CONCAT_WS(' ',
                                    NULLIF(BTRIM(c.first_name), ''),
                                    NULLIF(BTRIM(c.last_name), '')
                                 )), ''),
                                 NULLIF(BTRIM(c.name), '')
                              )) LIKE :searchPattern)
                       AND EXISTS (
                          SELECT 1 FROM itibari.product_suppliers ps
                          INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                          WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                       )
                    ),
                    customer_metrics AS (
                       SELECT public_id,
                              shop_name,
                              region_public_id,
                              region_name,
                              location_public_id,
                              location_name,
                              landmark,
                              COUNT(DISTINCT order_public_id) AS delivered_order_count,
                              COALESCE(SUM(buying_price * quantity_delivered), 0) AS total_cogs_tzs,
                              COALESCE(SUM(product_weight * quantity_delivered / 1000.0), 0) AS settled_tonnes,
                              CAST(MAX(creation_time) AS date) AS last_order_date
                       FROM base
                       GROUP BY public_id, shop_name, region_public_id, region_name, location_public_id, location_name, landmark
                    ),
                    product_detail AS (
                       SELECT q.public_id,
                              q.product_name,
                              q.total_qty,
                              COALESCE(d.purchase_dates, '') AS purchase_dates
                       FROM (
                          SELECT public_id,
                                 product_name,
                                 SUM(quantity_delivered) AS total_qty
                          FROM base
                          WHERE product_name IS NOT NULL
                          GROUP BY public_id, product_name
                       ) q
                       LEFT JOIN (
                          SELECT public_id,
                                 product_name,
                                 STRING_AGG(purchase_day_label, ', ' ORDER BY purchase_day) AS purchase_dates
                          FROM (
                             SELECT DISTINCT public_id,
                                    product_name,
                                    DATE(creation_time) AS purchase_day,
                                    TO_CHAR(DATE(creation_time), 'DD/MM/YYYY') AS purchase_day_label
                             FROM base
                             WHERE product_name IS NOT NULL
                          ) distinct_days
                          GROUP BY public_id, product_name
                       ) d ON d.public_id = q.public_id AND d.product_name = q.product_name
                    ),
                    product_by_customer AS (
                       SELECT public_id,
                              STRING_AGG(product_name, ', ' ORDER BY product_name) AS product_names,
                              STRING_AGG(product_name || ': ' || CAST(total_qty AS text), ', ' ORDER BY product_name) AS product_purchase_quantities,
                              STRING_AGG(product_name || ': ' || purchase_dates, ' | ' ORDER BY product_name) AS product_purchase_dates
                       FROM product_detail
                       GROUP BY public_id
                    )
                    SELECT cm.public_id,
                           cm.shop_name,
                           cm.region_public_id,
                           cm.region_name,
                           cm.location_public_id,
                           cm.location_name,
                           cm.landmark,
                           cm.delivered_order_count,
                           cm.total_cogs_tzs,
                           cm.settled_tonnes,
                           cm.last_order_date,
                           pb.product_names,
                           pb.product_purchase_quantities,
                           pb.product_purchase_dates
                    FROM customer_metrics cm
                    LEFT JOIN product_by_customer pb ON pb.public_id = cm.public_id
                    ORDER BY cm.total_cogs_tzs DESC, cm.shop_name ASC
                    """,
            countQuery = """
                    SELECT COUNT(*) FROM (
                       SELECT c.public_id
                       FROM itibari.sales_order_items soi
                       INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                       INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                       INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                       LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                       INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                       WHERE soi.deleted IS NOT TRUE
                       AND so.loading_status = 'DELIVERED'
                       AND soi.quantity_delivered > 0
                       AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                       AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                       AND (:productFilterDisabled = true OR pr.public_id IN (:productIds))
                       AND so.creation_time >= :fromTs
                       AND so.creation_time <= :toTs
                       AND (:searchPattern IS NULL OR LOWER(COALESCE(
                                 NULLIF(BTRIM(c.store_name), ''),
                                 NULLIF(BTRIM(CONCAT_WS(' ',
                                    NULLIF(BTRIM(c.first_name), ''),
                                    NULLIF(BTRIM(c.last_name), '')
                                 )), ''),
                                 NULLIF(BTRIM(c.name), '')
                              )) LIKE :searchPattern)
                       AND EXISTS (
                          SELECT 1 FROM itibari.product_suppliers ps
                          INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                          WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                       )
                       GROUP BY c.public_id,
                                COALESCE(
                                   NULLIF(BTRIM(c.store_name), ''),
                                   NULLIF(BTRIM(CONCAT_WS(' ',
                                      NULLIF(BTRIM(c.first_name), ''),
                                      NULLIF(BTRIM(c.last_name), '')
                                   )), ''),
                                   NULLIF(BTRIM(c.name), '')
                                ),
                                r.public_id, r.name, ol.public_id, ol.name, c.landmark
                    ) grouped_customers
                    """,
            nativeQuery = true)
    Page<Object[]> pageCustomerAnalysisCustomersV2(
            @Param("supplierId") UUID supplierId,
            @Param("regionIds") List<UUID> regionIds,
            @Param("regionFilterDisabled") boolean regionFilterDisabled,
            @Param("locationIds") List<UUID> locationIds,
            @Param("locationFilterDisabled") boolean locationFilterDisabled,
            @Param("productIds") List<UUID> productIds,
            @Param("productFilterDisabled") boolean productFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs,
            @Param("searchPattern") String searchPattern,
            Pageable pageable);

    /**
     * Export-only CUSTOMERS view: one row per product in each delivered order.
     * No row LIMIT — returns every matching order/product for the filter window.
     * Column layout matches {@link #pageCustomerAnalysisCustomersV2} so {@code mapCustomerRow} can be reused.
     * UI table continues to use {@link #pageCustomerAnalysisCustomersV2} (one row per customer).
     */
    @Query(
            value = """
                    WITH base AS (
                       SELECT c.public_id,
                              COALESCE(
                                 NULLIF(BTRIM(c.store_name), ''),
                                 NULLIF(BTRIM(CONCAT_WS(' ',
                                    NULLIF(BTRIM(c.first_name), ''),
                                    NULLIF(BTRIM(c.last_name), '')
                                 )), ''),
                                 NULLIF(BTRIM(c.name), '')
                              ) AS shop_name,
                              r.public_id AS region_public_id,
                              r.name AS region_name,
                              ol.public_id AS location_public_id,
                              COALESCE(ol.name, 'UNKNOWN') AS location_name,
                              c.landmark,
                              so.public_id AS order_public_id,
                              pr.id AS product_id,
                              pr.product_name,
                              COALESCE(pr.weight, 0) AS product_weight,
                              soi.quantity_delivered,
                              so.creation_time,
                              COALESCE(pr.buying_price, 0) AS buying_price
                       FROM itibari.sales_order_items soi
                       INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                       INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                       INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                       LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                       INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                       WHERE soi.deleted IS NOT TRUE
                       AND so.loading_status = 'DELIVERED'
                       AND soi.quantity_delivered > 0
                       AND (COALESCE(:regionId, r.public_id) = r.public_id)
                       AND (:locationId IS NULL OR ol.public_id = :locationId)
                       AND (:productFilterDisabled = true OR pr.public_id IN (:productIds))
                       AND so.creation_time >= :fromTs
                       AND so.creation_time <= :toTs
                       AND (:searchPattern IS NULL OR LOWER(COALESCE(
                                 NULLIF(BTRIM(c.store_name), ''),
                                 NULLIF(BTRIM(CONCAT_WS(' ',
                                    NULLIF(BTRIM(c.first_name), ''),
                                    NULLIF(BTRIM(c.last_name), '')
                                 )), ''),
                                 NULLIF(BTRIM(c.name), '')
                              )) LIKE :searchPattern)
                       AND EXISTS (
                          SELECT 1 FROM itibari.product_suppliers ps
                          INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                          WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                       )
                    )
                    SELECT public_id,
                           shop_name,
                           region_public_id,
                           region_name,
                           location_public_id,
                           location_name,
                           landmark,
                           1 AS delivered_order_count,
                           COALESCE(SUM(buying_price * quantity_delivered), 0) AS total_cogs_tzs,
                           COALESCE(SUM(product_weight * quantity_delivered / 1000.0), 0) AS settled_tonnes,
                           CAST(MAX(creation_time) AS date) AS order_date,
                           product_name AS product_names,
                           CAST(SUM(quantity_delivered) AS text) AS product_purchase_quantities,
                           TO_CHAR(MAX(creation_time), 'DD/MM/YYYY') AS product_purchase_dates
                    FROM base
                    GROUP BY public_id, shop_name, region_public_id, region_name,
                             location_public_id, location_name, landmark, order_public_id,
                             product_id, product_name
                    ORDER BY order_date DESC, shop_name ASC, order_public_id ASC, product_name ASC, product_id ASC
                    """,
            nativeQuery = true)
    List<Object[]> listCustomerAnalysisCustomersForExportV2(
            @Param("supplierId") UUID supplierId,
            @Param("regionId") UUID regionId,
            @Param("locationId") UUID locationId,
            @Param("productIds") List<UUID> productIds,
            @Param("productFilterDisabled") boolean productFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs,
            @Param("searchPattern") String searchPattern);

    /**
     * Customer Analysis V2 activity calendar — delivered aggregates grouped by order {@code creation_time} calendar day.
     * Columns: day (DATE), distinct customer count, distinct delivered order count.
     */
    @Query(
            value = """
                    SELECT DATE(so.creation_time),
                           COUNT(DISTINCT c.public_id),
                           COUNT(DISTINCT so.public_id)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND (:regionFilterDisabled = true OR r.public_id IN (:regionIds))
                    AND (:locationFilterDisabled = true OR ol.public_id IN (:locationIds))
                    AND (:productFilterDisabled = true OR pr.public_id IN (:productIds))
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    GROUP BY DATE(so.creation_time)
                    ORDER BY DATE(so.creation_time)
                    """,
            nativeQuery = true)
    List<Object[]> aggregateCustomerAnalysisActivityByDay(
            @Param("supplierId") UUID supplierId,
            @Param("regionIds") List<UUID> regionIds,
            @Param("regionFilterDisabled") boolean regionFilterDisabled,
            @Param("locationIds") List<UUID> locationIds,
            @Param("locationFilterDisabled") boolean locationFilterDisabled,
            @Param("productIds") List<UUID> productIds,
            @Param("productFilterDisabled") boolean productFilterDisabled,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    /**
     * Customer Locations V2 — distinct customers with all-time supplier delivered scope (map markers source).
     * Columns: customer public_id, customer_name (COALESCE store_name, name), latitude, longitude,
     * customer_active_status, max_order_time, region public_id/name, organization_location public_id/name.
     * Ordered by customer name to preserve marker ordering; capped in SQL for portal responsiveness.
     */
    @Query(
            value = """
                    WITH supplier_products AS (
                        SELECT DISTINCT ps.product_id
                        FROM itibari.product_suppliers ps
                        INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                        WHERE sup.public_id = :supplierId
                    ),
                    sampled_order_lines AS (
                        SELECT so.sales_order_customer_fk AS customer_id,
                               so.creation_time AS order_time
                        FROM itibari.sales_order_items soi
                        INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                        INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                        INNER JOIN supplier_products sp ON sp.product_id = pr.id
                        INNER JOIN itibari.customers c_scope ON c_scope.id = so.sales_order_customer_fk AND c_scope.deleted IS NOT TRUE
                        LEFT JOIN itibari.organization_regions r_scope
                            ON :regionId IS NOT NULL
                            AND r_scope.id = c_scope.customer_organization_region_fk
                        LEFT JOIN itibari.organization_locations ol_scope
                            ON :locationId IS NOT NULL
                            AND ol_scope.id = c_scope.customer_organization_location_fk
                        WHERE soi.deleted IS NOT TRUE
                        AND so.loading_status = 'DELIVERED'
                        AND soi.quantity_delivered > 0
                        AND (:regionId IS NULL OR r_scope.public_id = :regionId)
                        AND (:locationId IS NULL OR ol_scope.public_id = :locationId)
                        AND (:productId IS NULL OR pr.public_id = :productId)
                        LIMIT (:limit * 10)
                    ),
                    scoped_customers AS (
                        SELECT customer_id,
                               MAX(order_time) AS max_order_time
                        FROM sampled_order_lines
                        GROUP BY customer_id
                    )
                    SELECT c.public_id,
                           COALESCE(c.store_name, c.name),
                           c.latitude,
                           c.longitude,
                           c.customer_active_status,
                           sc.max_order_time,
                           r.public_id,
                           r.name,
                           ol.public_id,
                           COALESCE(ol.name, 'UNKNOWN')
                    FROM scoped_customers sc
                    INNER JOIN itibari.customers c ON c.id = sc.customer_id AND c.deleted IS NOT TRUE
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                    ORDER BY COALESCE(c.store_name, c.name) ASC, c.public_id ASC
                    LIMIT :limit
                    """,
            nativeQuery = true)
    List<Object[]> listCustomerLocationMarkersV2(
            @Param("supplierId") UUID supplierId,
            @Param("regionId") UUID regionId,
            @Param("locationId") UUID locationId,
            @Param("productId") UUID productId,
            @Param("limit") int limit);

    /**
     * Customer Locations V2 — heat map points (all-time supplier delivered scope, no date window).
     * Columns: customer public_id, latitude, longitude, customer_active_status, max_order_time.
     * Ordered deterministically by latest supplier order desc, then customer public_id; capped in SQL.
     */
    @Query(
            value = """
                    WITH supplier_products AS (
                        SELECT DISTINCT ps.product_id
                        FROM itibari.product_suppliers ps
                        INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                        WHERE sup.public_id = :supplierId
                    ),
                    sampled_order_lines AS (
                        SELECT so.sales_order_customer_fk AS customer_id,
                               so.creation_time AS order_time
                        FROM itibari.sales_order_items soi
                        INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                        INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                        INNER JOIN supplier_products sp ON sp.product_id = pr.id
                        INNER JOIN itibari.customers c_scope ON c_scope.id = so.sales_order_customer_fk AND c_scope.deleted IS NOT TRUE
                        LEFT JOIN itibari.organization_regions r_scope
                            ON :regionId IS NOT NULL
                            AND r_scope.id = c_scope.customer_organization_region_fk
                        LEFT JOIN itibari.organization_locations ol_scope
                            ON :locationId IS NOT NULL
                            AND ol_scope.id = c_scope.customer_organization_location_fk
                        WHERE soi.deleted IS NOT TRUE
                        AND so.loading_status = 'DELIVERED'
                        AND soi.quantity_delivered > 0
                        AND (:regionId IS NULL OR r_scope.public_id = :regionId)
                        AND (:locationId IS NULL OR ol_scope.public_id = :locationId)
                        AND (:productId IS NULL OR pr.public_id = :productId)
                        LIMIT (:limit * 10)
                    ),
                    scoped_customers AS (
                        SELECT customer_id,
                               MAX(order_time) AS max_order_time
                        FROM sampled_order_lines
                        GROUP BY customer_id
                    )
                    SELECT c.public_id,
                           c.latitude,
                           c.longitude,
                           c.customer_active_status,
                           sc.max_order_time
                    FROM scoped_customers sc
                    INNER JOIN itibari.customers c ON c.id = sc.customer_id AND c.deleted IS NOT TRUE
                    ORDER BY sc.max_order_time DESC NULLS LAST, c.public_id ASC
                    LIMIT :limit
                    """,
            nativeQuery = true)
    List<Object[]> listCustomerLocationHeatMapPointsV2(
            @Param("supplierId") UUID supplierId,
            @Param("regionId") UUID regionId,
            @Param("locationId") UUID locationId,
            @Param("productId") UUID productId,
            @Param("limit") int limit);

    /**
     * Customer Locations V2 — base customer detail when in supplier delivered scope (no date window on scope check).
     */
    @Query(
            value = """
                    SELECT c.public_id,
                           COALESCE(c.store_name, c.name),
                           c.phone_number,
                           c.latitude,
                           c.longitude,
                           c.customer_active_status,
                           c.landmark,
                           c.address,
                           r.public_id,
                           r.name,
                           ol.public_id,
                           COALESCE(ol.name, 'UNKNOWN')
                    FROM itibari.customers c
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                    WHERE c.deleted IS NOT TRUE
                    AND c.public_id = :customerId
                    AND EXISTS (
                       SELECT 1
                       FROM itibari.sales_order_items soi
                       INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                       INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                       WHERE soi.deleted IS NOT TRUE
                       AND so.sales_order_customer_fk = c.id
                       AND so.loading_status = 'DELIVERED'
                       AND soi.quantity_delivered > 0
                       AND (COALESCE(:regionId, r.public_id) = r.public_id)
                       AND (:locationId IS NULL OR ol.public_id = :locationId)
                       AND (:productId IS NULL OR pr.public_id = :productId)
                       AND EXISTS (
                          SELECT 1 FROM itibari.product_suppliers ps
                          INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                          WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                       )
                    )
                    """,
            nativeQuery = true)
    List<Object[]> findCustomerLocationDetailBaseV2(
            @Param("supplierId") UUID supplierId,
            @Param("customerId") UUID customerId,
            @Param("regionId") UUID regionId,
            @Param("locationId") UUID locationId,
            @Param("productId") UUID productId);

    /**
     * Customer Locations V2 — date-scoped period metrics for one in-scope customer.
     */
    @Query(
            value = """
                    SELECT COUNT(DISTINCT so.public_id),
                           COALESCE(SUM(COALESCE(pr.buying_price, 0) * soi.quantity_delivered), 0),
                           COALESCE(SUM(COALESCE(pr.weight, 0) * soi.quantity_delivered / 1000.0), 0),
                           MAX(so.creation_time)
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c ON c.id = so.sales_order_customer_fk AND c.deleted IS NOT TRUE
                    INNER JOIN itibari.organization_regions r ON r.id = c.customer_organization_region_fk
                    LEFT JOIN itibari.organization_locations ol ON ol.id = c.customer_organization_location_fk
                    INNER JOIN itibari.products pr ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    WHERE soi.deleted IS NOT TRUE
                    AND c.public_id = :customerId
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND (COALESCE(:regionId, r.public_id) = r.public_id)
                    AND (:locationId IS NULL OR ol.public_id = :locationId)
                    AND (:productId IS NULL OR pr.public_id = :productId)
                    AND so.creation_time >= :fromTs
                    AND so.creation_time <= :toTs
                    AND EXISTS (
                       SELECT 1 FROM itibari.product_suppliers ps
                       INNER JOIN itibari.suppliers sup ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                       WHERE ps.product_id = pr.id AND sup.public_id = :supplierId
                    )
                    """,
            nativeQuery = true)
    List<Object[]> aggregateCustomerLocationPeriodMetricsV2(
            @Param("supplierId") UUID supplierId,
            @Param("customerId") UUID customerId,
            @Param("regionId") UUID regionId,
            @Param("locationId") UUID locationId,
            @Param("productId") UUID productId,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);

    default Page<Object[]> pageSupplierPortalProductDistributionV2(UUID supplierId, UUID regionId, UUID locationId,
                                                                   LocalDateTime fromTs, LocalDateTime toTs,
                                                                   Pageable pageable) {
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionId, locationId);
        return pageSupplierPortalProductDistributionV2(supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(), fromTs, toTs, pageable);
    }

    default List<Object[]> listSupplierPortalProductDistributionV2(UUID supplierId, UUID regionId, UUID locationId,
                                                                  LocalDateTime fromTs, LocalDateTime toTs) {
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionId, locationId);
        return listSupplierPortalProductDistributionV2(supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(), fromTs, toTs);
    }

    default List<Object[]> aggregateSupplierPortalSalesCalendarByDay(UUID supplierId, UUID regionId, UUID locationId,
                                                                     LocalDateTime fromTs, LocalDateTime toTs) {
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionId, locationId);
        return aggregateSupplierPortalSalesCalendarByDay(supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(), fromTs, toTs);
    }

    default List<Object[]> aggregateSupplierPortalDeliveredOrdersByCreationMonth(UUID supplierId, UUID regionId,
                                                                                UUID locationId, LocalDateTime fromTs,
                                                                                LocalDateTime toTs) {
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionId, locationId);
        SupplierPortalProductFilter products = SupplierPortalProductFilter.none();
        return aggregateSupplierPortalDeliveredOrdersByCreationMonth(supplierId, geo.regionIdsParam(),
                geo.regionFilterDisabledParam(), geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                products.productIdsParam(), products.productFilterDisabledParam(), fromTs, toTs);
    }

    default Long countNewDeliveredCustomersForCustomerAnalysis(UUID supplierId, UUID regionId, UUID locationId,
                                                              LocalDateTime fromTs, LocalDateTime toTs) {
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionId, locationId);
        SupplierPortalProductFilter products = SupplierPortalProductFilter.none();
        return countNewDeliveredCustomersForCustomerAnalysis(supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                products.productIdsParam(), products.productFilterDisabledParam(), fromTs, toTs);
    }

    default Long countFrequentDeliveredCustomersForCustomerAnalysis(UUID supplierId, UUID regionId, UUID locationId,
                                                                   LocalDateTime fromTs, LocalDateTime toTs) {
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionId, locationId);
        SupplierPortalProductFilter products = SupplierPortalProductFilter.none();
        return countFrequentDeliveredCustomersForCustomerAnalysis(supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                products.productIdsParam(), products.productFilterDisabledParam(), fromTs, toTs);
    }

    default List<Object[]> listCustomerAnalysisDistrictRankingV2(UUID supplierId, UUID regionId, UUID locationId,
                                                                 LocalDateTime fromTs, LocalDateTime toTs) {
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionId, locationId);
        SupplierPortalProductFilter products = SupplierPortalProductFilter.none();
        return listCustomerAnalysisDistrictRankingV2(supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                products.productIdsParam(), products.productFilterDisabledParam(), fromTs, toTs);
    }

    default Page<Object[]> pageCustomerAnalysisCustomersV2(UUID supplierId, UUID regionId, UUID locationId,
                                                           LocalDateTime fromTs, LocalDateTime toTs,
                                                           String searchPattern, Pageable pageable) {
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionId, locationId);
        SupplierPortalProductFilter products = SupplierPortalProductFilter.none();
        return pageCustomerAnalysisCustomersV2(supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                products.productIdsParam(), products.productFilterDisabledParam(), fromTs, toTs, searchPattern, pageable);
    }

    default List<Object[]> aggregateCustomerAnalysisActivityByDay(UUID supplierId, UUID regionId, UUID locationId,
                                                                 LocalDateTime fromTs, LocalDateTime toTs) {
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionId, locationId);
        SupplierPortalProductFilter products = SupplierPortalProductFilter.none();
        return aggregateCustomerAnalysisActivityByDay(supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                products.productIdsParam(), products.productFilterDisabledParam(), fromTs, toTs);
    }

    @Query("SELECT s.salesOrderNumber FROM SalesOrders s WHERE s.deleted = false AND s.deliveryAgent = ?1 and s.salesOrderStatus = 'DELIVERED'")
    List<String> findByDeliveryAgent(DeliveryAgents deliveryAgent);

    Page<SalesOrders> findAllBySalesAgentPublicIdAndDeletedIsFalse(String salesAgentPublicId, Pageable pageable);

    List<SalesOrders> findAllByRoutePublicIdAndDeletedIsFalse(String string);
}
