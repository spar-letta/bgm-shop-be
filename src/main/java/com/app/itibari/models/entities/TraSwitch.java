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
@Table(name = "tra_switch", schema = "itibari")
public class TraSwitch  extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.TraSwitchView.class)
    @Column(name = "is_switch_on")
    private boolean switchOn = true;
}
