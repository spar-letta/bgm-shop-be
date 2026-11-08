package com.app.itibari.models.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
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

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "ticket_histories", schema = "itibari")
public class TicketHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "actioned_by")
    private String actionedBy;

    @Column(name = "from_department")
    private String fromDepartment;

    @Column(name = "to_department")
    private String toDepartment;

    @Column(name = "status_from")
    private String statusFrom;

    @Column(name = "status_to")
    private String statusTo;

    @Column(name = "reason")
    private String reason;

    @Column(name = "actioned_time", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime actionedTime;

    @ManyToOne
    @JoinColumn(name = "help_desk_id", referencedColumnName = "id")
    private HelpDesk helpDesk;
}
