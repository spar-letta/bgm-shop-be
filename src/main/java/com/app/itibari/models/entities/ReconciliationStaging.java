package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.ReconciliationStageStatus;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "reconciliation_stagings", schema = "itibari")
public class ReconciliationStaging extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "product_name")
    @JsonView(BaseView.ReturnView.class)
    private String deliveryAgentName;

    @Column(name = "status")
    @JsonView(BaseView.ReturnView.class)
    private ReconciliationStageStatus status;

    @OneToMany(mappedBy = "reconciliationStaging")
    @JsonView(BaseView.ReturnView.class)
    private List<ReconciliationStagingItem> reconciliationStagingItemList = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "product_recon_id", referencedColumnName = "id")
    private ProductRecon productRecon;

    @Column(name = "comment")
    private String comment;
}
