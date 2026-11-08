package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.AmountType;
import com.app.itibari.dataTypes.DiscountStatus;
import com.app.itibari.dataTypes.DiscountType;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "discounts", schema = "itibari")
public class Discounts extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.DiscountView.class)
    @Column(name = "name")
    private String discountName;

    @JsonView(BaseView.DiscountView.class)
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    @Column(name = "amountType")
    @Enumerated(EnumType.STRING)
    private AmountType amountType;

    @JsonView(BaseView.DiscountView.class)
    @Column(name = "amount_value")
    private BigDecimal amount = new BigDecimal("0.00");

    @Column(name = "product_value")
    private BigDecimal productValue = new BigDecimal("0.00");

    @Column(name = "min_quantity")
    private Integer minQuantity;

    @Deprecated
    @Column(name = "suspended")
    private Boolean suspended = false;

    @Deprecated
    @Column(name = "start_date")
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate startDate;

    @Deprecated
    @Column(name = "end_date")
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate endDate;

    @Deprecated
    @Column(name = "invoice_amount")
    private BigDecimal invoiceAmount = BigDecimal.ZERO;

    @ManyToMany
    @JoinTable(name = "discounts_customers", schema = "itibari",
            joinColumns = @JoinColumn(name = "discount_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "customer_id", referencedColumnName = "id"))
    private List<Customers> customersList = new ArrayList<>();

    @JsonView(BaseView.DiscountView.class)
    @ManyToMany
    @JoinTable(name = "discounts_products", schema = "itibari",
            joinColumns = @JoinColumn(name = "discount_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "product_id", referencedColumnName = "id"))
    private List<Product> productsList = new ArrayList<>();

    @Column(name = "customer_public_id")
    private UUID customerPublicId;

    @Column(name = "product_public_id")
    private UUID productPublicId;

    @Column(name = "order_public_id")
    private String orderPublicId;

    @Deprecated
    private boolean approved = false;

    @JsonView(BaseView.DiscountView.class)
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private DiscountStatus status;

    @ManyToMany
    @JoinTable(name = "volume_discount_range", schema = "itibari",
            joinColumns = @JoinColumn(name = "discount_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "range_id", referencedColumnName = "id"))
    private List<VolumeRange> volumeRanges = new ArrayList<>();


    @JsonView(BaseView.DiscountView.class)
    @ManyToMany
    @JoinTable(name = "special_products", schema = "itibari",
            joinColumns = @JoinColumn(name = "discount_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "special_item_id", referencedColumnName = "id"))
    private List<SpecialItem> specialItems = new ArrayList<>();

    @Transient
    @JsonView(BaseView.DiscountView.class)
    private String product;

    @Transient
    @JsonView(BaseView.DiscountView.class)
    private String customer;

    /**
     * {@link BaseEntity#getCreatedBy()} is {@code @JsonIgnore}; expose a slim view for discount lists.
     */
    @JsonProperty("createdBy")
    @JsonView(BaseView.DiscountView.class)
    public Map<String, String> getCreatedBySummary() {
        Person creator = getCreatedBy();
        if (creator == null) {
            return null;
        }
        Map<String, String> summary = new LinkedHashMap<>(2);
        summary.put("fullName", creator.getFullName());
        if (creator.getEmail() != null) {
            summary.put("email", creator.getEmail());
        }
        return summary;
    }

    public String getProduct() {

        if (discountType == DiscountType.VolumeDiscount && !productsList.isEmpty()) {
            return productsList.get(0).getSku();
        }
        return null;
    }

    public String getCustomer() {

        if (discountType == DiscountType.SpecialDiscount && !customersList.isEmpty()) {
            return customersList.get(0).getName();
        }
        return null;
    }

    // add customer

    public void addCustomer(List<Customers> customers) {

        for (Customers c : customers) {
            Optional<Customers> existingCustomer = customersList.stream()
                    .filter(cu -> cu.getPublicId().equals(c.getPublicId()))
                    .findFirst();

            if (existingCustomer.isEmpty()) {
                customersList.add(c);
            }
        }
    }

    public void addProduct(List<Product> products) {

        for (Product p : products) {
            Optional<Product> existingProduct = productsList.stream()
                    .filter(pr -> pr.getPublicId().equals(p.getPublicId()))
                    .findFirst();

            if (existingProduct.isEmpty()) {
                productsList.add(p);
            }
        }
    }

    // add volume range

    public void addVolumeRange(VolumeRange range) {

        Optional<VolumeRange> existingRange = volumeRanges.stream()
                .filter(r -> r.getPublicId().equals(range.getPublicId()))
                .findFirst();

        if (existingRange.isEmpty()) {
            volumeRanges.add(range);
        }
    }

    // remove volume range

    public void removeVolumeRange(VolumeRange range) {

        volumeRanges.removeIf(r -> r.getPublicId().equals(range.getPublicId()));
    }

    public BigDecimal rangeAmount(int quantity) {
        // where quantity ranges btn the range return the amount else zero
        for (VolumeRange range : volumeRanges) {
            if (quantity >= range.getMinVolume() && quantity <= range.getMaxVolume()) {
                return range.getDiscountedAmount();
            }
        }
        return BigDecimal.ZERO;
    }

    public boolean rangeExists(int minVolume, int maxVolume) {

        for (VolumeRange range : volumeRanges) {
            if (range.getDeleted() == Boolean.FALSE && minVolume == range.getMinVolume() && maxVolume == range.getMaxVolume()) {
                return true;
            }
        }
        return false;
    }

    public Integer getLeastVolumeRangeMoq() {

        return volumeRanges.stream().mapToInt(VolumeRange::getMinVolume).min().orElse(0);
    }
}
