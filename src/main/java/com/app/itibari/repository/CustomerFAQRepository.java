package com.app.itibari.repository;

import com.app.itibari.models.entities.CustomerFAQ;
import com.app.itibari.models.entities.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CustomerFAQRepository extends JpaRepository<CustomerFAQ, Long>, JpaSpecificationExecutor<CustomerFAQ> {
    Optional<CustomerFAQ> findByDepartmentAndDeletedIsFalse(Department department);

    Optional<CustomerFAQ> findByPublicIdAndDeletedIsFalse(UUID publicId);
}
