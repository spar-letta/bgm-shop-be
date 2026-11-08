package com.app.itibari.repository;

import com.app.itibari.models.entities.GoodsReceivedNote;
import com.app.itibari.models.entities.ReceivedProductItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReceivedProductItemRepository extends JpaRepository<ReceivedProductItem, Long> {

    Optional<ReceivedProductItem> findByPublicIdAndDeletedIsFalse(UUID itemPublicId);

    Page<ReceivedProductItem> findByGoodsReceivedNoteAndDeletedIsFalse(GoodsReceivedNote goodsReceivedNote, Pageable pageable);
    List<ReceivedProductItem> findByGoodsReceivedNoteAndDeletedIsFalse(GoodsReceivedNote goodsReceivedNote);

    Optional<ReceivedProductItem> findByProductPublicIdAndDeletedIsFalse(UUID publicId);
}
