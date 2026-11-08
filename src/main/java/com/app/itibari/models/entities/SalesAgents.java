package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.IdentificationType;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "sales_agents", schema = "itibari")
public class SalesAgents extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.SalesAgentsView.class, BaseView.CustomersView.class, BaseView.WorkflowView.class, BaseView.SalesAgentsMinifiedView.class})
    @Column(name = "name")
    private String name;

    @JsonView(BaseView.SalesAgentsView.class)
    @Column(name = "email")
    private String email;

    @JsonView({BaseView.SalesAgentsView.class, BaseView.WorkflowView.class})
    @Column(name = "phone_number")
    private String phoneNumber;

    @JsonIgnore
    @Column(name = "password")
    private String password;

    @Column(name = "force_password_reset")
    private Boolean forcePasswordReset = false;

    @JsonView(BaseView.SalesAgentsView.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_agents_manager_person_fk", referencedColumnName = "id")
    private Person salesAgentManager;

    @JsonView(BaseView.SalesAgentsView.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_agents_organization_region_fk", referencedColumnName = "id")
    private OrganizationRegions organizationRegion;

    @JsonView(BaseView.SalesAgentsView.class)
    @Column(name = "identification_type")
    @Enumerated(EnumType.STRING)
    private IdentificationType identificationType;

    @JsonView(BaseView.SalesAgentsView.class)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "sales_agents_organization_locations", schema = "itibari",
            joinColumns = @JoinColumn(name = "sales_agents_id",
                    referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "organization_locations_id",
                    referencedColumnName = "id"))
    private List<OrganizationLocations> organizationLocations = new ArrayList<>();

    @JsonView(BaseView.SalesAgentsView.class)
    @Column(name = "identification_number")
    private String identificationNumber;

    @Column(name = "otp_code")
    private String otpCode;

    @JsonView(BaseView.SalesAgentsView.class)
    @Column(name = "is_verified")
    private Boolean isVerified = false;

    //  @JsonView(BaseView.SalesAgentsView.class)
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // @JsonView(BaseView.SalesAgentsView.class)
    @Column(name = "attempt_count")
    private Integer attemptCount = 0;

    @JsonView(BaseView.SalesAgentsView.class)
    @Column(name = "is_active")
    private boolean active = true;

    // , cascade = CascadeType.ALL, orphanRemoval = true
    @OneToMany(mappedBy = "servedBy")
    private List<Customers> customers = new ArrayList<>();

    public void addLocation(OrganizationLocations location) {

        Optional<OrganizationLocations> existingLocation = organizationLocations.stream()
                .filter(l -> l.getPublicId().equals(location.getPublicId()))
                .findFirst();
        if (existingLocation.isEmpty()) {
            organizationLocations.add(location);
        }
    }

    public void addCustomer(Customers customer) {
        customers.add(customer);
        customer.setServedBy(this);
    }

    public void removeCustomer(Customers customer) {
        customers.remove(customer);
        customer.setServedBy(null);
    }
}
