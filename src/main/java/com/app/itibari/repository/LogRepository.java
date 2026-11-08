package com.app.itibari.repository;

import com.app.itibari.models.entities.Logs;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogRepository extends JpaRepository<Logs, Long> {

}
