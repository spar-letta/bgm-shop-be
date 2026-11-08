package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.DebtZone;
import com.app.itibari.dataTypes.IdentificationType;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "delivery_agents", schema = "itibari")
public class DeliveryAgents extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.DeliveryAgentsView.class, BaseView.SalesOrdersView.class, BaseView.WorkflowView.class})
    @Column(name = "first_name")
    private String firstName;

    @JsonView({BaseView.DeliveryAgentsView.class, BaseView.SalesOrdersView.class, BaseView.WorkflowView.class})
    @Column(name = "last_name")
    private String lastName;

    @Deprecated
    @Column(name = "national_id")
    private String nationalId;

    @JsonView({BaseView.DeliveryAgentsView.class, BaseView.SalesOrdersView.class, BaseView.WorkflowView.class})
    @Column(name = "phone_number")
    private String phoneNumber;

    @JsonView(BaseView.DeliveryAgentsView.class)
    @Column(name = "residential_address")
    private String residentialAddress;

    @JsonView({BaseView.DeliveryAgentsView.class, BaseView.VanSalesView.class, BaseView.OrderAmendmentView.class,})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id_fk", referencedColumnName = "id")
    private Warehouse warehouse;

    @JsonIgnore
    @Column(name = "password")
    private String password;

    @Column(name = "force_password_reset")
    private Boolean forcePasswordReset = false;

    @JsonView({BaseView.DeliveryAgentsView.class, BaseView.OrderAmendmentView.class, BaseView.VanSalesView.class,
            BaseView.OrderAmendmentView.class, BaseView.WorkflowView.class, BaseView.RouteOrderView.class,
            BaseView.RouteOrderView.class, BaseView.ReturnView.class, BaseView.DeliveryAgentsMinifiedView.class,
            BaseView.CustomerReturnView.class, BaseView.SalesOrdersView.class})
    @Transient
    private String fullName;

    @JsonView({BaseView.DeliveryAgentsView.class, BaseView.VanSalesView.class, BaseView.RouteOrderView.class, BaseView.RouteOrderView.class})
    @Column(name = "available")
    private boolean available = true;

    @JsonView({BaseView.DeliveryAgentsView.class, BaseView.RouteOrderView.class, BaseView.RouteOrderView.class})
    @Column(name = "identification_type")
    @Enumerated(EnumType.STRING)
    private IdentificationType identificationType;

    @JsonView({BaseView.DeliveryAgentsView.class, BaseView.RouteOrderView.class, BaseView.RouteOrderView.class})
    @Column(name = "identification_number")
    private String identificationNumber;

    @JsonView({BaseView.DeliveryAgentsView.class, BaseView.RouteOrderView.class, BaseView.RouteOrderView.class})
    @Column(name = "debt_zone")
    @Enumerated(EnumType.STRING)
    private DebtZone debtZone;

    @Column(name = "otp_code")
    private String otpCode;

    @JsonView({BaseView.DeliveryAgentsView.class, BaseView.RouteOrderView.class, BaseView.RouteOrderView.class})
    @Column(name = "is_verified")
    private Boolean isVerified = false;

    // @JsonView({BaseView.DeliveryAgentsView.class, BaseView.RouteOrderView.class, BaseView.RouteOrderView.class})
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // @JsonView({BaseView.DeliveryAgentsView.class, BaseView.RouteOrderView.class, BaseView.RouteOrderView.class})
    @Column(name = "attempt_count")
    private Integer attemptCount = 0;

    @Column(name = "loaded")
    @JsonView({BaseView.DeliveryAgentsView.class, BaseView.RouteOrderView.class, BaseView.RouteOrderView.class})
    private boolean loaded = false;

    public String getFullName() {

        return firstName + " " + lastName;
    }
}
