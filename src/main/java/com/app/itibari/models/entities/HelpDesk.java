package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.*;
import com.app.itibari.dataTypes.Module;
import com.app.itibari.dataTypes.WorkflowStatus;
import com.app.itibari.models.request.TicketDto;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "help_desks", schema = "itibari")
public class HelpDesk extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "content")
    private String content;

    @Column(name = "ticket_priority")
    @Enumerated(EnumType.STRING)
    private TicketPriority ticketPriority;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private HelpDeskStatus status;

    @Column(name = "ticket_number")
    private String ticketNumber;

    @ManyToMany
    @JoinTable(name = "ticket_assigned_user", schema = "itibari",
            joinColumns = {@JoinColumn(name = "fk_ticket_id", nullable = false)},
            inverseJoinColumns = {@JoinColumn(name = "fk_user_id", nullable = false)},
            uniqueConstraints = @UniqueConstraint(columnNames = {"fk_ticket_id", "fk_user_id"}))
    @Setter(AccessLevel.NONE)
    private List<Person> assignedUser = new ArrayList<>();

    @Column(name = "resolved_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime resolvedDate;

    @ManyToOne
    @JoinColumn(name = "issue_type_id_fk", referencedColumnName = "id")
    private IssueType issueType;

    @ManyToOne
    @JoinColumn(name = "section_id_fk", referencedColumnName = "id")
    private Department section;

    @Column(name = "module")
    @Enumerated(EnumType.STRING)
    private Module module;

    @ManyToOne
    @JoinColumn(name = "assigned_to_fk_id", referencedColumnName = "id")
    private Person assignedTo;

    @Column(name = "read")
    private Boolean read;

    @Column(name = "resolution_status")
    private String resolutionStatus;

    @ManyToOne
    @JoinColumn(name = "help_desk_customer_fk", referencedColumnName = "id")
    private HelpDeskCustomer helpDeskCustomer;

    @Column(name = "assigned_to_department")
    private Boolean assignedToDepartment;

    @Column(name = "assigned_to_poc")
    private Boolean assignedToPoc;

    @Column(name = "assign_to_dep_or_poc")
    @Enumerated(EnumType.STRING)
    private AssignToEnum assignToDepOrPoc;

    @Column(name = "poc_email")
    private String pocEmail;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "template_id_fk")
    private CustomerExperience template;

    @ElementCollection
    @CollectionTable(
            name = "helpdesk_email_in_copy",
            schema = "itibari",
            joinColumns = @JoinColumn(name = "help_desk_id"))
    @Column(name = "email")
    private List<String> emailInCopy = new ArrayList<>();

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "amount_paid")
    private BigDecimal amountPaid;

    @Column(name = "payment_mode")
    private String paymentMode;

    @Column(name = "summary")
    private String summary;

    @Type(JsonType.class)
    @Column(name = "custom_field", columnDefinition = "json")
    private JsonNode customField;

    @ManyToMany
    @JoinTable(name = "ticket_products", schema = "itibari",
            joinColumns = {@JoinColumn(name = "fk_ticket_product_id", nullable = false)},
            inverseJoinColumns = {@JoinColumn(name = "fk_product_id", nullable = false)},
            uniqueConstraints = @UniqueConstraint(columnNames = {"fk_ticket_product_id", "fk_product_id"}))
    @Setter(AccessLevel.NONE)
    private List<Product> productList = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "ticket_customers", schema = "itibari",
            joinColumns = {@JoinColumn(name = "fk_ticket_customers_id", nullable = false)},
            inverseJoinColumns = {@JoinColumn(name = "fk_customers_id", nullable = false)},
            uniqueConstraints = @UniqueConstraint(columnNames = {"fk_ticket_customers_id", "fk_customers_id"}))
    @Setter(AccessLevel.NONE)
    private List<Customers> customersList = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "workflow_id_fk")
    private Workflow workflow;

    @Column(name = "workflow_status")
    private String workflowStatus;

    @Column(name = "workflow_new_status")
    private String workflowNewStatus = WorkflowStatus.NEW.name();

    @Column(name = "template_public_id")
    private UUID templatePublicId;

    @JsonView(BaseView.WorkflowView.class)
    @ManyToOne
    @JoinColumn(name = "delivery_agent_id_fk", referencedColumnName = "id")
    private DeliveryAgents deliveryAgents;

    @JsonView(BaseView.WorkflowView.class)
    @ManyToOne
    @JoinColumn(name = "sales_agent_id_fk", referencedColumnName = "id")
    private SalesAgents salesAgents;
}
