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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "volume_range", schema = "itibari")
public class VolumeRange extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.DiscountApprovalView.class)
    @Column(name = "min_volume")
    private Integer minVolume;

    @JsonView(BaseView.DiscountApprovalView.class)
    @Column(name = "max_volume")
    private Integer maxVolume;

    @JsonView(BaseView.DiscountApprovalView.class)
    @Column(name = "discounted_amount")
    private BigDecimal discountedAmount = BigDecimal.ZERO;

    @JsonView(BaseView.DiscountApprovalView.class)
    @Column(name = "discount_public_id")
    private String discountPublicId;

    /**
     * {@link BaseEntity#getCreatedBy()} is {@code @JsonIgnore}; expose a slim view for volume range rows.
     */
    @JsonProperty("createdBy")
    @JsonView(BaseView.DiscountApprovalView.class)
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
