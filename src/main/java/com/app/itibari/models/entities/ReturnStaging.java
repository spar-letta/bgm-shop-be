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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
@Table(name = "return_staging", schema = "itibari")
public class ReturnStaging extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.ReturnView.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_fk", referencedColumnName = "id")
    private DeliveryAgents deliveryAgents;

    @JsonView(BaseView.ReturnView.class)
    @Column(name = "delivery_id")
    private String deliveryId;

    @JsonView(BaseView.ReturnView.class)
    @Where(clause = "deleted = false")
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "returnStaging", cascade = CascadeType.ALL)
    private List<ReturnItem> returnItems = new ArrayList<>();

    @JsonView(BaseView.ReturnView.class)
    @Column(name = "is_approved")
    private boolean approved = false;

    @Column(name = "warehouse_id")
    private String warehouseId;

    public void addItem(ReturnItem returnItem1) {

        Optional<ReturnItem> exists = returnItems.stream().filter(item -> item.getId().equals(returnItem1.getId())).findFirst();

        if (exists.isEmpty()) {
            returnItems.add(returnItem1);
        }
    }
}
