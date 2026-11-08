package com.app.itibari.repository;

import com.app.itibari.models.entities.GoodsReceivedNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoodsReceivedNoteRepository extends JpaRepository<GoodsReceivedNote, Long>, JpaSpecificationExecutor<GoodsReceivedNote> {

    Optional<GoodsReceivedNote> findByPublicId(UUID goodsReceivedNotePublicId);

    Optional<GoodsReceivedNote> findByPublicIdAndDeletedIsFalse(UUID returnNotePublicId);

    List<GoodsReceivedNote> findByDeletedIsFalseAndCreationTimeIsNullAndNoteDateIsNotNull();

    Optional<GoodsReceivedNote> findByNoteNumber(String goodsReturnNoteNumber);
    Optional<GoodsReceivedNote> findByPurchaseOrderNumber(String purchaseNumber);
}
