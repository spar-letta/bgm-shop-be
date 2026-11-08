package com.app.itibari.services;

import com.app.itibari.dataTypes.Module;
import com.app.itibari.models.entities.Department;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.RejectionsCode;
import com.app.itibari.models.request.RejectionCodeRequest;
import com.app.itibari.repository.DepartmentRepository;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.RejectionCodeRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.specification.RejectionsCodeSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class RejectionServiceImpl implements RejectionCodeService {

    private final RejectionCodeRepository rejectionCodeRepository;

    private final PersonRepository personRepository;

    private final DepartmentRepository departmentRepository;

    public RejectionServiceImpl(RejectionCodeRepository rejectionCodeRepository, PersonRepository personRepository, DepartmentRepository departmentRepository) {

        this.rejectionCodeRepository = rejectionCodeRepository;
        this.personRepository = personRepository;
        this.departmentRepository = departmentRepository;
    }

    @Override
    public RejectionsCode createRejectionCode(RejectionCodeRequest rejectionCodeRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<RejectionsCode> rejectionCodeOptional = rejectionCodeRepository.findRejectionsCodeByCodeAndModuleAndDeletedIsFalse(
                rejectionCodeRequest.getCode(), rejectionCodeRequest.getModule());

        if (rejectionCodeOptional.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Rejection code already exists")
                    .userMessage("Rejection code already exists");
        }

        RejectionsCode rejectionsCode = new RejectionsCode();
        rejectionsCode.setCode(rejectionCodeRequest.getCode());
        rejectionsCode.setDescription(rejectionCodeRequest.getDescription());
        optionalPerson.ifPresent(rejectionsCode::setCreatedBy);
        rejectionsCode.setCreationTime(LocalDateTime.now());
        rejectionsCode.setDepartment(resolveDepartment(rejectionCodeRequest.getDepartmentPublicId()));
        rejectionsCode.setModule(rejectionCodeRequest.getModule());

        return rejectionCodeRepository.save(rejectionsCode);
    }


    @Override
    public void updateRejectionCode(UUID publicId, RejectionCodeRequest rejectionCodeRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<RejectionsCode> rejectionCodeOptional = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(publicId);

        if (rejectionCodeOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Rejection code not found")
                    .userMessage("Rejection code not found");
        }

        RejectionsCode rejectionsCode = rejectionCodeOptional.get();
        rejectionsCode.setCode(rejectionCodeRequest.getCode());
        rejectionsCode.setDescription(rejectionCodeRequest.getDescription());
        rejectionsCode.setDepartment(resolveDepartment(rejectionCodeRequest.getDepartmentPublicId()));
        optionalPerson.ifPresent(rejectionsCode::setUpdatedBy);
        rejectionsCode.setUpdateDate(LocalDateTime.now());

        rejectionCodeRepository.save(rejectionsCode);
    }

    @Override
    public void deleteRejectionCode(UUID publicId, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<RejectionsCode> rejectionCodeOptional = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(publicId);

        if (rejectionCodeOptional.isPresent()) {
            RejectionsCode rejectionsCode = rejectionCodeOptional.get();
            rejectionsCode.setDeleted(true);
            optionalPerson.ifPresent(rejectionsCode::setUpdatedBy);
            rejectionsCode.setUpdateDate(LocalDateTime.now());
            rejectionCodeRepository.save(rejectionsCode);
        }
    }

    @Override
    public Page<RejectionsCode> getRejectionCodes(String searchParam, Module module, UUID  departmentPublicId,Pageable pageable) {

        Specification<RejectionsCode> spec = Specification
                .where(RejectionsCodeSpecification.searchParamMatches(searchParam))
                .and(RejectionsCodeSpecification.hasModule(module))
                .and(RejectionsCodeSpecification.hasDepartment(departmentPublicId))
                .and(RejectionsCodeSpecification.isNotDeleted());

        return rejectionCodeRepository.findAll(spec, pageable);
    }

    private Department resolveDepartment(UUID departmentPublicId) {
        if (departmentPublicId == null) {
            return null;
        }

        return departmentRepository.findByPublicIdAndDeletedIsFalse(departmentPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Department not found")
                        .userMessage("Department not found"));
    }
}
