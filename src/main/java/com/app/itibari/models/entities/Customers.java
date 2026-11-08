package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.*;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "customers", schema = "itibari")
public class Customers extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.CustomersView.class, BaseView.ReminderLogView.class, BaseView.OrderAmendmentView.class, BaseView.SalesOrdersView.class,
            BaseView.RouteOrderView.class, BaseView.DiscountView.class, BaseView.DetailedView.class, BaseView.TumaSmsView.class,
            BaseView.SupplierCustomerView.class})
    @Column(name = "name")
    private String name;

    @JsonView({BaseView.CustomersView.class, BaseView.OrderAmendmentView.class, BaseView.SalesOrdersView.class,
            BaseView.RouteOrderView.class, BaseView.DiscountView.class, BaseView.DetailedView.class,
            BaseView.SupplierCustomerView.class})
    @Column(name = "first_name")
    private String firstName;

    @JsonView({BaseView.CustomersView.class, BaseView.OrderAmendmentView.class, BaseView.SalesOrdersView.class,
            BaseView.RouteOrderView.class, BaseView.DiscountView.class, BaseView.DetailedView.class,
            BaseView.SupplierCustomerView.class})
    @Column(name = "last_name")
    private String lastName;

    @JsonView({BaseView.CustomersView.class, BaseView.SalesOrdersView.class})
    @Column(name = "email")
    private String email;

    @JsonView(BaseView.CustomersView.class)
    @Column(name = "national_id")
    private String nationalId;

    @JsonView({BaseView.CustomersView.class, BaseView.DetailedView.class, BaseView.RouteOrderView.class,
            BaseView.SalesOrdersView.class, BaseView.SupplierCustomerView.class, BaseView.CustomerFewDetailView.class, BaseView.TumaSmsView.class})
    @Column(name = "phone_number")
    private String phoneNumber;

    @JsonView(BaseView.CustomersView.class)
    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @JsonView(BaseView.CustomersView.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "source_channel")
    private SourceChannelEnum sourceChannel;

    @JsonView({BaseView.CustomersView.class, BaseView.RouteOrderView.class, BaseView.SalesOrdersView.class, BaseView.SupplierCustomerView.class})
    @Column(name = "latitude")
    private String latitude;

    @JsonView({BaseView.CustomersView.class, BaseView.RouteOrderView.class, BaseView.SalesOrdersView.class, BaseView.SupplierCustomerView.class})
    @Column(name = "longitude")
    private String longitude;

    @JsonView({BaseView.CustomersView.class, BaseView.DiscountView.class,
            BaseView.RouteOrderView.class, BaseView.CustomerFewDetailView.class,
            BaseView.SalesOrdersView.class, BaseView.DetailedView.class})
    @Column(name = "address")
    private String address;

    @JsonView({BaseView.CustomersView.class, BaseView.RouteOrderView.class, BaseView.SalesOrdersView.class, BaseView.SupplierCustomerView.class})
    @Column(name = "landmark")
    private String landmark;

    @JsonView({BaseView.CustomersView.class, BaseView.SalesOrdersView.class})
    @Column(name = "tin")
    private String tin;

    @JsonView({BaseView.CustomersView.class, BaseView.SupplierCustomerView.class, BaseView.CustomerFewDetailView.class})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_organization_region_fk", referencedColumnName = "id")
    private OrganizationRegions organizationRegions;

    @JsonView({BaseView.CustomersView.class, BaseView.SalesOrdersView.class,
            BaseView.RouteOrderView.class, BaseView.SupplierCustomerView.class, BaseView.CustomerFewDetailView.class})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_organization_location_fk", referencedColumnName = "id")
    private OrganizationLocations organizationLocation;

    @JsonView({BaseView.CustomersView.class, BaseView.DetailedView.class, BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Column(name = "customer_active_status")
    @Enumerated(EnumType.STRING)
    private CustomerActiveStatus customerActiveStatus;

    //@JsonView({BaseView.CustomersView.class, BaseView.SalesOrdersView.class,BaseView.RouteOrderView.class})
    @Column(name = "otp_code")
    private String otpCode;

    @JsonView({BaseView.CustomersView.class, BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Column(name = "is_verified")
    private Boolean isVerified = false;

    //@JsonView({BaseView.CustomersView.class, BaseView.SalesOrdersView.class,BaseView.RouteOrderView.class})
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // @JsonView({BaseView.CustomersView.class, BaseView.SalesOrdersView.class,BaseView.RouteOrderView.class})
    @Column(name = "attempt_count")
    private Integer attemptCount = 0;

    @Column(name = "agent_public_id")
    private UUID agentPublicId;

    @JsonView({BaseView.CustomersView.class, BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Column(name = "registered_by")
    private String registeredBy;

    @JsonView({BaseView.CustomersView.class, BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Column(name = "special_discount_requested")
    private boolean specialDiscountRequested = false;

    @JsonView({BaseView.CustomersView.class, BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Column(name = "special_discount_approved")
    private boolean specialDiscountApproved = false;

    @JsonView({BaseView.CustomersView.class, BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Column(name = "location_coordinates_verified")
    private Boolean locationCoordinatesVerified = false;

    @JsonView({BaseView.CustomersView.class, BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Column(name = "location_verification_otp")
    private String locationVerificationOtp;

    @Deprecated
    @Column(name = "show_phone_number_in_support_chat")
    private boolean showPhoneNumber = false;

    @JsonView({BaseView.CustomersView.class, BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Column(name = "show_phone_number_in_support_chats")
    private boolean showPhoneNumberInSupportChat = false;

    @ManyToOne
    @JoinColumn(name = "served_by_id")
    @JsonView({BaseView.CustomersView.class})
    private SalesAgents servedBy;

    @JsonView({BaseView.DetailedView.class, BaseView.CustomersView.class, BaseView.ReminderLogView.class})
    @Column(name = "credit_limit")
    private BigDecimal creditLimit = BigDecimal.ZERO;  // initial credit limit

    @JsonView({BaseView.CustomersView.class, BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Column(name = "customer_type")
    @Enumerated(EnumType.STRING)
    private CustomerType customerType;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "on_boarding_date")
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate onBoardingDate;


    @JsonView({BaseView.DetailedView.class, BaseView.CustomersView.class})
    @Column(name = "bnpl_account_created")
    private boolean bnplAccountCreated = false;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "last_credit_order_date")
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate lastCreditOrderDate;

    @JsonView({BaseView.DetailedView.class, BaseView.CustomersView.class})
    @Column(name = "bnpl_verification_status")
    @Enumerated(EnumType.STRING)
    private CustomerBnplVerificationStatus bnplVerificationStatus = CustomerBnplVerificationStatus.NOT_VERIFIED;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "self_onboarded")
    private boolean selfOnboarded = false;

    @Embedded
    @JsonView(BaseView.CustomersView.class)
    private OnBoardingData onBoardingData;

    @JsonView({BaseView.DetailedView.class, BaseView.CustomersView.class})
    @Column(name = "activated")
    private boolean active = true;

    @JsonView({BaseView.CustomersView.class, BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private CustomerStatus status;

    @JsonView(BaseView.OrderAmendmentView.class)
    @Column(name = "rejection_reason")
    private String rejectionReason;

    // not to be used per say, for future use
    @Column(name = "preliminary_aov")
    private BigDecimal preliminaryValue = BigDecimal.ZERO;

    @JsonView({BaseView.CustomersView.class, BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Column(name = "limited_offer_eligibility")
    private Boolean limitedOfferEligibility = false;

    @JsonView({BaseView.CustomersView.class, BaseView.ReminderLogView.class, BaseView.OrderAmendmentView.class, BaseView.SalesOrdersView.class,
            BaseView.RouteOrderView.class, BaseView.DiscountView.class, BaseView.DetailedView.class, BaseView.TumaSmsView.class,
            BaseView.SupplierCustomerView.class})
    @Column(name = "store_name")
    private String storeName;

    @JsonView({BaseView.CustomersView.class, BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Column(name = "awaiting_customer_type_change")
    private boolean awaitingCustomerTypeChange = false;

    @JsonView({BaseView.CustomersView.class, BaseView.OrderAmendmentView.class, BaseView.SalesOrdersView.class,
            BaseView.RouteOrderView.class, BaseView.DiscountView.class, BaseView.DetailedView.class,
            BaseView.SupplierCustomerView.class})
    @Column(name = "last_contact_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime lastContactDate;

    @JsonView({BaseView.CustomersView.class})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_status_id", referencedColumnName = "id")
    private CallStatus callStatus;

    @JsonView({BaseView.CustomersView.class})
    @Enumerated(EnumType.STRING)
    @Column(name = "lead_status")
    private ChurnedCustomerLeadStatus leadStatus;

    @JsonView({BaseView.CustomersView.class, BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Column(name = "last_order_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime lastOrderDate;

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    private List<SalesOrders> salesOrdersList = new ArrayList<>();

    //parent customer...default shop

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_customer_id", referencedColumnName = "id")
    private Customers parentCustomer;

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    private List<CustomerNotes> customerNotes = new ArrayList<>();

    @JsonView({BaseView.CustomersView.class, BaseView.ReminderLogView.class, BaseView.OrderAmendmentView.class, BaseView.SalesOrdersView.class,
            BaseView.RouteOrderView.class, BaseView.DiscountView.class, BaseView.DetailedView.class, BaseView.TumaSmsView.class,
            BaseView.SupplierCustomerView.class})
    @Column(name = "owner_number")
    private String ownerNumber;

    @JsonView({BaseView.CustomersView.class, BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Column(name = "previous_order_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime previousOrderDate;


    @JsonView({BaseView.CustomersView.class, BaseView.ReminderLogView.class, BaseView.OrderAmendmentView.class, BaseView.SalesOrdersView.class,
            BaseView.RouteOrderView.class, BaseView.DiscountView.class, BaseView.DetailedView.class, BaseView.TumaSmsView.class,
            BaseView.SupplierCustomerView.class})
    @Column(name = "shop_keeper_number")
    private String shopKeeperNumber;

    @JsonView({BaseView.CustomersView.class, BaseView.ReminderLogView.class, BaseView.OrderAmendmentView.class, BaseView.SalesOrdersView.class,
            BaseView.RouteOrderView.class, BaseView.DiscountView.class, BaseView.DetailedView.class, BaseView.TumaSmsView.class,
            BaseView.SupplierCustomerView.class})
    @Column(name = "has_other_stores")
    private boolean hasOtherStores = false;

    @JsonView({BaseView.CustomersView.class, BaseView.ReminderLogView.class, BaseView.OrderAmendmentView.class, BaseView.SalesOrdersView.class,
            BaseView.RouteOrderView.class, BaseView.DiscountView.class, BaseView.DetailedView.class, BaseView.TumaSmsView.class,
            BaseView.SupplierCustomerView.class})
    @Column(name = "can_child_customer_login")
    private boolean canChildCustomerLogin = true;

    @JsonView({BaseView.CustomersView.class, BaseView.ReminderLogView.class, BaseView.OrderAmendmentView.class, BaseView.SalesOrdersView.class,
            BaseView.RouteOrderView.class, BaseView.DiscountView.class, BaseView.DetailedView.class, BaseView.TumaSmsView.class,
            BaseView.SupplierCustomerView.class})
    @Column(name = "can_child_customer_place_order")
    private boolean canChildCustomerPlaceOrder = true;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "call_back_date")
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate callBackDate;

    @Transient
    @JsonView(BaseView.CustomersView.class)
    private boolean hasParentCustomer = false;

    @Transient
    private Long countDeliveredOrders;


    @Transient
    private Boolean hasAboveCreditLimit = false;


    @JsonView({BaseView.CustomersView.class, BaseView.OrderAmendmentView.class, BaseView.SalesOrdersView.class,
            BaseView.RouteOrderView.class, BaseView.DiscountView.class, BaseView.DetailedView.class,
            BaseView.SupplierCustomerView.class})
    public String getFullName() {
        return firstName + " " + lastName;
    }

    @JsonView({BaseView.CustomersView.class, BaseView.OrderAmendmentView.class, BaseView.ReminderLogView.class, BaseView.SalesOrdersView.class,
            BaseView.RouteOrderView.class, BaseView.DiscountView.class, BaseView.DetailedView.class,
            BaseView.SupplierCustomerView.class, BaseView.TumaSmsView.class, BaseView.SalesOrderMiniView.class})
    public String getName() {
        return (firstName != null && !firstName.isEmpty() &&
                lastName != null && !lastName.isEmpty())
                ? getFullName()
                : name;
    }

    public boolean isHasParentCustomer() {
        return parentCustomer != null;
    }

    @JsonView({BaseView.CustomersView.class, BaseView.OrderAmendmentView.class, BaseView.ReminderLogView.class, BaseView.SalesOrdersView.class,
            BaseView.RouteOrderView.class, BaseView.DiscountView.class, BaseView.DetailedView.class,
            BaseView.SupplierCustomerView.class, BaseView.TumaSmsView.class, BaseView.SalesOrderMiniView.class})
    public long getCountDeliveredOrders() {
        return countDeliveredOrders != null ? countDeliveredOrders : 0;
    }

    @JsonView({BaseView.CustomersView.class, BaseView.OrderAmendmentView.class, BaseView.SalesOrdersView.class,
            BaseView.RouteOrderView.class, BaseView.DiscountView.class, BaseView.DetailedView.class,
            BaseView.SupplierCustomerView.class})
    @Column(name = "call_made_by")
    private String callMadeBy;

    @Column(name = "follow_up_date")
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate followUpDate;

    @Column(name = "follow_up_time")
    private String followUpTime;


    public boolean isHasAboveCreditLimit() {
        return this.creditLimit != null && this.creditLimit.compareTo(new BigDecimal(100000)) > 0;
    }
}
