package com.app.itibari.repository;

import com.app.itibari.models.entities.DeliveryAgents;
import com.app.itibari.models.entities.TumaSms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TumaSmsRepository extends JpaRepository<TumaSms, Long>, JpaSpecificationExecutor<TumaSms> {
}
