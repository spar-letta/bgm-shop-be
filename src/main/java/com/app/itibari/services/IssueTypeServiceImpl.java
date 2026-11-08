package com.app.itibari.services;

import com.app.itibari.converter.Converter;
import com.app.itibari.models.entities.Department;
import com.app.itibari.models.entities.IssueType;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.PrioritySetting;
import com.app.itibari.models.request.IssueTypeRequest;
import com.app.itibari.models.request.TicketDto;
import com.app.itibari.models.response.IssueTypeResponse;
import com.app.itibari.repository.DepartmentRepository;
import com.app.itibari.repository.IssueTypeRepository;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.PrioritySettingRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IssueTypeServiceImpl implements IssueTypeService {
    @PersistenceContext
    private EntityManager entityManager;

    private final IssueTypeRepository issueTypeRepository;
    private final PersonRepository personRepository;
    private final DepartmentRepository departmentRepository;
    private final Converter<IssueType, IssueTypeResponse> converterToDto;
    private final PrioritySettingRepository prioritySettingRepository;


    @Override
    public IssueTypeResponse createIssueType(IssueTypeRequest issueTypeRequest, Principal principal) {
        Person createdBy = getPerson(principal.getName());
        Department department = fetchDepartment(issueTypeRequest.departmentPublicId());

        IssueType issueType =
                issueTypeRepository.findByDepartmentAndNameIgnoreCaseAndCustomerType(department, issueTypeRequest.name(), issueTypeRequest.customerType()).orElse(null);

        PrioritySetting prioritySetting = prioritySettingRepository.findByPublicIdAndDeletedIsFalse(issueTypeRequest.priorityPublicId()).orElse(null);

        IssueType saveIssueType;
        if (issueType == null) {
            IssueType issueType1 = new IssueType();
            issueType1.setName(issueTypeRequest.name());
            issueType1.setDepartment(department);
            issueType1.setCreatedBy(createdBy);
            issueType1.setPrioritySetting(prioritySetting);
            issueType1.setIsActive(Boolean.TRUE);
            issueType1.setCustomerType(issueTypeRequest.customerType());
            saveIssueType = issueTypeRepository.save(issueType1);
        } else {
            saveIssueType = issueType;
        }

        return converterToDto.convert(saveIssueType);
    }

    @Override
    public Page<IssueTypeResponse> listIssueType(UUID departmentId, String searchParam, Boolean isActive, TicketDto.CustomerType customerType, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<IssueType> cq = cb.createQuery(IssueType.class);
        Root<IssueType> root = cq.from(IssueType.class);
        Predicate[] predicates = buildPredicates(cb, root, departmentId, searchParam, isActive, customerType);

        cq.where(predicates).orderBy(cb.desc(root.get("id")));

        TypedQuery<IssueType> query = entityManager.createQuery(cq)
                .setMaxResults(pageable.getPageSize())
                .setFirstResult((int) pageable.getOffset());

        List<IssueType> tickets = query.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<IssueType> countRoot = countQuery.from(IssueType.class);
        Predicate[] countPredicates = buildPredicates(cb, countRoot, departmentId, searchParam, isActive, customerType);

        countQuery.select(cb.countDistinct(countRoot)).where(countPredicates);
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        List<IssueTypeResponse> dtoList = tickets.stream()
                .map(converterToDto::convert)
                .toList();

        return new PageImpl<>(dtoList, pageable, total);
    }

    @Override
    public IssueTypeResponse getIssueType(UUID publicId) {
        IssueType issueType = fetchIssueType(publicId);
        return converterToDto.convert(issueType);
    }

    @Override
    public IssueTypeResponse updateIssueType(UUID publicId, IssueTypeRequest issueTypeRequest, Principal principal) {
        Person createdBy = getPerson(principal.getName());
        IssueType foundIssueType = issueTypeRepository.findByPublicId(publicId).orElseThrow(() ->
                new BadRequestRestApiException()
                        .developerMessage("Issue not found")
                        .userMessage("Sorry, issue not found")
        );

        Department department = fetchDepartment(issueTypeRequest.departmentPublicId());

        IssueType duplicateCheck = issueTypeRepository
                .findByDepartmentAndNameIgnoreCaseAndCustomerType(department, issueTypeRequest.name(), issueTypeRequest.customerType())
                .orElse(null);

        if (duplicateCheck != null && !duplicateCheck.getId().equals(foundIssueType.getId())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Issue type with same name exists in department")
                    .userMessage("An issue type with this name already exists");
        }

        PrioritySetting prioritySetting = prioritySettingRepository
                .findByPublicIdAndDeletedIsFalse(issueTypeRequest.priorityPublicId())
                .orElseThrow(() ->
                        new BadRequestRestApiException()
                                .developerMessage("Priority not found")
                                .userMessage("Sorry, priority not found")
                );

        foundIssueType.setName(issueTypeRequest.name());
        foundIssueType.setDepartment(department);
        foundIssueType.setPrioritySetting(prioritySetting);
        foundIssueType.setCreatedBy(createdBy);
        foundIssueType.setCustomerType(issueTypeRequest.customerType());

        IssueType savedIssueType = issueTypeRepository.save(foundIssueType);

        return converterToDto.convert(savedIssueType);
    }

    @Override
    public IssueTypeResponse runActionOnIssueType(UUID publicId, Boolean isActive, Principal principal) {
        Person updatedBy = getPerson(principal.getName());
        if (isActive ==  null){
            isActive = Boolean.FALSE;
        }
        IssueType issueType = issueTypeRepository.findByPublicId(publicId).orElse(null);
        if (issueType == null){
            throw new BadRequestRestApiException()
                    .developerMessage("Issue type not found")
                    .userMessage("Sorry, issue type not found");
        }
        issueType.setIsActive(isActive);
        issueType.setUpdatedBy(updatedBy);

        IssueType savedIssueType = issueTypeRepository.save(issueType);

        return converterToDto.convert(savedIssueType);
    }

    @Override
    public void runDeleteIssueType(UUID publicId, Principal principal) {
        IssueType issueType = issueTypeRepository.findByPublicId(publicId).orElse(null);
        if (issueType == null){
            throw new BadRequestRestApiException()
                    .developerMessage("Issue type not found")
                    .userMessage("Sorry, issue type not found");
        }

        issueType.setDeleted(Boolean.TRUE);
        issueTypeRepository.save(issueType);
    }

    private Predicate[] buildPredicates(CriteriaBuilder cb, Root<IssueType> root, UUID departmentId, String searchParam, Boolean isActive, TicketDto.CustomerType customerType) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.isFalse(root.get("deleted")));

        if (departmentId != null) {
            predicates.add(cb.equal(root.get("department").get("publicId"), departmentId));
        }

        if (isActive != null) {
            predicates.add(cb.equal(root.get("isActive"), isActive));
        }

        if (customerType != null) {
            predicates.add(cb.equal(root.get("customerType"), customerType));
        }

        if (searchParam != null && searchParam.trim().length() >= 3) {
            String search = "%" + searchParam.trim().toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(root.get("name")), search));
        }

        return predicates.toArray(new Predicate[0]);
    }

    public Department fetchDepartment(UUID publicId) {
        Department department = departmentRepository.findByPublicId(publicId).orElse(null);
        if (department == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Department not found")
                    .userMessage("Sorry, department not found");
        }
        return department;
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

    public IssueType fetchIssueType(UUID publicId) {
        IssueType issueType = issueTypeRepository.findByPublicId(publicId).orElse(null);
        if (issueType == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Issue Type not found")
                    .userMessage("Sorry, Issue type not found");
        }
        return issueType;
    }
}
