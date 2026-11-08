package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.CustomerReturnStatus;
import com.app.itibari.models.CustomerReturnItem;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customer_returns", schema = "itibari")
public class CustomerReturn extends BaseEntity{

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonView(BaseView.CustomerReturnView.class)
    @Column(name = "return_id")
    private String returnId;

    @JsonView(BaseView.CustomerReturnView.class)
    @ManyToOne
    @JoinColumn(name = "sales_order_id", referencedColumnName = "id")
    private SalesOrders salesOrder;

    @JsonView(BaseView.CustomerReturnView.class)
    @ManyToOne
    @JoinColumn(name = "delivery_agent_id", referencedColumnName = "id")
    private DeliveryAgents deliveryAgent;

    @JsonView(BaseView.CustomerReturnView.class)
    @Column(name = "product_size")
    private Integer productSize;

    @JsonView(BaseView.CustomerReturnView.class)
    @Column(name = "returned_value")
    private BigDecimal returnedValue;

    @JsonView(BaseView.CustomerReturnView.class)
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private CustomerReturnStatus status;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "tb_customer_return_items", schema = "itibari",
            joinColumns = @JoinColumn(name = "customer_return_items_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "customer_return_id", referencedColumnName = "id"))
    private List<CustomerReturnItem> customerReturnItems = new ArrayList<>();

    @JsonView(BaseView.CustomerReturnView.class)
    private String getCreatedByName(){
        if (this.getCreatedBy() != null){
            return this.getCreatedBy().getFullName();
        }else {
            return "--";
        }
    }
}
