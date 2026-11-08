package com.app.itibari.services;

import com.app.itibari.models.request.DepartmentRequest;
import com.app.itibari.models.response.DepartmentDtoResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.security.Principal;
import java.util.UUID;

public interface DepartmentService {
    DepartmentDtoResponse createDepartment(@Valid DepartmentRequest departmentRequest, Principal principal);

    DepartmentDtoResponse updateDepartment(UUID publicId, @Valid DepartmentRequest departmentRequest, Principal principal);

    Page<DepartmentDtoResponse> getDepartmentList(String searchParam, Pageable pageable);

    DepartmentDtoResponse getDepartment(UUID publicId);

    void deleteDepartment(UUID publicId, Principal principal);
}
