package com.app.itibari.repository;

import com.app.itibari.models.entities.Notification;
import com.app.itibari.models.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.publicId = :publicId AND n.isDeleted = false")
    Optional<Notification> findByPublicIdAndNotDeleted(@Param("publicId") UUID publicId);

    @Query("SELECT DISTINCT n FROM Notification n " +
            "JOIN n.roles r " +
            "WHERE r IN :roles " +
            "AND n.isDeleted = false")
    List<Notification> findAllByPersonRolesAndNotDeleted(@Param("roles") List<Role> roles);
}
