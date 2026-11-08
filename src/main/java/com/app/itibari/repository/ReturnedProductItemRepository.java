package com.app.itibari.repository;

import com.app.itibari.models.entities.GoodsReturnDeliveryNote;
import com.app.itibari.models.entities.ReturnedProductItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnedProductItemRepository extends JpaRepository<ReturnedProductItem, Long> {

    List<ReturnedProductItem> findByGoodsReturnDeliveryNote(GoodsReturnDeliveryNote goodsReturnNote);

    Page<ReturnedProductItem> findByGoodsReturnDeliveryNoteAndDeletedIsFalse(GoodsReturnDeliveryNote goodsReturnDeliveryNote, Pageable pageable);
}
