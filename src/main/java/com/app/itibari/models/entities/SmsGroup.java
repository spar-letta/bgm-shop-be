package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "sms_groups", schema = "itibari")
public class SmsGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.TumaSmsView.class)
    @Column(name = "name")
    private String name;

    @JsonView(BaseView.TumaSmsView.class)
    @Column(name = "is_active")
    private Boolean isActive;

    @JsonView(BaseView.TumaSmsView.class)
    @OneToMany(mappedBy = "smsGroup")
    private List<SmsGroupEntity> smsGroupEntityList = new ArrayList<>();

    @JsonView(BaseView.TumaSmsView.class)
    @Type(JsonType.class)
    @Column(name = "error_list", columnDefinition = "json")
    private JsonNode errorList;

    @JsonView(BaseView.TumaSmsView.class)
    @Column(name = "default_group")
    private Boolean defaultGroup;

    @JsonView(BaseView.TumaSmsView.class)
    @Column(name = "default_name")
    private String defaultName;
}
