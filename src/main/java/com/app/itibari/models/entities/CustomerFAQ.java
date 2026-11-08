package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "customer_faqs", schema = "itibari")
public class CustomerFAQ extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "department_fk_id")
    @JsonView(BaseView.FaqView.class)
    private Department department;

    @JsonView(BaseView.FaqView.class)
    @OneToMany(mappedBy = "customerFAQ")
    private List<CustomerFAQNote> noteList = new ArrayList<>();
}
