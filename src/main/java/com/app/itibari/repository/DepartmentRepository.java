package com.app.itibari.repository;

import com.app.itibari.models.entities.Department;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByNameIgnoreCase(@NotEmpty String name);
    Optional<Department> findByNameIgnoreCaseAndDeletedIsFalse(@NotEmpty String name);

    Optional<Department> findByPublicId(UUID publicId);

    Page<Department> findDepartmentByNameIgnoreCase(String searchParam, Pageable pageable);

    Optional<Department> findByPublicIdAndDeletedIsFalse(UUID departmentPublicId);

    List<Department> findByDeletedIsFalse();
}
