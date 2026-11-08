package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.PurchaseOrderStatus;
import com.app.itibari.models.request.PurchaseOrderItemRequest;
import com.app.itibari.models.request.PurchaseOrderRequest;
import com.app.itibari.repository.ProductRepository;
import com.app.itibari.services.helper.TaxCalculatorHelper;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "purchase_order", schema = "itibari")
public class PurchaseOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @Column(name = "purchase_order_number")
    private String purchaseOrderNumber;

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @Column(name = "purchase_order_date")
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate purchaseOrderDate;

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PurchaseOrderStatus status;

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @Column(name = "notes")
    private String notes;

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id_fk", referencedColumnName = "id")
    private Suppliers supplier;

//    @Where(clause = "deleted = false")
//    @JsonView(BaseView.purchaseOrderSummaryView.class)
//    @OneToMany(fetch = FetchType.LAZY, mappedBy = "purchaseOrder")
//    private List<PurchaseOrderItem> purchaseOrderItems = new ArrayList<>();

    @OneToMany(
            fetch = FetchType.LAZY,
            mappedBy = "purchaseOrder",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Where(clause = "deleted = false")
    @JsonView(BaseView.purchaseOrderSummaryView.class)
    private List<PurchaseOrderItem> purchaseOrderItems = new ArrayList<>();


    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @Column(name = "proforma_invoice_number")
    private String proformaInvoiceNumber;

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @Column(name = "payment_reference")
    private String paymentReference;

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @Transient
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @Column(name = "paid_amount")
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "warehouse_id")
    @JsonView(BaseView.purchaseOrderSummaryView.class)
    private String warehouseId;

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @Column(name = "warehouse_name")
    private String warehouseName;

    @Column(name = "is_rebate")
    @JsonView(BaseView.purchaseOrderSummaryView.class)
    private boolean rebate = false;

    @Column(name = "is_replacement")
    @JsonView(BaseView.purchaseOrderSummaryView.class)
    private boolean replacementPurchaseOrder = false;

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @Transient
    private BigDecimal balance = BigDecimal.ZERO;

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @Transient
    private BigDecimal payableAmount = BigDecimal.ZERO;

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @Transient
    private BigDecimal totalAmountAfterTax = BigDecimal.ZERO;

    @Transient
    @JsonView(BaseView.purchaseOrderSummaryView.class)
    private BigDecimal totalTaxAmount = BigDecimal.ZERO;

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @Transient
    private boolean resolved = false;

    @Transient
    @JsonView(BaseView.purchaseOrderSummaryView.class)
    private String supplierName;

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @OneToMany(mappedBy = "purchaseOrder")
    private List<RejectionReason> rejectionReasons = new ArrayList<>();

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @ManyToMany
    @JoinTable(
            name = "purchase_order_rejections_codes",
            schema = "itibari",
            joinColumns = @JoinColumn(name = "po_rejection_code_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "rejection_code_id", referencedColumnName = "id"))
    private List<RejectionsCode> rejectionsCodes = new ArrayList<>();

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @Column(name = "previous_status")
    @Enumerated(EnumType.STRING)
    private PurchaseOrderStatus previousStatus;

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @Column(name = "goods_return_note_number_reference")
    private String goodsReturnNoteNumberReference;

    @Transient
    @JsonView(BaseView.purchaseOrderSummaryView.class)
    private BigDecimal totalAmountReceived = BigDecimal.ZERO;

    /**
     * calculates the total amount of the purchase order.
     * It sums up the total amount of each purchase order item.
     * If there are no purchase order items, it returns BigDecimal.ZERO.
     *
     * @return
     */
    public BigDecimal getTotalAmount() {

        BigDecimal amount = BigDecimal.ZERO;
        if (purchaseOrderItems != null && !purchaseOrderItems.isEmpty()) {
            amount = purchaseOrderItems.
                    stream()
                    .map(PurchaseOrderItem::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP);
        }
        return amount;
    }

    public void addPurchaseOrderItem(PurchaseOrderItem savedPurchaseOrderItem) {

        Optional<PurchaseOrderItem> optionalPurchaseOrderItem = purchaseOrderItems.stream()
                .filter(purchaseOrderItem -> purchaseOrderItem.getPublicId().equals(savedPurchaseOrderItem.getPublicId()))
                .findFirst();

        if (optionalPurchaseOrderItem.isEmpty()) {
            purchaseOrderItems.add(savedPurchaseOrderItem);
        }
    }

    public void removePurchaseOrderItem(PurchaseOrderItem savedPurchaseOrderItem) {

        Optional<PurchaseOrderItem> optionalPurchaseOrderItem = purchaseOrderItems.stream()
                .filter(purchaseOrderItem -> purchaseOrderItem.getPublicId().equals(savedPurchaseOrderItem.getPublicId()))
                .findFirst();

        if (optionalPurchaseOrderItem.isPresent()) {
            purchaseOrderItems.remove(savedPurchaseOrderItem);
        }
    }

    public boolean isResolved() {

        if (purchaseOrderItems != null && !purchaseOrderItems.isEmpty()) {
            purchaseOrderItems.forEach(purchaseOrderItem -> {
                resolved = purchaseOrderItem.getQuantityReceived() + purchaseOrderItem.getQuantityReturned() >= purchaseOrderItem.getQuantity();
            });
        }

        return resolved;
    }

    public BigDecimal getTotalTaxAmount() {

        BigDecimal amount = BigDecimal.ZERO;
        if (purchaseOrderItems != null && !purchaseOrderItems.isEmpty()) {
            amount = purchaseOrderItems.
                    stream()
                    .map(PurchaseOrderItem::getCalculatedTaxAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP);
        }
        return amount;
    }

    public BigDecimal getPayableAmount() {

        return getTotalAmount().add(getTotalTaxAmount()).setScale(4, RoundingMode.HALF_UP);
    }

    public String getSupplierName() {

        if (supplier != null) {
            return supplier.getSupplierName();
        }
        return null;
    }

    public BigDecimal getBalance() {

        if (paidAmount != null && paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            return getPayableAmount().subtract(paidAmount).max(BigDecimal.ZERO);
        } else {
            return getPayableAmount().max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
        }
    }

    public BigDecimal getTotalAmountAfterTax() {

        if (purchaseOrderItems != null && !purchaseOrderItems.isEmpty()) {
            totalAmountAfterTax = purchaseOrderItems.
                    stream()
                    .map(PurchaseOrderItem::getAmountAfterTax)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return totalAmountAfterTax.setScale(4, RoundingMode.HALF_UP);
    }

    public void updateFromRequest(PurchaseOrderRequest request, Person updatedBy, Suppliers suppliers, Warehouse warehouseOptional, RejectionsCode rejectionsCode) {
        this.setSupplier(suppliers);
        this.setPurchaseOrderDate(request.getOrderDate());
        this.setNotes(request.getComment());
        this.setProformaInvoiceNumber(request.getProformaInvoiceNumber());
        this.setRebate(request.isRebate());
        this.setUpdateDate(LocalDateTime.now());
        this.setWarehouseId(String.valueOf(warehouseOptional.getPublicId()));
        this.setWarehouseName(String.valueOf(warehouseOptional.getWarehouseName()));
        this.setUpdatedBy(updatedBy);
        this.getRejectionsCodes().add(rejectionsCode);
    }

    public BigDecimal getTotalAmountReceived() {

        BigDecimal amount = BigDecimal.ZERO;
        if (purchaseOrderItems != null && !purchaseOrderItems.isEmpty()) {
            amount = purchaseOrderItems.stream()
                    .map(PurchaseOrderItem::getAmountReceived)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return amount;
    }
}
