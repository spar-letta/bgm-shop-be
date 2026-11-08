package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "customer_faqs_notes", schema = "itibari")
public class CustomerFAQNote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.FaqView.class)
    @Column(name = "notes")
    private String question;

    @JsonView(BaseView.FaqView.class)
    @Column(name = "answer")
    private String answer;

    @JsonView(BaseView.FaqView.class)
    @ManyToOne
    @JoinColumn(name = "issue_type_fk_id")
    private IssueType issueType;

    @ManyToOne
    @JoinColumn(name = "customer_faq_fk_id")
    private CustomerFAQ customerFAQ;
}
