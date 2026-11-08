package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "reminder_logs", schema = "itibari")
@NoArgsConstructor
@AllArgsConstructor
public class ReminderLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reminder_job_fk", referencedColumnName = "id")
    private ReminderJob reminderJob;

    @JsonView(BaseView.ReminderLogView.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_fk", referencedColumnName = "id")
    private Customers customer;

    @JsonView(BaseView.ReminderLogView.class)
    @Column(name = "order_number")
    private String orderNumber;

    @JsonView(BaseView.ReminderLogView.class)
    @Column(name = "sent", nullable = false)
    private boolean sent;

    @JsonView(BaseView.ReminderLogView.class)
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @JsonView(BaseView.ReminderLogView.class)
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
}
