package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "workflow_statuses", schema = "itibari")
public class WorkflowStatus extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonView(BaseView.WorkflowView.class)
    @Column(name = "name")
    private String name;

    @JsonView(BaseView.WorkflowView.class)
    @ManyToOne
    @JoinColumn(name = "template_id_fk")
    private CustomerExperience template;

    @JsonView(BaseView.WorkflowView.class)
    @ManyToOne
    @JoinColumn(name = "department_id_fk")
    private Department department;
}
