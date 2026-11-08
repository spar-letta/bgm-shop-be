package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "call_status", schema = "itibari")
public class CallStatus extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.CallStatusView.class, BaseView.CustomersView.class, BaseView.OrderAmendmentView.class})
    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @JsonView({BaseView.CallStatusView.class, BaseView.CustomersView.class, BaseView.OrderAmendmentView.class})
    @Column(name = "description", nullable = false)
    private String description;

    @JsonView({BaseView.CallStatusView.class, BaseView.CustomersView.class})
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;
}
