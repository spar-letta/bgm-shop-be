package com.app.itibari.models.entities;

import com.app.itibari.models.request.TicketDto;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "issue_types", schema = "itibari")
@Where(clause = "deleted = false")
public class IssueType extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.TumaSmsView.class, BaseView.FaqView.class, BaseView.WorkflowView.class})
    @Column(name = "name")
    private String name;

    @JsonView({BaseView.TumaSmsView.class, BaseView.FaqView.class, BaseView.WorkflowView.class})
    @ManyToOne
    @JoinColumn(name = "department_id_fk", referencedColumnName = "id")
    private Department department;

    @JsonView({BaseView.TumaSmsView.class, BaseView.FaqView.class, BaseView.WorkflowView.class})
    @ManyToOne
    @JoinColumn(name = "priority_setting_id_fk", referencedColumnName = "id")
    private PrioritySetting prioritySetting;

    @JsonView({BaseView.TumaSmsView.class, BaseView.WorkflowView.class})
    @Column(name = "is_active")
    private Boolean isActive;

    @JsonView(BaseView.WorkflowView.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type")
    private TicketDto.CustomerType customerType;
}
