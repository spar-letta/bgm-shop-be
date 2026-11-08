package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.WrapperStatus;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "route_planning_wrapper", schema = "itibari")
public class RouteWrapper  extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "route_wrapper_id")
    @JsonView(BaseView.RouteView.class)
    private String routeWrapperId;

    @JsonIgnore
    @OneToMany(mappedBy = "routeWrapper", fetch = FetchType.LAZY)
    private List<RoutePlanning> routePlanningList = new ArrayList<>();

    @JsonView(BaseView.RouteView.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @Column(name = "start_time")
    private LocalDateTime start;

    @JsonView(BaseView.RouteView.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @Column(name = "end_time")
    private LocalDateTime end;

    @JsonView(BaseView.RouteView.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "wrapper_status")
    private WrapperStatus wrapperStatus;
}
