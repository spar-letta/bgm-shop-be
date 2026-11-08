package com.app.itibari.models.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "help_desk_notifications", schema = "itibari")
@Where(clause = "deleted = false")
public class HelpDeskNotification extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "assigned_to_public_id")
    private UUID assignedToPublicId;

    @Column(name = "message_action")
    private String messageAction;

    @Column(name = "read")
    private Boolean read = Boolean.FALSE;
}
