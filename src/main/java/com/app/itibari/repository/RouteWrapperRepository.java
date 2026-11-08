package com.app.itibari.repository;

import com.app.itibari.models.entities.RouteWrapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RouteWrapperRepository extends JpaRepository<RouteWrapper, Long>, JpaSpecificationExecutor<RouteWrapper> {

    Optional<RouteWrapper> findFirstByCreationTimeBetweenAndDeletedIsFalseOrderByCreationTimeAsc(LocalDateTime startTime,
                                                                                                 LocalDateTime endTime);

    Optional<RouteWrapper> findByPublicIdAndDeletedIsFalse(UUID publicId);
}
