package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "sms_group_entities", schema = "itibari")
public class SmsGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.TumaSmsView.class)
    @Column(name = "name")
    private String name;

    @JsonView(BaseView.TumaSmsView.class)
    @Column(name = "phone")
    private String phone;

    @ManyToOne
    @JoinColumn(name = "sms_group_fk_id")
    private SmsGroup smsGroup;

}
