package com.app.itibari.repository;

import com.app.itibari.models.entities.Transition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransitionRepository extends JpaRepository<Transition, Long> {
    Optional<Transition> findByPublicIdAndDeletedIsFalse(UUID transitionPublicId);

    Optional<Transition> findByTransitionIndexAndDeletedIsFalse(Integer i);
}
