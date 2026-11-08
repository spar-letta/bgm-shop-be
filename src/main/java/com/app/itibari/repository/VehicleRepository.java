package com.app.itibari.repository;

import com.app.itibari.models.entities.Vehicle;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, Long>, JpaSpecificationExecutor<Vehicle> {

    Optional<Vehicle> findVehicleByNumberPlateIgnoreCaseAndDeletedIsFalse(@NotNull String numberPlate);

    Optional<Vehicle> findByPublicIdAndDeletedIsFalse(UUID vehiclePublicId);
}
