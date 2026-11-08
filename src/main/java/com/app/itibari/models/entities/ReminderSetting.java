package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.CustomerType;
import com.app.itibari.dataTypes.ReminderFrequency;
import com.app.itibari.dataTypes.ReminderType;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "reminder_settings", schema = "itibari")
@NoArgsConstructor
@AllArgsConstructor
public class ReminderSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.ReminderSettingView.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false)
    private CustomerType customerType;

    @JsonView(BaseView.ReminderSettingView.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false)
    private ReminderType reminderType;

    @JsonView(BaseView.ReminderSettingView.class)
    @Column(name = "period", nullable = false)
    private Integer period;

    @JsonView(BaseView.ReminderSettingView.class)
    @Column(name = "message_send_time", nullable = false)
    private LocalTime messageSendTime;

    /**
     * Message template with dynamic placeholders.
     * Available variables:
     *   {{customerName}}  - Full name of the customer
     *   {{invoiceNumber}} - Invoice / order reference number
     *   {{dueDate}}       - Original due date of the invoice
     *   {{daysOverdue}}   - Number of days the invoice is overdue
     *   {{amountDue}}     - Outstanding amount due
     *   {{companyName}}   - Your organization name
     *
     * Example:
     *   "Dear {{customerName}}, your invoice {{invoiceNumber}} of {{amountDue}}
     *    was due on {{dueDate}} and is now {{daysOverdue}} day(s) overdue.
     *    Please settle at your earliest convenience. – {{companyName}}"
     */
    @JsonView(BaseView.ReminderSettingView.class)
    @Column(name = "message_template", nullable = false, columnDefinition = "TEXT")
    private String messageTemplate;

    @JsonView(BaseView.ReminderSettingView.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false)
    private ReminderFrequency frequency;
}
