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
@Table(name = "organization_locations", schema = "itibari")
public class OrganizationLocations extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.OrganizationLocationsView.class, BaseView.RouteOrderView.class})
    @Column(name = "code")
    private String code;

    @JsonView({BaseView.OrganizationLocationsView.class, BaseView.SalesAgentsView.class, BaseView.RouteOrderView.class,
            BaseView.CustomersView.class, BaseView.SalesOrdersView.class,
            BaseView.SupplierCustomerView.class, BaseView.CustomerFewDetailView.class})
    @Column(name = "name")
    private String name;

    @JsonView({BaseView.OrganizationLocationsView.class, BaseView.CustomersView.class, BaseView.SalesOrdersView.class})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_location_region_fk", referencedColumnName = "id")
    private OrganizationRegions organizationRegion;

    // latitude and longitude

    @JsonView({BaseView.OrganizationLocationsView.class, BaseView.CustomersView.class, BaseView.SalesAgentsView.class, BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Column(name = "latitude")
    private String latitude;

    @JsonView({BaseView.OrganizationLocationsView.class, BaseView.CustomersView.class, BaseView.SalesAgentsView.class, BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Column(name = "longitude")
    private String longitude;
}
