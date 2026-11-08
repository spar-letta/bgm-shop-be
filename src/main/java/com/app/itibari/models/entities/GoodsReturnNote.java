package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.GoodsNoteType;
import com.app.itibari.dataTypes.GoodsReturnStatus;
import com.app.itibari.dataTypes.PurchaseOrderStatus;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "goods_return_note", schema = "itibari")
public class GoodsReturnNote extends BaseEntity {

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

    @Deprecated
    @Column(name = "return_reason")
    private String returnReason;

    @Deprecated
    @ManyToOne
    @JoinColumn(name = "product_id_fk")
    private Product product;

    @OneToMany
    @JoinTable(
            name = "goods_return_note_products", schema = "itibari",
            joinColumns = @JoinColumn(name = "goods_return_note_id_fk", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "product_item_id_fk", referencedColumnName = "id"
            ))
    private List<ProductItem> productItems = new ArrayList<>();

    @Deprecated
    //@JsonView({BaseView.GoodsReturned.class, BaseView.GoodsReceived.class})
    @Column(name = "quantity")
    private Long quantity;

    @JsonView({BaseView.GoodsReturned.class, BaseView.GoodsReceived.class})
    @Column(name = "purchase_order_number")
    private String purchaseOrderNumber;

    @JsonView({BaseView.GoodsReturned.class, BaseView.GoodsReceived.class})
    @ManyToOne
    @JoinColumn(name = "supplier_id_fk")
    private Suppliers supplier;

    @Column(name = "goods_note_type")
    @Enumerated(EnumType.STRING)
    private GoodsNoteType goodsNoteType;

    // quantity requested
    @Deprecated
    // @JsonView({BaseView.GoodsReturned.class, BaseView.GoodsReceived.class})
    @Column(name = "quantity_requested")
    private Long quantityRequested;

    @JsonView({BaseView.GoodsReturned.class})
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "rejections_code_id_fk")
    private RejectionsCode rejectionsCode;

    @JsonView({BaseView.GoodsReturned.class})
    @Column(name = "goods_receive_note_id_fk")
    private String goodsReceiveNoteId;

    @JsonView({BaseView.GoodsReceived.class, BaseView.GoodsReturned.class})
    @Column(name = "delivery_note_number")
    private String deliveryNoteNumber;

    @JsonView({BaseView.GoodsReceived.class, BaseView.GoodsReturned.class})
    @Column(name = "purchase_order_status")
    @Enumerated(EnumType.STRING)
    private PurchaseOrderStatus purchaseOrderStatus;

    @JsonView({BaseView.GoodsReturned.class})
    @Column(name = "replacement_raised")
    private boolean replacementRaised = false;

    @JsonView({BaseView.GoodsReturned.class})
    @Column(name = "replacement_purchase_order")
    private String replacementPurchaseOrder;

    @JsonView({BaseView.GoodsReturned.class})
    @Column(name = "goods_receive_note_number")
    private String goodsReceiveNoteNumber;

    public void addProduct(ProductItem product) {

        Optional<ProductItem> optionalProduct = productItems
                .stream()
                .filter(p -> p.getPublicId().equals(product.getPublicId()))
                .findFirst();

        if (optionalProduct.isEmpty()) {
            productItems.add(product);
        }
    }
}
