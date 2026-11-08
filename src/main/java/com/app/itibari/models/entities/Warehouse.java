package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.WarehouseType;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "warehouse", schema = "itibari")
public class Warehouse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.WarehouseDetailView.class, BaseView.RegionMappingView.class, BaseView.VanSalesView.class,
            BaseView.OrderAmendmentView.class, BaseView.RouteOrderView.class, BaseView.VehicleView.class,
            BaseView.DeliveryAgentsView.class, BaseView.SalesOrdersView.class, BaseView.InventoryView.class,
            BaseView.ProductStockView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "warehouse_name")
    private String warehouseName;

    @JsonView(BaseView.WarehouseDetailView.class)
    @Column(name = "warehouse_street")
    private String warehouseStreet;

    @JsonView(BaseView.WarehouseDetailView.class)
    @Column(name = "warehouse_city")
    private String warehouseCity;

    @JsonView(BaseView.WarehouseDetailView.class)
    @Column(name = "warehouse_state")
    private String warehouseState;

    @JsonView(BaseView.WarehouseDetailView.class)
    @Column(name = "warehouse_postal_code")
    private String warehousePostalCode;

    @JsonView(BaseView.WarehouseDetailView.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "warehouse_type")
    private WarehouseType warehouseType;

    @JsonView({BaseView.WarehouseDetailView.class, BaseView.SalesOrdersView.class})
    @Column(name = "latitude")
    private String latitude;

    @JsonView({BaseView.WarehouseDetailView.class, BaseView.SalesOrdersView.class})
    @Column(name = "longitude")
    private String longitude;

    @JsonView(BaseView.WarehouseDetailView.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_organization_region_fk", referencedColumnName = "id")
    private OrganizationRegions organizationRegion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_fk_id", referencedColumnName = "id")
    private Warehouse parentWarehouse;

    @Column(name = "super_warehouse")
    private boolean superWarehouse;
}
