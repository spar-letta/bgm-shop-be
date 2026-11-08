package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.GoodsReturnStatus;
import com.app.itibari.dataTypes.POType;
import com.app.itibari.dataTypes.PurchaseOrderStatus;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * looks okay!
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "goods_received_notes", schema = "itibari")
@Where(clause = "deleted = false")
public class GoodsReceivedNote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.GoodsReturned.class, BaseView.GoodsReceived.class})
    @Column(name = "note_date")
    private LocalDate noteDate;

    @JsonView({BaseView.GoodsReturned.class, BaseView.GoodsReceived.class})
    @Column(name = "note_number")
    private String noteNumber;

    @JsonView({BaseView.GoodsReturned.class, BaseView.GoodsReceived.class})
    @ManyToOne
    @JoinColumn(name = "supplier_id_fk")
    private Suppliers supplier;

    @JsonView({BaseView.GoodsReturned.class, BaseView.GoodsReceived.class})
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private GoodsReturnStatus status;

    @JsonView({BaseView.GoodsReturned.class, BaseView.GoodsReceived.class})
    @Column(name = "purchase_order_number")
    private String purchaseOrderNumber;

    @JsonView({BaseView.GoodsReceived.class, BaseView.GoodsReturned.class})
    @Column(name = "delivery_note_number")
    private String deliveryNoteNumber;

    @JsonView({BaseView.GoodsReceived.class, BaseView.GoodsReturned.class})
    @Column(name = "purchase_order_status")
    @Enumerated(EnumType.STRING)
    private PurchaseOrderStatus purchaseOrderStatus;

    @JsonView({BaseView.GoodsReceived.class, BaseView.GoodsReturned.class})
    @Column(name = "po_type")
    @Enumerated(EnumType.STRING)
    private POType poType;

    @OneToMany(mappedBy = "goodsReceivedNote")
    private List<ReceivedProductItem> receivedProductItem = new ArrayList<>();

    public void addProduct(ReceivedProductItem product) {

        Optional<ReceivedProductItem> optionalProduct = receivedProductItem
                .stream()
                .filter(p -> p.getPublicId().equals(product.getPublicId()))
                .findFirst();

        if (optionalProduct.isEmpty()) {
            receivedProductItem.add(product);
        }
    }
}
