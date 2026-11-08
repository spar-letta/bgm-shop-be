package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "events", schema = "itibari")
public class Events {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.EventsView.class})
    @Column(name = "reference_id")
    private String referenceId;

    @JsonView({BaseView.EventsView.class})
    @Column(name = "event_type")
    private String eventType;

    @JsonView({BaseView.EventsView.class})
    @Column(name = "event_date_time")
    private LocalDateTime eventDateTime;

    @JsonView({BaseView.EventsView.class})
    @Column(name = "event_description")
    private String eventDescription;

    @JsonView({BaseView.EventsView.class})
    @Column(name = "event_action")
    private String eventAction;

    @JsonView({BaseView.EventsView.class})
    @ManyToOne
    @JoinColumn(name = "created_by_fk")
    private Person createdBy;

    public Events(String referenceId,
                  String eventType,
                  LocalDateTime eventDateTime,
                  String eventDescription,
                  String eventAction,
                  Person createdBy) {

        this.referenceId = referenceId;
        this.eventType = eventType;
        this.eventDateTime = eventDateTime;
        this.eventDescription = eventDescription;
        this.eventAction = eventAction;
        this.createdBy = createdBy;
    }
}
