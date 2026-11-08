package com.app.itibari.models.entities;

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
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity implements Serializable {

    private static final ZoneId NAIROBI_ZONE = ZoneId.of("Africa/Nairobi");

    @JsonView({BaseView.class, BaseView.PackingView.class, BaseView.ReturnView.class, BaseView.Profile.class,
            BaseView.DiscountApprovalView.class, BaseView.DebitNotesView.class, BaseView.ReconciliationSummaryView.class, BaseView.TaxMappingSummaryView.class,
            BaseView.RouteView.class, BaseView.VehicleView.class, BaseView.GoodsReceived.class,
            BaseView.RemittanceSummaryView.class, BaseView.InventoryView.class,
            BaseView.RejectionCodeView.class, BaseView.GoodsReturned.class,
            BaseView.FileDataView.class, BaseView.purchaseOrderSummaryView.class,
            BaseView.WarehouseDetailView.class, BaseView.ProductSummaryView.class,
            BaseView.SupplierSummaryView.class, BaseView.InventorySummaryView.class,
            BaseView.ProductSummaryView.class, BaseView.SummaryView.class,
            BaseView.PermissionGroupView.class, BaseView.RoleView.class, BaseView.ProductDetailView.class,
            BaseView.DeliveryAgentsView.class, BaseView.SalesAgentsView.class,
            BaseView.OrganizationCountriesView.class, BaseView.OrganizationCitiesView.class,
            BaseView.OrganizationRegionsView.class, BaseView.OrderAmendmentView.class,
            BaseView.OrganizationLocationsView.class, BaseView.PriceManagementSummaryView.class,
            BaseView.CustomersView.class, BaseView.RegionMappingView.class, BaseView.DiscountView.class,
            BaseView.SalesOrdersView.class, BaseView.RouteOrderView.class, BaseView.AvcoView.class,
            BaseView.DetailedView.class, BaseView.TaxCodeView.class, BaseView.ProductStockView.class,
            BaseView.SupplierCustomerView.class, BaseView.SupplierTargetView.class,
            BaseView.ProfileFewDetails.class, BaseView.CustomerFewDetailView.class,BaseView.TumaSmsView.class,
            BaseView.CreditNoteSummaryView.class, BaseView.UserViewDetails.class, BaseView.TumaSmsView.class,
            BaseView.ReminderSettingView.class, BaseView.FaqView.class, BaseView.LocationApprovalView.class,
            BaseView.WorkflowView.class, BaseView.CallStatusView.class, BaseView.SalesAgentsMinifiedView.class,
            BaseView.MiniProdView.class,
            BaseView.DeliveryAgentsMinifiedView.class, BaseView.SalesOrderMiniView.class, BaseView.ProductBrandView.class,
            BaseView.WhatsAppView.class,BaseView.TraSwitchView.class, BaseView.CustomerReturnView.class, BaseView.SalesOrderItemsView.class
    })
    @Column(name = "public_id", updatable = false)
    private UUID publicId;

    @JsonView({BaseView.class, BaseView.DebitNotesView.class, BaseView.ReturnView.class, BaseView.DiscountApprovalView.class,
            BaseView.DiscountView.class, BaseView.RegionMappingView.class, BaseView.OrderAmendmentView.class, BaseView.ReconciliationSummaryView.class,
            BaseView.PriceManagementSummaryView.class, BaseView.SalesOrdersView.class, BaseView.PackingView.class,
            BaseView.RouteView.class, BaseView.VehicleView.class, BaseView.GoodsReceived.class, BaseView.RemittanceSummaryView.class,
            BaseView.InventoryView.class, BaseView.GoodsReturned.class, BaseView.FileDataView.class, BaseView.purchaseOrderSummaryView.class,
            BaseView.WarehouseDetailView.class, BaseView.SupplierSummaryView.class, BaseView.ProductSummaryView.class,
            BaseView.ProductDetailView.class, BaseView.DeliveryAgentsView.class, BaseView.SalesAgentsView.class,
            BaseView.DetailedView.class, BaseView.TaxCodeView.class, BaseView.ProductStockView.class, BaseView.TumaSmsView.class,
            BaseView.SupplierTargetView.class, BaseView.UserViewDetails.class,
            BaseView.LocationApprovalView.class,BaseView.SalesOrderMiniView.class, BaseView.WhatsAppView.class,
            BaseView.TumaSmsView.class, BaseView.FaqView.class, BaseView.ProductBrandView.class,
            BaseView.TraSwitchView.class, BaseView.CustomerReturnView.class})
    @CreatedDate
    @Column(name = "creation_time", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime creationTime;

    @LastModifiedDate
    @JsonView({BaseView.class, BaseView.DebitNotesView.class, BaseView.RegionMappingView.class, BaseView.ProductDetailView.class, BaseView.DetailedView.class, BaseView.purchaseOrderSummaryView.class})
    @Column(name = "update_date", insertable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime updateDate;

    @JsonIgnore
    @CreatedBy
    @JsonView({BaseView.class, BaseView.ReconciliationSummaryView.class, BaseView.DetailedView.class, BaseView.DiscountView.class, BaseView.RegionMappingView.class, BaseView.DebitNotesView.class, BaseView.PriceManagementSummaryView.class,
            BaseView.PackingView.class, BaseView.RouteView.class, BaseView.VehicleView.class, BaseView.GoodsReceived.class,
            BaseView.CustomersView.class, BaseView.RemittanceSummaryView.class, BaseView.GoodsReturned.class, BaseView.FileDataView.class,
            BaseView.purchaseOrderSummaryView.class, BaseView.WarehouseDetailView.class, BaseView.ProductSummaryView.class, BaseView.SupplierSummaryView.class, BaseView.ProductDetailView.class, BaseView.SummaryView.class, BaseView.RoleView.class, BaseView.DeliveryAgentsView.class,
            BaseView.SalesAgentsView.class, BaseView.OrganizationCountriesView.class, BaseView.DiscountApprovalView.class, BaseView.OrganizationCitiesView.class, BaseView.OrganizationRegionsView.class,
            BaseView.OrganizationLocationsView.class, BaseView.TaxCodeView.class,BaseView.TraSwitchView.class})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    private Person createdBy;

    @JsonIgnore
    @LastModifiedBy
    @JsonView({BaseView.class, BaseView.RouteView.class, BaseView.DebitNotesView.class,
            BaseView.ProductDetailView.class, BaseView.TaxCodeView.class})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", referencedColumnName = "id")
    private Person updatedBy;

    @JsonIgnore
    @JsonView(BaseView.CustomersView.class)
    @Column(name = "deleted")
    private Boolean deleted = Boolean.FALSE;

    @PrePersist
    public void prePersist() {
        LocalDateTime nairobiDateTime = currentNairobiDateTime();

        this.publicId = UUID.randomUUID();
        this.creationTime = nairobiDateTime;
        this.deleted = Boolean.FALSE;
    }

    @PreUpdate
    public void preUpdate() {
        this.updateDate = currentNairobiDateTime();
    }

    private LocalDateTime currentNairobiDateTime() {
        return LocalDateTime.ofInstant(Instant.now(), NAIROBI_ZONE);
    }
}
