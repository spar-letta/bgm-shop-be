package com.app.itibari.repository;

import com.app.itibari.models.entities.HelpDesk;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface HelpDeskRepository extends JpaRepository<HelpDesk, Long> {

    Optional<HelpDesk> findByPublicId(UUID publicId);

    Optional<HelpDesk> findByPublicIdAndDeletedIsFalse(UUID ticketPublicId);

    @Query("select hs.workflowNewStatus from HelpDesk hs " +
            "where (:status is null or lower(hs.workflowNewStatus) like lower(concat('%', :status, '%')))")
    Set<String> findByAllNames(@Param("status") String status);

    @Query("select hs.workflowNewStatus from HelpDesk hs")
    Set<String> findAllNames();
}
