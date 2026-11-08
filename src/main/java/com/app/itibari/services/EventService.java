package com.app.itibari.services;

import com.app.itibari.models.entities.Events;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface EventService {

    Page<Events> getEvent(UUID publicId, Pageable pageable);
}
