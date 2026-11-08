package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.WorkflowStatus;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "workflows", schema = "itibari")
public class Workflow extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonView(BaseView.WorkflowView.class)
    @Column(name = "name")
    private String name;

    @ElementCollection
    @CollectionTable(
            name = "workflow_status_table", schema = "itibari",
            joinColumns = @JoinColumn(name = "workflow_status_id"))
    @Column(name = "statuses")
    private List<WorkflowStatus> statuses;

    @JsonView(BaseView.WorkflowView.class)
    @ManyToMany
    @JoinTable(name = "transition_workflows", schema = "itibari",
            joinColumns = {@JoinColumn(name = "fk_transition_id", nullable = false)},
            inverseJoinColumns = {@JoinColumn(name = "fk_workflow_id", nullable = false)},
            uniqueConstraints = @UniqueConstraint(columnNames = {"fk_transition_id", "fk_workflow_id"}))
    @Setter(AccessLevel.NONE)
    private List<Transition> transitions = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "template_id_fk")
    @JsonView(BaseView.WorkflowView.class)
    private CustomerExperience template;

    @JsonView(BaseView.WorkflowView.class)
    @ManyToOne
    @JoinColumn(name = "department_fk_id")
    private Department assignedDepartment;

    @JsonView(BaseView.WorkflowView.class)
    @Transient
    public String issueType;

    public String getIssueType(){
        if (template != null && template.getIssueType() != null){
            return template.getIssueType().getName();
        }
        return "--";
    }
}

