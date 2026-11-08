package com.app.itibari.services;

import com.app.itibari.models.entities.Events;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.TraSwitch;
import com.app.itibari.repository.EventsRepository;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.TraSwitchRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TraSwitchServiceIml implements TraSwitchService {
    private final TraSwitchRepository traSwitchRepository;
    private final PersonRepository personRepository;
    private final EventsRepository eventsRepository;

    public TraSwitchServiceIml(TraSwitchRepository traSwitchRepository, PersonRepository personRepository, EventsRepository eventsRepository) {
        this.traSwitchRepository = traSwitchRepository;
        this.personRepository = personRepository;
        this.eventsRepository = eventsRepository;
    }

    @Override
    public TraSwitch switchTraOn(Boolean switchOn, String name) {
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("User not found")
                    .developerMessage("User not found with email: " + name);
        }
        Person person = optionalPerson.get();

        TraSwitch traSwitch = traSwitchRepository.findTraSwitchById(1L).orElse(new TraSwitch());
        traSwitch.setSwitchOn(switchOn!=null? switchOn: true);
        traSwitch.setCreatedBy(person);
        TraSwitch saved = traSwitchRepository.save(traSwitch);

        String description = saved.isSwitchOn() ? "TRA switch has been turned ON by " + person.getFirstName() + " " + person.getLastName()
                : "TRA switch has been turned OFF by " + person.getFirstName() + " " + person.getLastName();

        eventsRepository.save(new Events(saved.getPublicId().toString(),"TRA SWITCH", LocalDateTime.now(),description,"SWITCH",person));

        return saved;
    }

    @Override
    public boolean traSwitchOn() {
        return traSwitchRepository.findTraSwitchById(1L).map(TraSwitch::isSwitchOn).orElse(false);
    }

    @Override
    public TraSwitch getTraSwitchStatus(String name) {
        return traSwitchRepository.findTraSwitchById(1L).orElse(null);
    }
}
