package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.RouteStatus;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "route_planning", schema = "itibari")
public class RoutePlanning extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.RouteView.class)
    @Column(name = "route_id")
    private String routeId;

    @JsonView(BaseView.RouteView.class)
    @Column(name = "warehouse_name")
    private String warehouseName;

    @JsonView(BaseView.RouteView.class)
    @Column(name = "warehouse_id")
    private String warehouseId;

    @JsonView(BaseView.RouteView.class)
    @Column(name = "vehicle_id")
    private String vehicleId;

    @JsonView(BaseView.RouteView.class)
    @Column(name = "vehicle_name")
    private String vehicleName;

    @JsonView(BaseView.RouteView.class)
    @Column(name = "vehicle_weight")
    private String vehicleWeight;

    @JsonView(BaseView.RouteView.class)
    @Column(name = "delivery_agent_id")
    private String deliveryAgentId;

    @JsonView(BaseView.RouteView.class)
    @Column(name = "delivery_agent_name")
    private String deliveryAgentName;

    @JsonView(BaseView.RouteView.class)
    @Column(name = "route_region_id")
    private String routeRegionId;

    @JsonView(BaseView.RouteView.class)
    @Column(name = "route_region_name")
    private String routeRegionName;

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "route_sales_orders", schema = "itibari",
            joinColumns = @JoinColumn(name = "route_id"),
            inverseJoinColumns = @JoinColumn(name = "sales_order_id"))
    private List<SalesOrders> salesOrders = new ArrayList<>();

    @JsonView(BaseView.RouteView.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RouteStatus routeStatus;

    @Column(name = "van_sale_generated")
    private boolean vanSaleGenerated = false;

    @JsonView(BaseView.RouteView.class)
    @Column(name = "balance")
    private BigDecimal balance = BigDecimal.ZERO;

    @JsonView(BaseView.RouteView.class)
    @Column(name = "locked")
    private Boolean locked = false;

    @JsonView(BaseView.RouteView.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_wrapper_fk")
    private RouteWrapper routeWrapper;

    @JsonView(BaseView.RouteView.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @Column(name = "start_trip_time")
    private LocalDateTime endDateTime;

    @JsonView(BaseView.RouteView.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @Column(name = "end_trip_time")
    private LocalDateTime startDateTime;

    public void addSalesOrder(SalesOrders salesOrder) {

        Optional<SalesOrders> optionalSalesOrder = salesOrders.stream()
                .filter(salesOrder1 -> salesOrder1.getPublicId().equals(salesOrder.getPublicId()))
                .findFirst();

        if (optionalSalesOrder.isEmpty()) {
            salesOrders.add(salesOrder);
        }
    }

    public void removeSalesOrder(SalesOrders salesOrder) {

        salesOrders.removeIf(salesOrder1 -> salesOrder1.getPublicId().equals(salesOrder.getPublicId()));
    }
}
