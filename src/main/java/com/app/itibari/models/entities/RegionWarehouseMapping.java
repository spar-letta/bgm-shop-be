package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "region_warehouse_mappings", schema = "itibari")
public class RegionWarehouseMapping extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.RegionMappingView.class)
    @Where(clause = "deleted = false")
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "regionWarehouseMapping", cascade = CascadeType.ALL)
    private List<OrganizationRegions> mappedRegions = new ArrayList<>();

    @JsonView(BaseView.RegionMappingView.class)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", referencedColumnName = "id")
    private Warehouse warehouse;

    // add region

    public void addRegion(OrganizationRegions region) {

        Optional<OrganizationRegions> existingRegion = mappedRegions.stream()
                .filter(r -> r.getPublicId().equals(region.getPublicId())).findFirst();
        if (existingRegion.isEmpty()) {
            mappedRegions.add(region);
            region.setRegionWarehouseMapping(this);
        }
    }

    // remove region

    public void removeRegion(OrganizationRegions region) {

        mappedRegions.removeIf(r -> r.getPublicId().equals(region.getPublicId()));
        region.setRegionWarehouseMapping(null);
    }
}
