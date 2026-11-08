package com.app.itibari.repository;

import com.app.itibari.models.entities.CustomerExperience;
import com.app.itibari.models.entities.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {
    Optional<Workflow> findByPublicIdAndDeletedIsFalse(UUID workflowId);

    Optional<Workflow> findByTemplateAndDeletedIsFalse(CustomerExperience customerExperience);
    Optional<Workflow> findTopByTemplateAndDeletedIsFalseOrderByCreationTimeDesc(CustomerExperience customerExperience);

}
