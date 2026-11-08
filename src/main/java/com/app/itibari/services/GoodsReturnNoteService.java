package com.app.itibari.services;

import com.app.itibari.dataTypes.GoodsNoteType;
import com.app.itibari.dataTypes.GoodsReturnStatus;
import com.app.itibari.dataTypes.PromoType;
import com.app.itibari.dataTypes.PurchaseOrderStatus;
import com.app.itibari.models.entities.GoodsReceivedNote;
import com.app.itibari.models.entities.GoodsReturnDeliveryNote;
import com.app.itibari.models.entities.GoodsReturnNote;
import com.app.itibari.models.entities.ProductItem;
import com.app.itibari.models.entities.ReceivedProductItem;
import com.app.itibari.models.entities.ReturnedProductItem;
import com.app.itibari.models.request.GoodsReturnNoteRequest;
import com.app.itibari.models.request.PromoRequestNoteRequest;
import com.app.itibari.models.response.ReceivedProductItemResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface GoodsReturnNoteService {

    void createGoodsReturnNote(@Valid GoodsReturnNoteRequest goodsReturnNoteRequest, String name);

    Page<GoodsReturnNote> getGoodsReturnNote(GoodsReturnStatus status,
                                             String search,
                                             String purchaseOrderNumber,
                                             UUID supplierPublicId,
                                             LocalDate to,
                                             LocalDate from,
                                             GoodsNoteType goodsNoteType,
                                             Pageable pageable);

    Page<ProductItem> getGoodsReceiveNoteItems(String search, UUID goodsReturnNotePublicId, GoodsNoteType goodsNoteType, Pageable pageable);

    Page<ProductItem> getGoodsReturnNoteItems(String search, UUID goodsReturnNotePublicId, GoodsNoteType goodsNoteType, Pageable pageable);

    void receivePromoGoods(PromoType promoType, @Valid PromoRequestNoteRequest promoNoteRequest, String name);

    Page<GoodsReceivedNote> getShipmentReceiveNote(GoodsReturnStatus status,
                                                   String search,
                                                   String purchaseOrderNumber,
                                                   UUID supplierPublicId,
                                                   LocalDate to,
                                                   LocalDate from,
                                                   GoodsNoteType goodsNoteType,
                                                   PurchaseOrderStatus poStatus,
                                                   Pageable pageable);

    ReceivedProductItemResponse getShipmentNoteItems(String search, UUID goodsReceivedNotePublicId, Pageable pageable);

    void createGoodsReturnDeliveryNote(@Valid GoodsReturnNoteRequest goodsReturnNoteRequest, String name);

    Page<GoodsReturnDeliveryNote> listReturnedGoodsNotes(GoodsReturnStatus status,
                                                         String search,
                                                         String purchaseOrderNumber,
                                                         UUID supplierPublicId,
                                                         LocalDate to,
                                                         LocalDate from,
                                                         GoodsNoteType goodsNoteType,
                                                         Pageable pageable);

    Page<ReturnedProductItem> getReturnedProductItems(String search, UUID goodsReturnedNotePublicId, Pageable pageable);
}
