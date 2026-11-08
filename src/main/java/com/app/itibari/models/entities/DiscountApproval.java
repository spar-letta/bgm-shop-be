package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.DiscountApprovalStatus;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "discount_approval", schema = "itibari")
public class DiscountApproval extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.DiscountApprovalView.class)
    @Column(name = "approval_status")
    @Enumerated(EnumType.STRING)
    private DiscountApprovalStatus approvalStatus;

    @JsonView(BaseView.DiscountApprovalView.class)
    @Column(name = "requested_by")
    private String requestedBy;

    @JsonView(BaseView.DiscountApprovalView.class)
    @Column(name = "requested_by_public_id")
    private String requestedByPublicId;

    @JsonView(BaseView.DiscountApprovalView.class)
    @Column(name = "customer")
    private String customer;

    @Column(name = "customer_public_id")
    private String customerPublicId;

    // @JsonView(BaseView.DiscountApprovalView.class)
    @Column(name = "amount")
    private BigDecimal amount = BigDecimal.ZERO;

    // @JsonView(BaseView.DiscountApprovalView.class)
    @Column(name = "reason")
    private String reason;

    @JsonView(BaseView.DiscountApprovalView.class)
    @Column(name = "first_approved")
    private Boolean firstApproved = false;

    @Column(name = "otp")
    private String otp;

    @Column(name = "expiry_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime expiryTime;

    @Column(name = "otp_used")
    private boolean otpUsed = false;


    @Column(name = "has_van_sales")
    private Boolean hasVanSales = false;

    @Deprecated
    @ManyToMany
    @JoinTable(
            name = "discount_approval_products",
            schema = "itibari",
            joinColumns = @JoinColumn(name = "discount_approval_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "product_id", referencedColumnName = "id")
    )
    private List<Product> products = new ArrayList<>();

    @JsonView(BaseView.DiscountApprovalView.class)
    @ManyToMany
    @JoinTable(
            name = "requestItems_approval_products",
            schema = "itibari",
            joinColumns = @JoinColumn(name = "requestItems_approval_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "requestItems_id", referencedColumnName = "id")
    )
    private List<RequestItems> requestItems = new ArrayList<>();

    @Transient
    @JsonView(BaseView.DiscountApprovalView.class)
    private BigDecimal totalDiscountRequested = BigDecimal.ZERO;

    @Transient
    @JsonView(BaseView.DiscountApprovalView.class)
    private BigDecimal totalDiscountApproved = BigDecimal.ZERO;

    public BigDecimal getTotalDiscountRequested() {

        BigDecimal requestedAmount = BigDecimal.ZERO;

        if (!requestItems.isEmpty()) {
            for (RequestItems ri : requestItems) {
                if (ri != null) {
                    requestedAmount = requestedAmount.add(ri.getTotalDiscountRequest());
                }
            }
        }

        return requestedAmount;
    }

    public BigDecimal getTotalDiscountApproved() {

        BigDecimal approvedAmount = BigDecimal.ZERO;

        if (!requestItems.isEmpty()) {
            for (RequestItems ri : requestItems) {
                if (ri != null) {
                    approvedAmount = approvedAmount.add(ri.getTotalApprovedDiscount());

                }
            }
        }

        return approvedAmount;
    }
}
