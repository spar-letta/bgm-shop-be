package com.app.itibari.repository;

import com.app.itibari.dataTypes.CounterType;
import com.app.itibari.models.entities.Counter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CounterRepository extends JpaRepository<Counter, Long> {

    Optional<Counter> findByCounterType(CounterType counterType);
}
