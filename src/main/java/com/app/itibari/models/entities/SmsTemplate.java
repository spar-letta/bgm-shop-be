package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.SmsChannel;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "sms_templates", schema = "itibari")
public class SmsTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "sms_channel")
    private SmsChannel smsChannel;

    @Column(name = "message")
    private String message;

    @Column(name = "active")
    private boolean active;
}
