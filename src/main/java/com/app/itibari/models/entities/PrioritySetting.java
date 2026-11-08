package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.SlaType;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "priority_settings", schema = "itibari")
public class PrioritySetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "priority_name")
    private String priorityName;

    @Enumerated(EnumType.STRING)
    @Column(name = "sla_type")
    private SlaType slaType;

    @Column(name = "sla_duration")
    private String slaDuration;

    @Column(name = "color_name")
    private String colorName;

    @JsonView(BaseView.TumaSmsView.class)
    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "first_time_response")
    private Integer firstTimeResponse;
}
