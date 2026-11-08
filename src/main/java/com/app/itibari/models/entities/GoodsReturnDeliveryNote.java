package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.GoodsReturnStatus;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "goods_return_delivery_notes", schema = "itibari")
public class GoodsReturnDeliveryNote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.GoodsReturned.class, BaseView.GoodsReceived.class})
    @Column(name = "goods_return_note_number")
    private String goodsReturnNoteNumber;

    @JsonView({BaseView.GoodsReturned.class, BaseView.GoodsReceived.class})
    @Column(name = "goods_return_note_date")
    private LocalDate goodsReturnNoteDate;

    @JsonView({BaseView.GoodsReturned.class, BaseView.GoodsReceived.class})
    @Column(name = "goods_return_note_status")
    @Enumerated(EnumType.STRING)
    private GoodsReturnStatus status;

    @OneToMany(mappedBy = "goodsReturnDeliveryNote")
    private List<ReturnedProductItem> returnedProductItems = new ArrayList<>();

    @JsonView({BaseView.GoodsReturned.class, BaseView.GoodsReceived.class})
    @Column(name = "purchase_order_number")
    private String purchaseOrderNumber;

    @JsonView({BaseView.GoodsReturned.class, BaseView.GoodsReceived.class})
    @ManyToOne
    @JoinColumn(name = "supplier_id_fk")
    private Suppliers supplier;

    @JsonView({BaseView.GoodsReturned.class})
    @Column(name = "replacement_raised")
    private boolean replacementRaised = false;

    @JsonView({BaseView.GoodsReturned.class, BaseView.GoodsReceived.class})
    @ManyToOne
    @JoinColumn(name = "goods_received_note_fk")
    private GoodsReceivedNote goodsReceivedNote;

    @JsonView({BaseView.GoodsReturned.class})
    @Column(name = "replacement_purchase_order")
    private String replacementPurchaseOrder;

    @Transient
    @JsonView({BaseView.GoodsReturned.class, BaseView.GoodsReceived.class})
    private UUID goodsReturnDeliveryNoteId;

    public UUID getGoodsReturnDeliveryNoteId() {
        if (goodsReceivedNote != null){
            return goodsReceivedNote.getPublicId();
        }
        return null;
    }
}
