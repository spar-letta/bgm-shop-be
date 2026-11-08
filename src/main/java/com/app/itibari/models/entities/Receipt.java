package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.ReceiptPaymentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "receipts", schema = "itibari")
public class Receipt extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "sales_order_public_id")
    private String salesOrderPublicId;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "rctv_num")
    private String rctvNum;

    @Column(name = "rctv_code")
    private String rctvCode;

    @Column(name = "z_number")
    private String zNumber;

    @Column(name = "vfd_invoice_num")
    private String vfdInvoiceNum;

    @Column(name = "invoice_date")
    private String iDate;

    @Column(name = "invoice_time")
    private String iTime;

    @Column(name = "customer_invoice_number")
    private String custInvoiceNo;

    @Column(name = "customer_id_type")
    private String custIdType;

    @Column(name = "customer_id")
    private String custId;

    @Column(name = "mobile_number")
    private String mobileNum;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "pay_type")
    private String payType;

    @Column(name = "status")
    private String status;

    @Column(name = "qr_path")
    private String qrPath;

    @Column(name = "qr_code_uri")
    private String qrCodeUri;

    @Column(name = "customer_address")
    private String customerAddress;

    @Column(name = "customer_land_mark")
    private String customerLandMark;

    @Column(name = "payment_type")
    @Enumerated(EnumType.STRING)
    private ReceiptPaymentType paymentType;

    @Where(clause = "deleted = false")
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "receipt", cascade = CascadeType.ALL)
    private List<ReceiptItem> receiptItems = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_item_id", referencedColumnName = "id")
    private CreditLimitItem creditLimitItem;


}
