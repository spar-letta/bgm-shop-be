package com.app.itibari.repository;

import com.app.itibari.models.entities.Events;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventsRepository extends JpaRepository<Events, Long> {

    Page<Events> findByReferenceIdIgnoreCase(String string, Pageable pageable);
}
