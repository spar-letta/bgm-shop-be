package com.app.itibari.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "delivery_agent_login_sessions", schema = "itibari")
public class DeliveryAgentLoginSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_agent_id", nullable = false)
    private DeliveryAgents deliveryAgent;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    private String deviceName;
    private String deviceModel;
    private String platform;
    private String appVersion;
    private LocalDateTime loginTime;
    private LocalDateTime lastActivityTime;
    private LocalDateTime logoutTime;
    private LocalDateTime expiresAt;
    private boolean active;
    private String logoutReason;
}
