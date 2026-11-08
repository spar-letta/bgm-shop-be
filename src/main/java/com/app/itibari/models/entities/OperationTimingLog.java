package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.OperationTimingEntityType;
import com.app.itibari.dataTypes.OperationTimingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "operation_timing_logs", schema = "itibari")
public class OperationTimingLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "operation_name")
    private String operationName;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type")
    private OperationTimingEntityType entityType;

    @Column(name = "route_id")
    private String routeId;

    @Column(name = "route_public_id")
    private String routePublicId;

    @Column(name = "requested_by")
    private String requestedBy;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OperationTimingStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
