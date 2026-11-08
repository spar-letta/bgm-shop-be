package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.TransactionType;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "avco", schema = "itibari")
public class AvcoItems extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.AvcoView.class)
    @Column(name = "product_name")
    private String productName;

    @JsonView(BaseView.AvcoView.class)
    @Column(name = "product_sku")
    private String productSku;

    @JsonView(BaseView.AvcoView.class)
    @Column(name = "product_public_id")
    private String productPublicId;

    @JsonView(BaseView.AvcoView.class)
    @Column(name = "transaction_type")
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @JsonView(BaseView.AvcoView.class)
    @Column(name = "purchase_order_number")
    private String purchaseOrderNumber;

    @JsonView(BaseView.AvcoView.class)
    @Column(name = "quantity")
    private Long quantity;

    @JsonView(BaseView.AvcoView.class)
    @Column(name = "avco")
    private BigDecimal avco = BigDecimal.ZERO;

    @JsonView(BaseView.AvcoView.class)
    @Column(name = "unit_cost")
    private BigDecimal unitCost = BigDecimal.ZERO;

    @JsonView(BaseView.AvcoView.class)
    @Column(name = "total_value")
    private BigDecimal value = BigDecimal.ZERO;

    @JsonView(BaseView.AvcoView.class)
    @Column(name = "transaction_date")
    @JsonFormat(pattern = "dd-MM-yyyy", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate transactionDate;
}
