package com.app.itibari.repository;

import com.app.itibari.models.entities.FollowUp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {
    Page<FollowUp> findByCustomerPublicIdAndDeletedIsFalse(UUID customerPublicId, Pageable pageable);

    Optional<FollowUp> findByPublicIdAndDeletedIsFalse(UUID publicId);

    @Query("SELECT f " +
            "FROM FollowUp f " +
            "WHERE f.followUpDate = ?1 " +
            "AND f.deleted = false ")
    List<FollowUp> findAllByOneDayTo(LocalDate localDate);
}
