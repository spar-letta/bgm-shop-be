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
import java.time.temporal.ChronoUnit;
import java.util.*;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "sales_orders", schema = "itibari")
public class SalesOrders extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.OrderAmendmentView.class,
            BaseView.RouteOrderView.class, BaseView.SalesOrderMiniView.class, BaseView.CustomerReturnView.class})
    @Column(name = "sales_order_number")
    private String salesOrderNumber;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "source_channel")
    @Enumerated(EnumType.STRING)
    private SourceChannelEnum sourceChannel;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.OrderAmendmentView.class, BaseView.RouteOrderView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "loading_status")
    @Enumerated(EnumType.STRING)
    private LoadingStatusEnum salesOrderStatus;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.OrderAmendmentView.class, BaseView.RouteOrderView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "payment_status")
    @Enumerated(EnumType.STRING)
    private PaymentStatusEnum paymentStatus;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "booked_by")
    private String bookedBy;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.OrderAmendmentView.class, BaseView.RouteOrderView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "tax_amount")
    private BigDecimal taxAmount;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Column(name = "invoice_number")
    private String invoiceNumber;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.OrderAmendmentView.class, BaseView.SalesOrderMiniView.class})
    @OneToMany(mappedBy = "salesOrder")
    private Set<SalesOrderItems> salesOrderItems = new HashSet<>();

    @JsonView({BaseView.SalesOrdersView.class, BaseView.OrderAmendmentView.class, BaseView.RouteOrderView.class, BaseView.SalesOrderMiniView.class})
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "sales_order_customer_fk", referencedColumnName = "id")
    private Customers customer;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.OrderAmendmentView.class, BaseView.RouteOrderView.class})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_delivery_agent_fk", referencedColumnName = "id")
    private DeliveryAgents deliveryAgent;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class, BaseView.SalesOrderMiniView.class})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_warehouse_fk", referencedColumnName = "id")
    private Warehouse warehouse;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class, BaseView.SalesOrderMiniView.class})
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "sales_order_organization_region_fk", referencedColumnName = "id")
    private OrganizationRegions organizationRegions;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Column(name = "awaiting_approval")
    private boolean awaitingApproval;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "booked_by_sales_agent")
    private boolean bookedBySalesAgent;

    // sales agent public id
    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class, BaseView.SalesAgentsView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "sales_agent_public_id")
    private String salesAgentPublicId;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class, BaseView.OrderAmendmentView.class})
    @Column(name = "order_amendment_status")
    @Enumerated(EnumType.STRING)
    private OrderAmendStatus orderAmendStatus;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class, BaseView.SalesOrderMiniView.class})
    @Transient
    private BigDecimal totalAmountDelivered = BigDecimal.ZERO;

    // delivery status
    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class, BaseView.SalesOrderMiniView.class})
    @Transient
    @Enumerated(EnumType.STRING)
    private DeliveryStatus deliveryStatus;

    @Column(name = "route_public_id")
    private String routePublicId;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Column(name = "delivered_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime deliveryDate;

    @OneToOne(mappedBy = "salesOrder", cascade = CascadeType.ALL)
    private Invoice invoice;

    @Transient
    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class, BaseView.SalesOrderMiniView.class})
    private BigDecimal totalSalesOrderWeight;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "has_special_discount")
    private boolean hasSpecialDiscount = false;

    @ManyToOne
    @JoinColumn(name = "updated_by_customer", referencedColumnName = "id")
    private Customers updatedByCustomer;

    @ManyToOne
    @JoinColumn(name = "updated_by_sales_agent_fk", referencedColumnName = "id")
    private SalesAgents updatedBySalesAgent;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @ManyToOne
    @JoinColumn(name = "rejection_reason", referencedColumnName = "id")
    private RejectionsCode rejectionReason;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Column(name = "credit_applied")
    private boolean creditApplied = false;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Column(name = "customer_amount_to_pay")
    private BigDecimal customerAmountToPay = BigDecimal.ZERO;

    @Transient
    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    private BigDecimal totalDiscount = BigDecimal.ZERO;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class, BaseView.SalesOrderMiniView.class})
    @Transient
    private BigDecimal totalAmountPayable = BigDecimal.ZERO;

    @Column(name = "tax_amount_value")
    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class, BaseView.SalesOrderMiniView.class})
    public BigDecimal taxAmountValue = BigDecimal.ZERO;

    @Column(name = "sold_at")
    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class, BaseView.SalesOrderMiniView.class})
    private LocalDateTime soldAt;

    @OneToMany(mappedBy = "salesOrders")
    @JsonView(BaseView.SalesOrdersView.class)
    private List<RejectionReason> rejectionReasons = new ArrayList<>();


    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "tra_receipt_generated")
    private Boolean traReceiptGenerated = false;

    @JsonView(BaseView.SalesOrdersView.class)
    @Column(name = "tra_receipt_failed")
    private Boolean receiptFailed;

    @JsonView(BaseView.SalesOrdersView.class)
    @Column(name = "interest_rate")
    private BigDecimal interestRate = BigDecimal.ZERO;

    @JsonView(BaseView.SalesOrdersView.class)
    @Column(name = "interest_amount")
    private BigDecimal interestAmount = BigDecimal.ZERO;

    @JsonView(BaseView.SalesOrdersView.class)
    @Column(name = "credit_one_time_password")
    private String creditOneTimePassword;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Column(name = "has_valid_tin")
    private boolean hasValidTin = false;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Column(name = "credit_amount")
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @Column(name = "order_type")
    @Enumerated(EnumType.STRING)
    private OrderType orderType;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Column(name = "payment_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime paymentDate;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Column(name = "due_date")
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate dueDate;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.OrderAmendmentView.class, BaseView.RouteOrderView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private OrderStatusEnum status = OrderStatusEnum.Pending;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.OrderAmendmentView.class, BaseView.RouteOrderView.class})
    @Column(name = "delivered_amount")
    private BigDecimal deliveredAmount = BigDecimal.ZERO;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.OrderAmendmentView.class, BaseView.RouteOrderView.class})
    @Column(name = "locked")
    private Boolean locked = false;

    @Column(name = "sms_notify_in_planning")
    private boolean smsNotifyInPlanning = false;

    @Column(name = "sms_notify_in_transit")
    private boolean smsNotifyInTransit = false;

    @Column(name = "da_location_approval_request_attempts")
    private Integer daLocationApprovalRequestAttempts = 0;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "walk_in")
    private boolean walkIn;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "booked_by_parent")
    private boolean bookedByParent;

    @Column(name = "is_amended")
    private boolean amended = false;

    @Column(name = "static_discount")
    private BigDecimal staticDiscount = new BigDecimal("0.00");


    @Transient
    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class, BaseView.SalesOrderMiniView.class})
    private BigDecimal totalTaxPayable = BigDecimal.ZERO;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Transient
    private BigDecimal creditTotalAmount = BigDecimal.ZERO;

    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
    @Transient
    private String dueDays;


    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class, BaseView.SalesOrderMiniView.class})
    @Transient
    private Boolean enterpriseOrder;

    @Transient
    private Boolean hasParentCustomer;

    @Transient
    private Customers parentCustomer;


