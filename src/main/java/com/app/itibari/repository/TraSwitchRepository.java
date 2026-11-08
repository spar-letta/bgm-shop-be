package com.app.itibari.repository;

import com.app.itibari.models.entities.TraSwitch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TraSwitchRepository extends JpaRepository<TraSwitch, Long> {

    Optional<TraSwitch> findTraSwitchById(long l);
}

