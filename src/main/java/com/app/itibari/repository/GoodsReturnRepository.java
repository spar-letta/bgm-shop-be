package com.app.itibari.repository;

import com.app.itibari.dataTypes.GoodsNoteType;
import com.app.itibari.models.entities.GoodsReturnNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoodsReturnRepository extends JpaRepository<GoodsReturnNote, Long>, JpaSpecificationExecutor<GoodsReturnNote> {

    Optional<GoodsReturnNote> findByPublicIdAndDeletedIsFalse(UUID goodsReturnNotePublicId);

    List<GoodsReturnNote> findByPurchaseOrderNumberIgnoreCaseAndGoodsNoteType(String order, GoodsNoteType goodsNoteType);

    List<GoodsReturnNote> findByGoodsNoteTypeAndDeletedFalse(GoodsNoteType  goodsNoteType);

    List<GoodsReturnNote> findByGoodsNoteTypeAndDeletedFalseAndCreationTimeBetween(GoodsNoteType goodsNoteType, LocalDateTime startDate, LocalDateTime endDate);
}
