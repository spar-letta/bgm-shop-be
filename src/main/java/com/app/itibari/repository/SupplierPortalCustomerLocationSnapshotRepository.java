package com.app.itibari.repository;

import com.app.itibari.models.entities.SupplierPortalCustomerLocationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Pre-computed supplier portal customer location rows for map markers and heat map reads.
 * Populated nightly by {@link com.app.itibari.services.SupplierPortalCustomerLocationsSnapshotService}.
 */
@Repository
public interface SupplierPortalCustomerLocationSnapshotRepository
        extends JpaRepository<SupplierPortalCustomerLocationSnapshot, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM itibari.supplier_portal_customer_location_snapshot", nativeQuery = true)
    void deleteAllSnapshotRows();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    INSERT INTO itibari.supplier_portal_customer_location_snapshot (
                        snapshot_as_of,
                        supplier_public_id,
                        customer_public_id,
                        product_public_id,
                        customer_name,
                        latitude,
                        longitude,
                        customer_active_status,
                        max_order_time,
                        region_public_id,
                        region_name,
                        location_public_id,
                        location_name
                    )
                    SELECT :snapshotAsOf,
                           sup.public_id,
                           c.public_id,
                           pr.public_id,
                           COALESCE(c.store_name, c.name),
                           c.latitude,
                           c.longitude,
                           c.customer_active_status,
                           MAX(so.creation_time),
                           r.public_id,
                           r.name,
                           ol.public_id,
                           COALESCE(ol.name, 'UNKNOWN')
                    FROM itibari.sales_order_items soi
                    INNER JOIN itibari.sales_orders so
                        ON so.id = soi.sales_order_id AND so.deleted IS NOT TRUE
                    INNER JOIN itibari.products pr
                        ON pr.id = soi.sales_order_items_product_fk AND pr.deleted IS NOT TRUE
                    INNER JOIN itibari.product_suppliers ps ON ps.product_id = pr.id
                    INNER JOIN itibari.suppliers sup
                        ON sup.id = ps.supplier_id AND sup.deleted IS NOT TRUE
                    INNER JOIN itibari.customers c
                        ON c.id = so.sales_order_customer_fk AND c.deleted IS NOT TRUE
                    INNER JOIN itibari.organization_regions r
                        ON r.id = c.customer_organization_region_fk
                    LEFT JOIN itibari.organization_locations ol
                        ON ol.id = c.customer_organization_location_fk
                    WHERE soi.deleted IS NOT TRUE
                    AND so.loading_status = 'DELIVERED'
                    AND soi.quantity_delivered > 0
                    AND so.creation_time < :cutoff
                    GROUP BY sup.public_id,
                             c.public_id,
                             pr.public_id,
                             c.store_name,
                             c.name,
                             c.latitude,
                             c.longitude,
                             c.customer_active_status,
                             r.public_id,
                             r.name,
                             ol.public_id,
                             ol.name
                    """,
            nativeQuery = true)
    int insertSnapshotRows(@Param("snapshotAsOf") LocalDate snapshotAsOf,
                           @Param("cutoff") LocalDateTime cutoff);

    @Query(value = "SELECT COUNT(*) FROM itibari.supplier_portal_customer_location_snapshot", nativeQuery = true)
    long countSnapshotRows();

    /**
     * Map markers source — one row per customer after optional product filter dedupe.
     */
    @Query(
            value = """
                    WITH filtered AS (
                        SELECT s.customer_public_id,
                               s.customer_name,
                               s.latitude,
                               s.longitude,
                               s.customer_active_status,
                               s.max_order_time,
                               s.region_public_id,
                               s.region_name,
                               s.location_public_id,
                               s.location_name,
                               s.product_public_id
                        FROM itibari.supplier_portal_customer_location_snapshot s
                        WHERE s.supplier_public_id = :supplierId
                        AND (:regionId IS NULL OR s.region_public_id = :regionId)
                        AND (:locationId IS NULL OR s.location_public_id = :locationId)
                        AND (:productId IS NULL OR s.product_public_id = :productId)
                    ),
                    deduped AS (
                        SELECT DISTINCT ON (f.customer_public_id)
                               f.customer_public_id,
                               f.customer_name,
                               f.latitude,
                               f.longitude,
                               f.customer_active_status,
                               f.max_order_time,
                               f.region_public_id,
                               f.region_name,
                               f.location_public_id,
                               f.location_name
                        FROM filtered f
                        ORDER BY f.customer_public_id,
                                 f.max_order_time DESC NULLS LAST,
                                 f.product_public_id ASC
                    )
                    SELECT d.customer_public_id,
                           d.customer_name,
                           d.latitude,
                           d.longitude,
                           d.customer_active_status,
                           d.max_order_time,
                           d.region_public_id,
                           d.region_name,
                           d.location_public_id,
                           d.location_name
                    FROM deduped d
                    ORDER BY d.customer_name ASC NULLS LAST, d.customer_public_id ASC
                    LIMIT :limit
                    """,
            nativeQuery = true)
    List<Object[]> listCustomerLocationMarkersFromSnapshot(
            @Param("supplierId") UUID supplierId,
            @Param("regionId") UUID regionId,
            @Param("locationId") UUID locationId,
            @Param("productId") UUID productId,
            @Param("limit") int limit);

    /**
     * Heat map points source — one row per customer after optional product filter dedupe.
     */
    @Query(
            value = """
                    WITH filtered AS (
                        SELECT s.customer_public_id,
                               s.latitude,
                               s.longitude,
                               s.customer_active_status,
                               s.max_order_time,
                               s.product_public_id
                        FROM itibari.supplier_portal_customer_location_snapshot s
                        WHERE s.supplier_public_id = :supplierId
                        AND (:regionId IS NULL OR s.region_public_id = :regionId)
                        AND (:locationId IS NULL OR s.location_public_id = :locationId)
                        AND (:productId IS NULL OR s.product_public_id = :productId)
                    ),
                    deduped AS (
                        SELECT DISTINCT ON (f.customer_public_id)
                               f.customer_public_id,
                               f.latitude,
                               f.longitude,
                               f.customer_active_status,
                               f.max_order_time
                        FROM filtered f
                        ORDER BY f.customer_public_id,
                                 f.max_order_time DESC NULLS LAST,
                                 f.product_public_id ASC
                    )
                    SELECT d.customer_public_id,
                           d.latitude,
                           d.longitude,
                           d.customer_active_status,
                           d.max_order_time
                    FROM deduped d
                    ORDER BY d.max_order_time DESC NULLS LAST, d.customer_public_id ASC
                    LIMIT :limit
                    """,
            nativeQuery = true)
    List<Object[]> listCustomerLocationHeatMapPointsFromSnapshot(
            @Param("supplierId") UUID supplierId,
            @Param("regionId") UUID regionId,
            @Param("locationId") UUID locationId,
            @Param("productId") UUID productId,
            @Param("limit") int limit);
}
