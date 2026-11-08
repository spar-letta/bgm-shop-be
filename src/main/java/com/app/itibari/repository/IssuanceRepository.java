package com.app.itibari.repository;

import com.app.itibari.models.entities.Issuance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssuanceRepository extends JpaRepository<Issuance, Long> {

    void deleteByRouteId(String routeId);
}
