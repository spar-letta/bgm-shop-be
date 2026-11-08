package com.app.itibari.repository;

import com.app.itibari.models.entities.VolumeRange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VolumeRangeRepository extends JpaRepository<VolumeRange, Long> {

    Optional<VolumeRange> findByPublicIdAndDeletedIsFalse(UUID publicId);
}
