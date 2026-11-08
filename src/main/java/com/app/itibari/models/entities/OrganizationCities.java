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
@Table(name = "organization_cities", schema = "itibari")
public class OrganizationCities extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.OrganizationCitiesView.class})
    @Column(name = "code")
    private String code;

    @JsonView({BaseView.OrganizationCitiesView.class, BaseView.OrganizationRegionsView.class, BaseView.SalesOrderMiniView.class,
            BaseView.OrganizationLocationsView.class, BaseView.CustomersView.class, BaseView.SalesAgentsView.class, BaseView.SalesOrdersView.class})
    @Column(name = "name")
    private String name;

    @JsonView({BaseView.OrganizationCitiesView.class, BaseView.OrganizationRegionsView.class, BaseView.OrganizationLocationsView.class, BaseView.CustomersView.class, BaseView.SalesAgentsView.class, BaseView.SalesOrdersView.class})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_country_fk", referencedColumnName = "id")
    private OrganizationCountries organizationCountry;
}
