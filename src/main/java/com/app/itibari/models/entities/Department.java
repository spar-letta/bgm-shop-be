package com.app.itibari.models.entities;

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
@Table(name = "departments", schema = "itibari")
@Where(clause = "deleted = false")
public class Department extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    @JsonView({BaseView.UserView.class, BaseView.Profile.class, BaseView.WorkflowView.class,
            BaseView.UserViewDetails.class, BaseView.TumaSmsView.class, BaseView.FaqView.class, BaseView.RejectionCodeView.class})
    private String name;
}
