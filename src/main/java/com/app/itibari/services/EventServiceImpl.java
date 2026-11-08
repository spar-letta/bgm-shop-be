package com.app.itibari.services;

import com.app.itibari.models.entities.Events;
import com.app.itibari.repository.EventsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EventServiceImpl implements EventService {

    private final EventsRepository eventRepository;

    public EventServiceImpl(EventsRepository eventRepository) {

        this.eventRepository = eventRepository;
    }

    @Override
    public Page<Events> getEvent(UUID publicId, Pageable pageable) {

        return eventRepository.findByReferenceIdIgnoreCase(publicId.toString(), pageable);
    }
}
