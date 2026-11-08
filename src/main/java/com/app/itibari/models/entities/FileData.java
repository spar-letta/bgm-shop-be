package com.app.itibari.models.entities;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "file_data", schema = "itibari")
public class FileData extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.FileDataView.class})
    @Column(name = "file_name")
    private String fileName;

    @JsonView({BaseView.FileDataView.class})
    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @JsonView({BaseView.FileDataView.class})
    @Column(name = "file_path")
    private String filePath;

    @JsonView({BaseView.FileDataView.class})
    @Column(name = "reference_id")
    private String referenceId;

    @JsonView({BaseView.FileDataView.class})
    @Column(name = "reference_type")
    private String referenceType;

    @JsonView({BaseView.FileDataView.class})
    @Column(name = "description")
    private String description;

    @JsonView({BaseView.FileDataView.class})
    @Column(name = "receipt")
    private String receipt;

    @JsonView({BaseView.FileDataView.class})
    @Column(name = "amount")
    private BigDecimal amount = BigDecimal.ZERO;

    @JsonView({BaseView.FileDataView.class})
    @ManyToOne
    @JoinColumn(name = "document_rule_id", referencedColumnName = "id")
    private DocumentRule documentRule;

    @Column(name = "expiry")
    @JsonView({BaseView.FileDataView.class})
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate expiry;

    @JsonView({BaseView.FileDataView.class})
    @Column(name = "approved")
    private boolean approved = false;

    @JsonView({BaseView.FileDataView.class})
    @Column(name = "rejection_reason")
    private String rejectionReason;

    @JsonView({BaseView.FileDataView.class})
    @Column(name = "supplier_document_info")
    private String supplierDocumentInfo;

    @JsonView({BaseView.FileDataView.class})
    @Column(name = "active_file")
    private boolean activeFile = false;

    @JsonView({BaseView.FileDataView.class})
    @Column(name = "processed")
    private boolean processed = false;

    @JsonView({BaseView.FileDataView.class})
    @Column(name = "fifteen_day_processed")
    private boolean fifteenDayProcessed = false;

    @JsonView({BaseView.FileDataView.class})
    @Column(name = "thirty_day_processed")
    private boolean thirtyDayProcessed = false;

    @Column(name = "category_public_id")
    private String categoryPublicId;

    @Column(name = "class_public_id")
    private String classPublicId;

    @Column(name = "sub_class_public_id")
    private String subClassPublicId;

    @Column(name = "product_ids")
    private String productPublicIds;

    @Column(name = "brand")
    private String brand;

    @Transient
    @JsonView({BaseView.FileDataView.class})
    private boolean expired;

    @Transient
    @JsonView({BaseView.FileDataView.class})
    private String status;

    @Transient
    private String expiresIn;


    public boolean isExpired() {
        if (this.expiry == null) {
            return false;
        }
        return LocalDate.now().isAfter(this.expiry);
    }


    public String getExpiresIn() {
        if (this.expiry == null) {
            return "N/A";
        }
        LocalDate now = LocalDate.now();
        if (isExpired()) {
            return "Expired";
        }
        long daysBetween = ChronoUnit.DAYS.between(now, this.expiry);
        return daysBetween + " days";
    }

    public String getStatus() {
        if (this.rejectionReason != null) {
            return "Rejected";
        } else if (!this.approved) {
            return "Pending";
        } else
            return "Approved";
    }
}
