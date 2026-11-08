package com.app.itibari.repository;

import com.app.itibari.models.entities.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductStockRepository extends JpaRepository<ProductStock, Long>, JpaSpecificationExecutor<ProductStock> {
    Optional<ProductStock> findByProductPublicIdAndInventoryPublicIdAndRecordTodayAndWarehouseAndDeletedFalse(UUID publicId, UUID publicId1, LocalDate now, Warehouse warehouse);
//
    @Query("""
                SELECT w.warehouseName AS warehouseName,
                       w.publicId AS warehousePublicId,
                       COUNT(ps.id) AS productCount,
                       SUM(ps.availableStock + ps.reservedStock + ps.quarantinedStock) AS totalStock,
                       SUM((ps.availableStock + ps.reservedStock + ps.quarantinedStock) * p.buyingPrice) AS totalValue,
                       CASE 
                           WHEN SUM(ps.availableStock + ps.reservedStock + ps.quarantinedStock) = 0 THEN 0
                           ELSE ROUND(SUM((ps.availableStock + ps.reservedStock + ps.quarantinedStock) * p.buyingPrice) / SUM(ps.availableStock + ps.reservedStock + ps.quarantinedStock), 2)
                       END AS averageCost,
                       MAX(ps.creationTime) AS latestCreationTime
                FROM ProductStock ps
                JOIN ps.warehouse w
                JOIN ps.product p
                WHERE (:warehousePublicId IS NULL OR w.publicId = :warehousePublicId)    
                AND ps.deleted = false
                AND ps.recordToday = :nairobiDate
                GROUP BY w.warehouseName, w.publicId
                ORDER BY latestCreationTime DESC
            """)
    Page<ProductStockRapperResponse> getProductStockRapper(Pageable pageable,
                                                           @Param("warehousePublicId") UUID warehousePublicId,
                                                           @Param("nairobiDate") LocalDate nairobiDate);

    ProductStock findTopByProductPublicIdAndRecordTodayOrderByRecordTodayDesc(UUID productPublicId,
                                                                              LocalDate recordToday);

    List<ProductStock> findByWarehouseAndRecordToday(Warehouse warehouse, LocalDate recordToday);

    List<ProductStock> findByRecordToday(LocalDate nairobiDate);

    List<ProductStock> findAllByInventoryPublicIdAndRecordTodayAndDeletedFalse(UUID publicId, LocalDate nairobiDate);

    List<ProductStock> findByDeletedFalseAndProductPublicIdAndInventoryPublicIdAndRecordToday(UUID publicId,
                                                                                           UUID publicId1, LocalDate now);

    List<ProductStock> findAllByRecordTodayAndDeletedFalse(LocalDate localDate);

    Optional<ProductStock> findProductStockByDeletedFalseAndProductPublicIdAndInventoryPublicIdAndRecordToday(UUID publicId,
                                                                                              UUID publicId1, LocalDate now);

    Optional<ProductStock> findByDeletedFalseAndProductPublicIdAndRecordToday(UUID publicId, LocalDate nairobiDate);

    List<ProductStock> findAllByRecordTodayAndDeletedFalseAndProductPublicIdAndInventoryPublicId(LocalDate localDate, UUID productPublicId, UUID inventoryPublicId);

    Optional<ProductStock> findByProductAndRecordTodayAndDeletedFalse(Product product, LocalDate previousDay);
}