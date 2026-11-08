package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.Module;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "rejections_code", schema = "itibari")
public class RejectionsCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.RejectionCodeView.class,BaseView.DetailedView.class,
            BaseView.OrderAmendmentView.class, BaseView.SalesOrdersView.class, BaseView.QuarantineView.class,
            BaseView.purchaseOrderSummaryView.class, BaseView.CustomerReturnView.class})
    @Column(name = "code")
    private String code;

    @JsonView({BaseView.RejectionCodeView.class,BaseView.DetailedView.class, BaseView.OrderAmendmentView.class,
            BaseView.GoodsReturned.class, BaseView.SalesOrdersView.class,
            BaseView.QuarantineView.class, BaseView.purchaseOrderSummaryView.class, BaseView.CustomerReturnView.class})
    @Column(name = "description")
    private String description;

    @JsonView({BaseView.RejectionCodeView.class})
    @Column(name = "module")
    @Enumerated(EnumType.STRING)
    private Module module;

    @JsonView({BaseView.RejectionCodeView.class})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id",referencedColumnName = "id")
    private Department department;
}
