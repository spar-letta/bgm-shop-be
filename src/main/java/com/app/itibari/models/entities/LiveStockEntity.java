package com.app.itibari.models.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "live_stock_value", schema = "itibari")
public class LiveStockEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "live_date")
    @JsonFormat(pattern = "dd-MM-yyyy", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate liveDate;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "product_code")
    private String productCode;

    @Column(name = "inventory_item_public_id")
    private String inventoryItemPublicId;

    @Column(name = "inventory_public_id")
    private String inventoryPublicId;

    @Column(name = "warehouse_public_id")
    private String warehousePublicId;

    @Column(name = "warehouse_name")
    private String warehouseName;

    @Column(name = "opening_stock")
    private Integer openingStock;

    @Column(name = "closing_stock")
    private Integer closingStock;

    @Column(name = "issued_stock")
    private Integer issuedStock;

    @Column(name = "goods_received_stock")
    private Integer goodsReceivedStock;

    @Column(name = "goods_returned_stock")
    private Integer goodsReturnedStock;

    @Column(name = "replaced_stock")
    private Integer replacedStock;

    @Column(name = "returned_from_trade_stock")
    private Integer returnedFromTradeStock;

    @Column(name = "sold_stock")
    private Integer soldStock = 0;

    @Column(name = "returned_from_quarantine_stock")
    private Integer returnedFromQuarantineStock;

    @Column(name = "quarantine_stock")
    private Integer quarantineStock;

    @Column(name = "adjusted_stock")
    private Integer adjustedStock;

    @Column(name = "negative_adjusted_tock")
    private Integer negativeAdjustedStock;

    @Transient
    public Integer getExpectedAfterIssuedStock() {
        return safeValue(openingStock) - safeValue(issuedStock);
    }

    @Column(name = "bonus_stock")
    private Integer bonusStock;

    @Column(name = "rebate_stock")
    private Integer rebateStock;

    @Column(name = "incoming_stock")
    private Integer incomingStock;     // trigger with dispatch = increase, receive decrease

    @Column(name = "customer_returned_stock")
    private Integer customerReturnedStock;

    @Transient
    private Integer closingStockTwo;

    @Transient
    public Integer getExpectedClosingStock() {
        return (safeValue(getExpectedAfterIssuedStock())
                + safeValue(goodsReceivedStock)
                + safeValue(replacedStock)
                + safeValue(returnedFromTradeStock)
                + safeValue(adjustedStock))
                + safeValue(bonusStock)
                + safeValue(rebateStock)
                + safeValue(customerReturnedStock)
                - (safeValue(negativeAdjustedStock) + safeValue(goodsReturnedStock));

        // Opening - Issued - GRTN + (stock adjustment + Goods received note + Quarantine stock + Replacement +
        // NEGATIVE aJST)
    }

    @Transient
    public Integer getClosingStockTwo() {
        return safeValue(issuedStock)
                + safeValue(goodsReturnedStock)
                + safeValue(returnedFromTradeStock)
                + safeValue(returnedFromQuarantineStock);
    }

    @Transient
    public Integer getTotalStock() {
        return (
                safeValue(goodsReceivedStock) +   // (COALESCE(ps.goods_received_stock,0) +
                        //(COALESCE(ps.replaced_stock,0) +
                        //(COALESCE(ps.returned_from_trade_stock,0) +

                        // COALESCE(ps.reserved_stock,0)) AS ps_total,
                        safeValue(replacedStock) +
                        safeValue(returnedFromTradeStock) +
                        safeValue(getExpectedAfterIssuedStock()) +
                        safeValue(adjustedStock) +
                        safeValue(bonusStock) +
                        safeValue(rebateStock)) -
                (safeValue(goodsReturnedStock) + safeValue(negativeAdjustedStock));
    }

    private int safeValue(Integer value) {
        return value != null ? value : 0;
    }

    public int getOpeningStock() {
        return openingStock == null ? 0 : openingStock;
    }
}

