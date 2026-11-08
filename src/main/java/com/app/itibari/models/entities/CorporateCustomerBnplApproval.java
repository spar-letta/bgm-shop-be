package com.app.itibari.models.entities;


import com.app.itibari.dataTypes.CorporateApprovalStatus;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "corporate_customer_bnpl_approval", schema = "itibari")
public class CorporateCustomerBnplApproval extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.DetailedView.class)
    @ManyToOne
    @JoinColumn(name = "customer_id", referencedColumnName = "id")
    private Customers customer;

    @JsonView(BaseView.DetailedView.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CorporateApprovalStatus approvalStatus;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "actioned_by")
    private String actionedBy;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "actioned_on")
    private LocalDate actionedOn;

    @Column(name = "change_to_regular")
    private boolean changeToRegular;
}
