package com.app.itibari.repository;

import com.app.itibari.models.entities.Department;
import com.app.itibari.models.entities.IssueType;
import com.app.itibari.models.request.TicketDto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IssueTypeRepository extends JpaRepository<IssueType, Long> {
    Optional<IssueType> findByDepartmentAndNameIgnoreCaseAndCustomerType(Department department, String name, TicketDto.CustomerType customerType);

    Optional<IssueType> findByPublicId(UUID publicId);

    Optional<IssueType> findByPublicIdAndDeletedIsFalse(UUID uuid);
}
