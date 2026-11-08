package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "customer_feedback", schema = "itibari")
public class CustomerFeedBack extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.DetailedView.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "id")
    private Customers customer;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "feedback_text")
    private String feedbackText;

    @JsonView(BaseView.DetailedView.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejection_code_id", referencedColumnName = "id")
    private RejectionsCode rejectionCode;
}
