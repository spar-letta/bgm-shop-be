package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.StockMovementTransactionType;
import com.app.itibari.dataTypes.TransactionBoundType;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "stock_movement_item", schema = "itibari")
public class StockMovementItem extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.StockMovementView.class)
    @Column(name = "product_name")
    private String productName;

    @JsonView(BaseView.StockMovementView.class)
    @Column(name = "product_sku")
    private String productSku;

    @JsonView(BaseView.StockMovementView.class)
    @Column(name = "quantity")
    private Integer quantity;

    @JsonView(BaseView.StockMovementView.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private StockMovementTransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "bound_type")
    private TransactionBoundType boundType;

    @JsonView(BaseView.StockMovementView.class)
    @Column(name = "done_by")
    private String doneBy;

    @JsonView(BaseView.StockMovementView.class)
    @Column(name = "warehouse")
    private String warehouse;

    @Column(name = "warehouse_public_id")
    private String warehousePublicId;

    @Column(name = "previous_closing_stock")
    private int previousClosingStock;

    @JsonView(BaseView.StockMovementView.class)
    @Column(name = "order_public_id")
    private String orderPublicId;

    @JsonView(BaseView.StockMovementView.class)
    @Column(name = "route_number")
    private String routeNumber;

    @JsonView(BaseView.StockMovementView.class)
    @Transient
    private String transactionQuantity;


    public String getTransactionQuantity() {
        if (this.boundType == TransactionBoundType.INCOMING) {
            return "+ " + this.quantity;
        } else if (this.boundType == TransactionBoundType.OUTGOING) {
            return "- " + this.quantity;
        } else if(this.boundType == TransactionBoundType.RECORD) {
            if(this.transactionType == StockMovementTransactionType.CLOSING_STOCK_UPDATE){
                return String.format("%d (previous: %d)", this.quantity, this.previousClosingStock);
            } else {
                return String.format("%d (recorded only)", this.quantity);
            }
        } else {
            return String.valueOf(this.quantity);
        }
    }
}
