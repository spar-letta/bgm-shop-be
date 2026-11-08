package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "organization_regions", schema = "itibari")
public class OrganizationRegions extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.OrganizationRegionsView.class, BaseView.RegionMappingView.class})
    @Column(name = "code")
    private String code;

    @JsonView({BaseView.OrganizationRegionsView.class, BaseView.RegionMappingView.class,
            BaseView.RouteOrderView.class, BaseView.OrganizationLocationsView.class, BaseView.CustomersView.class,
            BaseView.SalesAgentsView.class, BaseView.SalesOrdersView.class,
            BaseView.SupplierCustomerView.class, BaseView.CustomerFewDetailView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "name")
    private String name;

    @JsonView({BaseView.OrganizationRegionsView.class, BaseView.OrganizationLocationsView.class,
            BaseView.CustomersView.class, BaseView.SalesAgentsView.class, BaseView.SalesOrdersView.class, BaseView.SalesOrderMiniView.class})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_city_fk", referencedColumnName = "id")
    private OrganizationCities organizationCity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_warehouse_mapping_fk", referencedColumnName = "id")
    private RegionWarehouseMapping regionWarehouseMapping;

    // latitude and longitude

    @JsonView({BaseView.OrganizationRegionsView.class, BaseView.CustomersView.class, BaseView.SalesAgentsView.class, BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class, BaseView.OrganizationLocationsView.class})
    @Column(name = "latitude")
    private String latitude;

    @JsonView({BaseView.OrganizationRegionsView.class, BaseView.CustomersView.class, BaseView.SalesAgentsView.class, BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class, BaseView.OrganizationLocationsView.class})
    @Column(name = "longitude")
    private String longitude;
}
