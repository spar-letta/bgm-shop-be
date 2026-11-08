package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Deprecated
@Hidden
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "price_management", schema = "itibari")
public class PriceManagement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.PriceManagementSummaryView.class)
    @Column(name = "price")
    private BigDecimal price;

    @JsonView(BaseView.PriceManagementSummaryView.class)
    @Column(name = "product")
    private String product;

    @JsonView(BaseView.PriceManagementSummaryView.class)
    @Column(name = "sku")
    private String sku;

    @JsonView(BaseView.PriceManagementSummaryView.class)
    @Column(name = "bulk_number")
    private String bulkNumber;
}
