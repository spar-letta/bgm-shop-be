package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.AssignToEnum;
import com.app.itibari.models.request.TicketDto;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.util.*;

@Getter
@Setter
@Entity
@Table(name = "customer_experiences", schema = "itibari")
public class CustomerExperience extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonView({BaseView.WorkflowView.class})
    @Column(name = "template_name")
    private String templateName;

    @JsonView(BaseView.WorkflowView.class)
    @ManyToOne
    @JoinColumn(name = "issue_type_id_fk", referencedColumnName = "id")
    private IssueType issueType;

    @ManyToMany
    @JoinTable(name = "cx_department_in_copy", schema = "itibari",
            joinColumns = @JoinColumn(name = "cx_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "department_id", referencedColumnName = "id"))
    private List<Department> departmentInCopy = new ArrayList<>();

    @JsonView(BaseView.WorkflowView.class)
    @Column(name = "assign_to")
    @Enumerated(EnumType.STRING)
    private AssignToEnum assignTo;

    @JsonView(BaseView.WorkflowView.class)
    @Column(name = "poc_email")
    private String pocEmail;

    @JsonView(BaseView.WorkflowView.class)
    @Column(name = "priority_name")
    private String priorityName;

    @JsonView(BaseView.WorkflowView.class)
    @Column(name = "color_name")
    private String colorName;

    @Column(name = "sla_name")
    private String slaName;

    @Column(name = "sla_duration")
    private String slaDuration;

    @JsonView(BaseView.WorkflowView.class)
    @Type(JsonType.class)
    @Column(name = "extra_data", columnDefinition = "json")
    private JsonNode customFields;

    @JsonView(BaseView.WorkflowView.class)
    @Column(name = "is_active")
    private Boolean isActive;

    @JsonView(BaseView.WorkflowView.class)
    @Column(name = "email_in_copy")
    private String emailInCopy;

    @JsonView(BaseView.WorkflowView.class)
    @Column(name = "allow_notification")
    private boolean allowNotification;

    @JsonIgnore
    public Map<String, String> getEmailInCopy() {
        if (emailInCopy != null && !emailInCopy.isBlank()) {
            Map<String, String> mailLists = new LinkedHashMap<>();

            List<String> list = Arrays.stream(emailInCopy.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();

            for (int i = 0; i < list.size(); i++) {
                mailLists.put("email_" + i, list.get(i));
            }
            return mailLists;
        } else {
            return Collections.emptyMap();
        }
    }

    public List<String> getEmailInCopyAsList() {
        if (emailInCopy != null && !emailInCopy.isBlank()) {
            Map<String, String> mailLists = new LinkedHashMap<>();

            return Arrays.stream(emailInCopy.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();

        } else {
            return Collections.emptyList();
        }
    }

}
