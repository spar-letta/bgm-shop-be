package com.app.itibari.repository;

import com.app.itibari.models.entities.CustomerExperience;
import com.app.itibari.models.entities.Department;
import com.app.itibari.models.entities.WorkflowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface WorkflowStatusRepository extends JpaRepository<WorkflowStatus, Long> {
    Page<WorkflowStatus> findByTemplateAndDeletedIsFalse(CustomerExperience customerExperience, Pageable pageable);

    Optional<WorkflowStatus> findByPublicIdAndDeletedIsFalse(UUID uuid);

    Page<WorkflowStatus> findByDeletedIsFalse(Pageable pageable);

    Optional<WorkflowStatus> findByPublicIdAndTemplateAndDeletedIsFalse(UUID statusPublicId, CustomerExperience customerExperience);

    Optional<WorkflowStatus> findByNameAndTemplateAndDeletedIsFalse(String name, CustomerExperience customerExperience);

    Set<WorkflowStatus> findByNameIgnoreCaseAndTemplateAndDeletedIsFalseAndDepartment(String aNew, CustomerExperience customerExperience, Department department);
}
