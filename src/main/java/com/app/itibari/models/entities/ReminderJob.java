package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.NotificationChannel;
import com.app.itibari.dataTypes.ReminderJobStatus;
import com.app.itibari.dataTypes.ReminderJobType;
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
@Table(name = "reminder_jobs", schema = "itibari")
@NoArgsConstructor
@AllArgsConstructor
public class ReminderJob extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.ReminderJobView.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false)
    private ReminderJobType jobType;

    @JsonView(BaseView.ReminderJobView.class)
    @Column(name = "scheduled_time", nullable = false)
    private LocalDateTime scheduledTime;

    @JsonView(BaseView.ReminderJobView.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReminderJobStatus status = ReminderJobStatus.SCHEDULED;

    @JsonView(BaseView.ReminderJobView.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_channel", nullable = false)
    private NotificationChannel notificationChannel;

    @JsonView(BaseView.ReminderJobView.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reminder_setting_fk", referencedColumnName = "id")
    private ReminderSetting reminderSetting;

    @JsonView(BaseView.ReminderJobView.class)
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
}