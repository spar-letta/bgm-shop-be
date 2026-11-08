package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.AmendAction;
import com.app.itibari.dataTypes.AmendmentStatus;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "order_amendments", schema = "itibari")
public class OrderAmendment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.OrderAmendmentView.class)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id", referencedColumnName = "id")
    private SalesOrders salesOrder;

    @JsonView(BaseView.OrderAmendmentView.class)
    @Column(name = "amendment_reason")
    private String description;

    @JsonView(BaseView.OrderAmendmentView.class)
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private AmendmentStatus status;

    @JsonView(BaseView.OrderAmendmentView.class)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejection_code_id", referencedColumnName = "id")
    private RejectionsCode rejectionsCode;

    @JsonView(BaseView.OrderAmendmentView.class)
    @Column(name = "route_id")
    private String routeId;

    @JsonView(BaseView.OrderAmendmentView.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "amend_action")
    private AmendAction amendAction;

    @JsonView(BaseView.OrderAmendmentView.class)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_agent_id", referencedColumnName = "id")
    private DeliveryAgents deliveryAgent;

    @JsonView(BaseView.OrderAmendmentView.class)
    @Column(name = "sales_order_public_id")
    private String salesOrderPublicId;

    @Column(name = "route_public_id")
    private String routePublicId;

    @Column(name = "comment")
    private String comment;

    @OneToMany(mappedBy = "orderAmendment", fetch = FetchType.LAZY)
    @JsonView(BaseView.OrderAmendmentView.class)
    private List<AmendmentProductItem> amendmentProductItems = new ArrayList<>();

    @JsonView(BaseView.OrderAmendmentView.class)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cx_reason_code_id", referencedColumnName = "id")
    private RejectionsCode cxReasonCode;

    @JsonView(BaseView.OrderAmendmentView.class)
    @Column(name = "failed_order")
    private boolean failedOrder;

    @JsonView({BaseView.OrderAmendmentView.class})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_status_id", referencedColumnName = "id")
    private CallStatus callStatus;

    @Transient
    @JsonView(BaseView.OrderAmendmentView.class)
    private BigDecimal amendedTotal;

    @JsonView(BaseView.OrderAmendmentView.class)
    public BigDecimal diffPreviousSalesAmendmentTotal() {
        if (salesOrder == null || salesOrder.getTotalAmount() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal amendedTotal = getAmendedTotal();
        if (amendedTotal == null) {
            amendedTotal = BigDecimal.ZERO;
        }

        return salesOrder.getTotalAmount().subtract(amendedTotal);
    }


    public void addProductItem(AmendmentProductItem productItem) {

        amendmentProductItems.add(productItem);
        productItem.setOrderAmendment(this);
    }

    public void removeProductItem(AmendmentProductItem productItem) {

        amendmentProductItems.removeIf(p -> p.getPublicId().equals(productItem.getPublicId()));
        productItem.setOrderAmendment(null);
    }

    public BigDecimal getAmendedTotal() {
        if (amendmentProductItems == null || amendmentProductItems.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return amendmentProductItems.stream()
                .map(item -> {
                    BigDecimal price = item.getPricePerUnit() != null ? item.getPricePerUnit() : BigDecimal.ZERO;
                    Integer qty = item.getAmendmentQuantity() != null ? item.getAmendmentQuantity() : 0;
                    return price.multiply(BigDecimal.valueOf(qty));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


}
