package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.IdentificationType;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
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
@Table(name = "person", schema = "itibari")
public class Person extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.UserView.class, BaseView.purchaseOrderSummaryView.class, BaseView.EventsView.class,
            BaseView.Profile.class, BaseView.UserViewDetails.class})
    @Column(name = "first_name")
    private String firstName;

    @JsonView({BaseView.UserView.class, BaseView.Profile.class, BaseView.UserViewDetails.class})
    @Column(name = "last_name")
    private String lastName;

    @JsonView({BaseView.UserView.class, BaseView.Profile.class, BaseView.UserViewDetails.class})
    @Column(name = "email")
    private String email;

    @JsonView({BaseView.UserView.class, BaseView.Profile.class, BaseView.UserViewDetails.class})
    @Column(name = "phone")
    private String phoneNumber;

    @JsonView({BaseView.UserView.class, BaseView.Profile.class, BaseView.UserViewDetails.class})
    @Column(name = "address")
    private String address;

    @JsonView({BaseView.UserView.class, BaseView.Profile.class, BaseView.UserViewDetails.class})
    @Column(name = "city")
    private String city;

    @JsonView({BaseView.UserView.class, BaseView.Profile.class})
    @Column(name = "state")
    private String state;

    @JsonIgnore
    @Column(name = "password")
    private String password;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "person_roles", schema = "itibari",
            joinColumns = @JoinColumn(name = "person_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
    private List<Role> roles = new ArrayList<>();

    @Column(name = "reset_pin_expiry")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime expiryDate;

    @Column(name = "reset_pin")
    private String resetPin;

    @JsonView({BaseView.UserView.class, BaseView.UserViewDetails.class})
    @Column(name = "identification_type")
    @Enumerated(EnumType.STRING)
    private IdentificationType identificationType;

    @JsonView({BaseView.UserView.class, BaseView.Profile.class, BaseView.UserViewDetails.class})
    @Column(name = "identification_number")
    private String identificationNumber;

    @JsonView(BaseView.Profile.class)
    @Column(name = "supplier_public_id")
    private String supplierPublicId;

    @Column(name = "has_supplier_account")
    private Boolean hasSupplierAccount = false;

    @JsonView({BaseView.Profile.class, BaseView.UserViewDetails.class})
    @Column(name = "main_supplier")
    private Boolean mainSupplier = false;

    @JsonView({BaseView.UserView.class, BaseView.Profile.class, BaseView.UserViewDetails.class})
    @ManyToOne
    @JoinColumn(name = "department_id_fk", referencedColumnName = "id")
    private Department department;

    @Column(name = "caller_id")
    private String callerId;

    @JsonView({BaseView.UserView.class, BaseView.Profile.class, BaseView.UserViewDetails.class})
    @Column(name = "finance_service_officer")
    private Boolean financeServiceOfficer = Boolean.FALSE;

    @Column(name = "accepted_terms")
    private boolean acceptedTerms;

    @JsonView({BaseView.class, BaseView.Profile.class,BaseView.DetailedView.class, BaseView.DiscountView.class, BaseView.DiscountApprovalView.class, BaseView.DebitNotesView.class,
            BaseView.RegionMappingView.class, BaseView.CustomersView.class, BaseView.ReconciliationSummaryView.class, BaseView.PriceManagementSummaryView.class,
            BaseView.PackingView.class, BaseView.RouteView.class, BaseView.VehicleView.class, BaseView.GoodsReceived.class,
            BaseView.RemittanceSummaryView.class, BaseView.GoodsReturned.class, BaseView.FileDataView.class, BaseView.WarehouseDetailView.class, BaseView.ProductSummaryView.class,
            BaseView.SupplierSummaryView.class, BaseView.ProductDetailView.class, BaseView.SummaryView.class, BaseView.RoleView.class,
            BaseView.DeliveryAgentsView.class, BaseView.SalesAgentsView.class, BaseView.ProfileFewDetails.class,
            BaseView.TaxCodeView.class, BaseView.purchaseOrderSummaryView.class, BaseView.UserViewDetails.class,BaseView.TraSwitchView.class})
    @Transient
    private String fullName;

    @JsonView(BaseView.UserView.class)
    @Transient
    private String roleName;

    @Transient
    @JsonView(BaseView.Profile.class)
    private List<String> rolesName = new ArrayList<>();

    @Transient
    @JsonView(BaseView.Profile.class)
    private List<String> permissions = new ArrayList<>();

    @Transient
    @JsonView(BaseView.UserViewDetails.class)
    private String createdByFullName;

    public String getCreatedByFullName() {
        if (this.getCreatedBy() != null) {
            return this.getCreatedBy().getFullName();
        } else {
            return "--";
        }
    }


    public String getFullName() {

        return firstName + " " + lastName;
    }

    public void removeRole(Role role) {

        roles.removeIf(r -> r.getPublicId().equals(role.getPublicId()));
    }

    public void addRole(Role role) {

        Optional<Role> roleOptional = roles.stream().filter(r -> r.getPublicId().equals(role.getPublicId())).findFirst();
        if (roleOptional.isEmpty()) {
            roles.add(role);
        }
    }

    public String getRoleName() {

        if (roles != null && !roles.isEmpty()) {
            return roles.get(0).getRoleName();
        }
        return null;
    }

    public List<String> getRolesName() {

        if (roles != null && !roles.isEmpty()) {
            roles.forEach(role -> rolesName.add(role.getRoleName()));
        }
        return rolesName;
    }

    public List<String> getPermissions() {

        if (roles != null && !roles.isEmpty()) {
            roles.forEach(role -> {
                if (role.getPermissions() != null && !role.getPermissions().isEmpty()) {
                    role.getPermissions().forEach(permission -> {
                        if (!permissions.contains(permission.getName())) {
                            permissions.add(permission.getName());
                        }
                    });
                }
            });
        }
        return permissions;
    }
}