//    @Transient
//    @JsonView({BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class})
//    private BigDecimal discountOnTransit = BigDecimal.ZERO;

    public Long getDayDiff() {
        if (dueDate == null || deliveryDate == null) {
            return null;
        }

        LocalDate today = LocalDate.now();

        if (!today.isAfter(dueDate)) {
            return ChronoUnit.DAYS.between(deliveryDate.toLocalDate(), dueDate);
        } else {
            return -ChronoUnit.DAYS.between(dueDate, today);
        }
    }

    public String getDueDays() {
        Long days = getDayDiff();
        if (days == null) {
            return null;
        }

        if (paymentStatus == PaymentStatusEnum.PAID || paymentStatus == PaymentStatusEnum.CANCELLED) {
            return "0";
        }

        return days < 0 ? "+" + Math.abs(days) : String.valueOf(days);
    }


    public void addSalesOrderItem(SalesOrderItems salesOrderItem) {

        salesOrderItems.add(salesOrderItem);
        salesOrderItem.setSalesOrder(this);
    }

    public void removeSalesOrderItem(SalesOrderItems salesOrderItem) {
        salesOrderItems.remove(salesOrderItem);
        salesOrderItem.setSalesOrder(null);
    }

    public void calculateTotalTax() {

        this.taxAmount = salesOrderItems.stream()
                .map(item -> Optional.ofNullable(item.getTaxAmount()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalSalesOrderWeight() {

        return salesOrderItems.stream()
                .filter(Objects::nonNull)
                .map(SalesOrderItems::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalAmountDelivered() {
        return salesOrderItems.stream()
                .filter(item -> item.getQuantityDelivered() > 0)
                .map(item -> {
                    BigDecimal itemPrice = Optional.ofNullable(item.getPricePerUnit()).orElse(BigDecimal.ZERO);
                    int quantityDelivered = item.getQuantityDelivered();
                    return itemPrice.multiply(BigDecimal.valueOf(quantityDelivered));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateItemTotal(SalesOrderItems item) {

        BigDecimal itemPrice = Optional.ofNullable(item.getProductBeforePriceAfterTax())
                .orElse(BigDecimal.ZERO);
        BigDecimal discount = Optional.ofNullable(item.getDiscount())
                .orElse(BigDecimal.ZERO);
        BigDecimal taxRate = Optional.ofNullable(item.getProduct().getTaxCode().getCodeValue())
                .orElse(BigDecimal.ZERO);
        BigDecimal quantity = BigDecimal.valueOf(item.getQuantityDelivered());

        // Amount without VAT
        BigDecimal baseAmount = itemPrice.multiply(quantity);

        // VAT amount
        return itemPrice.subtract(discount)
                .multiply(taxRate)
                .multiply(quantity);
    }


    public DeliveryStatus getDeliveryStatus() {

        if (salesOrderStatus == LoadingStatusEnum.FAILED_DELIVERY) {
            return DeliveryStatus.Failed;
        }

        if (salesOrderItems == null || salesOrderItems.isEmpty()) {
            return DeliveryStatus.Pending;
        }

        boolean allNotDelivered = salesOrderItems.stream()
                .map(SalesOrderItems::getStatus)
                .filter(Objects::nonNull)
                .allMatch(status -> status == OrderItemStatus.NotDelivered);

        if (allNotDelivered) {
            return DeliveryStatus.NotDelivered;
        }

        boolean allDelivered = salesOrderItems.stream()
                .map(SalesOrderItems::getStatus)
                .filter(Objects::nonNull)
                .allMatch(status -> status == OrderItemStatus.Delivered);

        if (allDelivered) {
            return DeliveryStatus.Delivered;
        }

        return DeliveryStatus.PartiallyDelivered;
    }

    public BigDecimal getTotalDiscount() {
        if (walkIn) {
            BigDecimal specialDiscount = salesOrderItems.stream()
                    .filter(Objects::nonNull)
                    .map(item -> Optional.ofNullable(item.getWalkInSpecialDiscount()).orElse(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal volumeDiscount = salesOrderItems.stream()
                    .filter(Objects::nonNull)
                    .map(item -> Optional.ofNullable(item.getWalkInVolumeDiscount()).orElse(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return specialDiscount.add(volumeDiscount);
        } else {
            if (invoice == null || !invoice.isHasBeenApplied()) {
                return BigDecimal.ZERO;
            }
            return Optional.ofNullable(invoice.getDeductibleAmount()).orElse(BigDecimal.ZERO);
        }
    }


    public BigDecimal getTotalTaxAmount() {
        return salesOrderItems.stream()
                .map(this::calculateItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalAmountPayable() {

        if (salesOrderStatus == LoadingStatusEnum.DELIVERED) {
            return this.getTotalAmountDelivered().subtract(this.getTotalDiscount());
        }
        return this.totalAmount.subtract(this.getTotalDiscount());
    }

    public BigDecimal getTotalAmountPayableForReceipt(BigDecimal totalAmountDelivered) {

        return totalAmountDelivered.subtract(this.getTotalDiscount());
    }

    public BigDecimal getCreditTotalAmount() {
        if (this.creditApplied && this.interestAmount != null && this.creditAmount != null) {
            return this.creditAmount.add(this.interestAmount);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getTotalTaxPayable() {
        if (!salesOrderItems.isEmpty()) {
            return salesOrderItems.stream()
                    .filter(Objects::nonNull)
                    .map(SalesOrderItems::getTotalItemTaxAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return BigDecimal.ZERO;
    }

    public boolean areAllItemsDelivered() {
        return this.salesOrderItems
                .stream()
                .allMatch(item -> item.getStatus().equals(OrderItemStatus.Delivered));
    }

//    public BigDecimal getDiscountOnTransit(){
//        if(salesOrderStatus == LoadingStatusEnum.IN_TRANSIT){
//            return salesOrderItems.stream().map(SalesOrderItems::getDiscount).reduce(BigDecimal.ZERO,BigDecimal::add);
//        }
//        return BigDecimal.ZERO;
//    }

    public Boolean getEnterpriseOrder(){
        return customer.isHasOtherStores()|| customer.isHasParentCustomer();
    }




}
