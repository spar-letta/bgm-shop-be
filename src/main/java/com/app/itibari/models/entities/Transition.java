package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "transitions", schema = "itibari")
public class Transition extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonView(BaseView.WorkflowView.class)
    @Column(name = "name")
    private String name;

    @Deprecated
    @Column(name = "from_status")
    private String from;

    @Deprecated
    @Column(name = "to_status")
    private String to;

    @JsonView(BaseView.WorkflowView.class)
    @Column(name = "duration_number")
    private String durationNumber;

    @JsonView(BaseView.WorkflowView.class)
    @ManyToOne
    @JoinColumn(name = "status_from_id")
    private WorkflowStatus statusFrom;

    @JsonView(BaseView.WorkflowView.class)
    @ManyToOne
    @JoinColumn(name = "status_to_id")
    private WorkflowStatus statusTo;

    @Column(name = "transition_index")
    @JsonView(BaseView.WorkflowView.class)
    private Integer transitionIndex;

    @Column(name = "ticket_arrival_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime ticketArrivalTime;
}