package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "sales_order_agents", schema = "itibari")
public class SalesOrderAgent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.SalesOrderItemsView.class})
    @ManyToOne
    @JoinColumn(name = "sales_agent_id")
    private SalesAgents salesAgents;

    @OneToMany(mappedBy = "salesOrderAgent",  fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<SalesAgentDeliveredItem> deliveredItem = new ArrayList<>();
}
