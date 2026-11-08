package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.RegistrationStatus;
import com.app.itibari.dataTypes.VehicleType;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "vehicles", schema = "itibari")
public class Vehicle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.VehicleView.class)
    @Column(name = "number_plate")
    private String numberPlate;

    @JsonView(BaseView.VehicleView.class)
    @Column(name = "weight")
    private Integer weight;

    @JsonView(BaseView.VehicleView.class)
    @Column(name = "owner")
    private String owner;

    @JsonView(BaseView.VehicleView.class)
    @Column(name = "owner_phone")
    private String ownerPhone;

    @JsonView(BaseView.VehicleView.class)
    @Column(name = "assigned_driver")
    private String assignedDriver;

    @JsonView(BaseView.VehicleView.class)
    @Column(name = "registration_status")
    @Enumerated(EnumType.STRING)
    private RegistrationStatus registrationStatus;

    @JsonView(BaseView.VehicleView.class)
    @Column(name = "insurance_expiry_date")
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate insuranceExpiryDate;

    @JsonView(BaseView.VehicleView.class)
    @Column(name = "vehicle_type")
    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;

    @JsonView(BaseView.VehicleView.class)
    @Column(name = "available")
    private boolean available = true;

    @JsonView(BaseView.VehicleView.class)
    @Column(name = "length")
    private Double length = 0.0;

    @JsonView(BaseView.VehicleView.class)
    @Column(name = "width")
    private Double width = 0.0;    // in meters

    @JsonView(BaseView.VehicleView.class)
    @Column(name = "height")
    private Double height = 0.0;   // in meters

    @JsonView(BaseView.VehicleView.class)
    @Column(name = "payload_capacity")
    private Double payloadCapacity = 0.0;
}
