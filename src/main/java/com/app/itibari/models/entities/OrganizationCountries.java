package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "organization_countries", schema = "itibari")
public class OrganizationCountries extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.OrganizationCountriesView.class, BaseView.OrganizationCitiesView.class, BaseView.OrganizationRegionsView.class, BaseView.OrganizationLocationsView.class, BaseView.CustomersView.class, BaseView.SalesAgentsView.class, BaseView.SalesOrdersView.class})
    @Column(name = "code")
    private String code;

    @JsonView({BaseView.OrganizationCountriesView.class, BaseView.OrganizationCitiesView.class, BaseView.OrganizationRegionsView.class, BaseView.OrganizationLocationsView.class, BaseView.CustomersView.class, BaseView.SalesAgentsView.class, BaseView.SalesOrdersView.class})
    @Column(name = "name")
    private String name;

    @JsonView(BaseView.OrganizationCountriesView.class)
    @Column(name = "customer_care_supervisor")
    private String customerCareSupervisor;
}
