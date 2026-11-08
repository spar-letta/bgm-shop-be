package com.app.itibari.services;

import com.app.itibari.converter.Converter;
import com.app.itibari.models.entities.Department;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.request.DepartmentRequest;
import com.app.itibari.models.response.DepartmentDtoResponse;
import com.app.itibari.repository.DepartmentRepository;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImp implements DepartmentService{

    private final DepartmentRepository departmentRepository;
    private final PersonRepository personRepository;
    private final Converter<Department, DepartmentDtoResponse> convertToDto;


    @Override
    public DepartmentDtoResponse createDepartment(DepartmentRequest departmentRequest, Principal principal) {
        Person createdBy = getPerson(principal.getName());
        Department department = departmentRepository.findByNameIgnoreCase(departmentRequest.name()).orElse(null);

        Department saveDepartment;
        if (department == null){
            Department department1 = new Department();
            department1.setName(departmentRequest.name());
            department1.setCreatedBy(createdBy);
            saveDepartment = departmentRepository.save(department1);
        }else {
            saveDepartment = department;
        }

        return convertToDto.convert(saveDepartment);
    }

    @Override
    public DepartmentDtoResponse updateDepartment(UUID publicId, DepartmentRequest departmentRequest, Principal principal) {
        Person updatedBy = getPerson(principal.getName());
        Department department = fetchDepartment(publicId);
        department.setName(departmentRequest.name());
        department.setUpdatedBy(updatedBy);
        Department updatedDepartment = departmentRepository.save(department);
        return convertToDto.convert(updatedDepartment);
    }

    @Override
    public Page<DepartmentDtoResponse> getDepartmentList(String searchParam, Pageable pageable) {
        Page<Department> departments;

        if (searchParam != null && !searchParam.isBlank()) {
            departments = departmentRepository.findDepartmentByNameIgnoreCase(searchParam, pageable);
        } else {
            departments = departmentRepository.findAll(pageable);
        }

        return departments.map(convertToDto::convert);
    }

    @Override
    public DepartmentDtoResponse getDepartment(UUID publicId) {
        Department department = fetchDepartment(publicId);
        return convertToDto.convert(department);
    }

    @Override
    public void deleteDepartment(UUID publicId, Principal principal) {
        Person deletedBy = getPerson(principal.getName());
        Department foundDepartment = fetchDepartment(publicId);
        foundDepartment.setDeleted(true);
        foundDepartment.setUpdatedBy(deletedBy);
        departmentRepository.save(foundDepartment);
    }


    public Person getPerson(String username) {
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(username);
        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("User not found")
                    .userMessage("Sorry, user not found");
        }
        return optionalPerson.get();
    }

    public Department fetchDepartment(UUID publicId){
        Department department = departmentRepository.findByPublicId(publicId).orElse(null);
        if (department == null){
            throw new BadRequestRestApiException()
                    .developerMessage("Department not found")
                    .userMessage("Sorry, department not found");
        }
        return department;
    }
}
