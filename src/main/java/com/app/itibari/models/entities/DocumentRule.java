package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.DocumentTypes;
import com.app.itibari.dataTypes.EntityType;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "document_rules", schema = "itibari")
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    @Column(name = "id")
    private Long id;

    @Column(name = "document_name")
    @JsonView({BaseView.DetailedView.class,BaseView.FileDataView.class})
    private String documentName;

    @Column(name = "entity_type")
    @Enumerated(EnumType.STRING)
    @JsonView({BaseView.DetailedView.class})
    private EntityType entityType;

    @Column(name = "entity_sub_type")
    @JsonView({BaseView.DetailedView.class})
    private String entitySubType;

    @Column(name = "required")
    @JsonView({BaseView.DetailedView.class,BaseView.FileDataView.class})
    private boolean documentRequired;

    @Column(name = "expiry_check")
    @JsonView({BaseView.DetailedView.class,BaseView.FileDataView.class})
    private boolean documentExpiryCheck;

    @Column(name = "description")
    @JsonView({BaseView.DetailedView.class,BaseView.FileDataView.class})
    private String documentDescription;

    @Column(name = "document_type")
    @Enumerated(EnumType.STRING)
    @JsonView({BaseView.DetailedView.class})
    private DocumentTypes documentType;
}
