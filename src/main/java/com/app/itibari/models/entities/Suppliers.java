package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.SupplierStatus;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "suppliers", schema = "itibari")
public class Suppliers extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.SupplierSummaryView.class,BaseView.CreditNoteSummaryView.class, BaseView.RemittanceSummaryView.class, BaseView.GoodsReceived.class, BaseView.GoodsReturned.class, BaseView.ProductDetailView.class, BaseView.purchaseOrderSummaryView.class})
    @Column(name = "name")
    private String supplierName;

    @JsonView(BaseView.SupplierSummaryView.class)
    @Column(name = "address")
    private String supplierAddress;

    @JsonView(BaseView.SupplierSummaryView.class)
    @Column(name = "point_of_contact")
    private String supplierPointOfContact;

    @JsonView(BaseView.SupplierSummaryView.class)
    @Column(name = "phone_number")
    private String phoneNumber;

    @JsonView(BaseView.SupplierSummaryView.class)
    @Column(name = "email")
    private String email;

    @JsonView(BaseView.SupplierSummaryView.class)
    @Column(name = "lead_days")
    private Integer leadDays;

    @JsonView(BaseView.SupplierSummaryView.class)
    @Column(name = "credit_terms")
    private Integer creditTerms;

    @JsonView(BaseView.SupplierSummaryView.class)
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private SupplierStatus status;

    @JsonView(BaseView.SupplierSummaryView.class)
    @Column(name = "tin_number")
    private String tinNumber;

    @JsonView(BaseView.SupplierSummaryView.class)
    @Column(name = "credit_limit")
    private BigDecimal creditLimit;

    @JsonView(BaseView.SupplierSummaryView.class)
    @Column(name = "postal_address")
    private String postalAddress;

    @JsonView(BaseView.SupplierSummaryView.class)
    @Column(name = "bank_name")
    private String BankName;

    @JsonView(BaseView.SupplierSummaryView.class)
    @Column(name = "bank_account_name")
    private String BankAccountName;

    @JsonView(BaseView.SupplierSummaryView.class)
    @Column(name = "bank_account_number")
    private String BankAccountNumber;

    @JsonView(BaseView.SupplierSummaryView.class)
    @Column(name = "portal_access_granted")
    private Boolean portalAccessGranted = false;
}
