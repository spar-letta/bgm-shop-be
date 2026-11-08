package com.app.itibari.models.request;

import com.app.itibari.dataTypes.RegistrationStatus;
import com.app.itibari.dataTypes.VehicleType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class VehicleRequest {

    @NotNull
    private String numberPlate;

    private String ownerName;

    private String ownerPhoneNumber;

    private String assignedDriver;

    private RegistrationStatus registrationStatus;

    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate insuranceExpiryDate;

    private VehicleType vehicleType;

    private Integer weight; // ---truck itself

    private double length;   // in meters
    private double width;    // in meters
    private double height;   // in meters
    private double payloadCapacity; // in kilograms   ---cargo it can carry
}
