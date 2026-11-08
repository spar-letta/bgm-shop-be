package com.app.itibari.services;

import com.app.itibari.dataTypes.CounterType;
import com.app.itibari.models.entities.Counter;
import com.app.itibari.repository.CounterRepository;
import org.springframework.stereotype.Service;

@Service
public class CounterServiceImpl implements CounterService {

    private final CounterRepository counterRepository;

    public CounterServiceImpl(CounterRepository counterRepository) {

        this.counterRepository = counterRepository;
    }

    @Override
    public Integer getNextCounter(CounterType counterType) {

        Counter counter = counterRepository.findByCounterType(counterType)
                .orElseGet(() -> {
                    Counter newCounter = new Counter(counterType);
                    return counterRepository.save(newCounter);
                });

        Integer current = counter.getCurrent();
        counter.setCurrent(current + 1);
        counterRepository.save(counter);

        return current;
    }
}
