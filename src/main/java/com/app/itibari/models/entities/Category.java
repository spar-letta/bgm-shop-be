package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "category", schema = "itibari")
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.ProductSummaryView.class, BaseView.ProductDetailView.class, BaseView.SalesOrdersView.class, BaseView.purchaseOrderSummaryView.class})
    @Column(name = "name")
    private String name;

    @JsonView({BaseView.ProductSummaryView.class, BaseView.SalesOrdersView.class, BaseView.purchaseOrderSummaryView.class})
    @Column(name = "description")
    private String description;

    /**
     * Browser-ready category thumbnail URL (signed GCS / local placeholder).
     * Mirrors POS inventory {@code imageUrl}.
     */
    @Transient
    @JsonView({BaseView.ProductSummaryView.class, BaseView.ProductDetailView.class})
    private String imageUrl;

    /** Alias retained for clients that still read {@code productImage}. */
    @Transient
    @JsonView({BaseView.ProductSummaryView.class, BaseView.ProductDetailView.class})
    private String productImage;

    /**
     * {@link BaseEntity#getCreatedBy()} is {@code @JsonIgnore}; expose a slim view for category list/detail.
     */
    @JsonProperty("createdBy")
    @JsonView({BaseView.ProductSummaryView.class, BaseView.ProductDetailView.class})
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
}
