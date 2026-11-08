package com.app.itibari.repository;

import com.app.itibari.models.entities.GoodsReturnDeliveryNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoodsReturnDeliveryNoteRepository
        extends JpaRepository<GoodsReturnDeliveryNote, Long>, JpaSpecificationExecutor<GoodsReturnDeliveryNote> {

    Optional<GoodsReturnDeliveryNote> findByPublicIdAndDeletedIsFalse(UUID goodsReturnNotePublicId);

    List<GoodsReturnDeliveryNote> findAllByReplacementRaisedIsTrueAndDeletedIsFalse();

    List<GoodsReturnDeliveryNote> findAllByDeletedIsFalse();

    Optional<GoodsReturnDeliveryNote> findByGoodsReturnNoteNumberAndDeletedIsFalse(String goodsReturnNoteNumberReference);
}
