package com.app.itibari.repository;

import com.app.itibari.models.entities.LiveStockEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LiveStockEntityRepository extends JpaRepository<LiveStockEntity, Long> {

    Optional<LiveStockEntity> findLiveStockEntityByInventoryItemPublicIdAndLiveDateAndDeletedIsFalse(String publicId, LocalDate date);

    Optional<LiveStockEntity> findByInventoryItemPublicIdAndInventoryPublicIdAndWarehousePublicIdAndLiveDateAndDeletedFalse( String inventoryItemPublicId, String inventoryPublicId, String warehousePublicId, LocalDate liveDate );

    Page<LiveStockEntity> findAllByWarehousePublicIdAndLiveDateAndDeletedIsFalse(String string, LocalDate localDate,
                                                                                 Pageable pageable);

    Page<LiveStockEntity> findAllByWarehousePublicIdAndLiveDateAndDeletedIsFalseAndProductNameIgnoreCaseOrProductCodeIgnoreCase(
            String warehousePublicId,
            LocalDate liveDate,
            String productName,
            String productCode,
            Pageable pageable);

    @Query("SELECT lse FROM LiveStockEntity lse WHERE lse.inventoryItemPublicId = ?1 AND lse.liveDate = ?2 AND lse.deleted = false")
    List<LiveStockEntity> findLiveStockEntityByInventoryItemPublicIdAndLiveDateAndDeletedIsFalseA(String publicId, LocalDate date);

    @Query("SELECT lse FROM LiveStockEntity lse WHERE lse.inventoryItemPublicId = ?1 AND lse.inventoryPublicId = ?2 AND lse.warehousePublicId = ?3 AND lse.liveDate = ?4 AND lse.deleted = false ORDER BY lse.id DESC")
    List<LiveStockEntity> findLatestLiveStockRows(String inventoryItemPublicId, String inventoryPublicId, String warehousePublicId, LocalDate liveDate);

    @Query("SELECT lse FROM LiveStockEntity lse WHERE lse.inventoryItemPublicId = ?1 AND lse.liveDate = ?2 AND lse.deleted = false ORDER BY lse.id DESC")
    List<LiveStockEntity> findLatestLiveStockRowsByInventoryItemAndDate(String inventoryItemPublicId, LocalDate liveDate);

    boolean existsByInventoryItemPublicIdAndInventoryPublicIdAndWarehousePublicIdAndLiveDateAndDeletedFalse( String inventoryItemPublicId, String inventoryPublicId, String warehousePublicId, LocalDate liveDate );
}
