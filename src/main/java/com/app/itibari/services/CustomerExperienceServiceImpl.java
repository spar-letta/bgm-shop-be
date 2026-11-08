package com.app.itibari.services;

import com.app.itibari.converter.CustomerNotesToResponse;
import com.app.itibari.converter.PrioritySettingToResponse;
import com.app.itibari.dataTypes.AssignToEnum;
import com.app.itibari.dataTypes.NoteType;
import com.app.itibari.dataTypes.SmsChannel;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.*;
import com.app.itibari.models.response.CustomerNotesDTO;
import com.app.itibari.models.response.PrioritySettingResponse;
import com.app.itibari.models.response.SmsTemplateResponse;
import com.app.itibari.repository.*;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.formatter.DateFormatter;
import com.app.itibari.utils.helpers.CsNotesToExcel;
import com.app.itibari.utils.helpers.EmailBodyCrafter;
import com.app.itibari.utils.helpers.WebhookClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class CustomerExperienceServiceImpl implements CustomerExperienceService {

    private final CustomerExperienceRepository customerExperienceRepository;
    private final PrioritySettingRepository prioritySettingRepository;
    private final PersonRepository personRepository;
    private final IssueTypeRepository issueTypeRepository;
    private final DepartmentRepository departmentRepository;
    private final PrioritySettingToResponse prioritySettingToResponse;
    private final CustomerNotesRepository customerNotesRepository;
    private final CustomersRepository customersRepository;
    private final CustomerNotesToResponse customerNotesToResponse;
    private final DeliveryAgentsRepository deliveryAgentsRepository;
    private final SalesAgentsRepository salesAgentsRepository;
    private final RejectionCodeRepository rejectionCodeRepository;
    private final SmsTemplateRepository smsTemplateRepository;
    private final WebhookClient webhookClient;
    private final EmailBodyCrafter emailBodyCrafter;
    private final EmailService emailService;
    private final CsNotesToExcel csNotesToExcel;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public CustomerExperience createCustomerExperience(CustomerExperienceRequest request, Principal principal) {
        Person createdBy = getPerson(principal.getName());
        ObjectMapper mapper = new ObjectMapper();

        IssueType issueType = issueTypeRepository.findByPublicIdAndDeletedIsFalse(request.issueTypePublicId()).orElse(null);

        if (issueType == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Issue type not found")
                    .userMessage("Sorry, Issue type found");
        }

        List<Department> department = new ArrayList<>();
        if (request.departmentInCopyPublicId() != null && !request.departmentInCopyPublicId().isEmpty()) {
            for (UUID uuid : request.departmentInCopyPublicId()) {
                Department foundDepartment = departmentRepository.findByPublicIdAndDeletedIsFalse(uuid).orElse(null);
                if (foundDepartment == null) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Department not found")
                            .userMessage("Sorry, department found");
                }
                department.add(foundDepartment);
            }
        }

        CustomerExperience customerExperience = new CustomerExperience();
        customerExperience.setTemplateName(request.templateName());
        customerExperience.setIssueType(issueType);
        if (!department.isEmpty()) {
            customerExperience.getDepartmentInCopy().addAll(department);
        }
        customerExperience.setAssignTo(request.assignTo() != null ? request.assignTo() : AssignToEnum.department);
        customerExperience.setPocEmail(request.pocEmail());
        customerExperience.setIsActive(Boolean.TRUE);
        customerExperience.setCreatedBy(createdBy);
        customerExperience.setAllowNotification(request.allowNotification());
        if (!request.emailInCopy().isEmpty()) {
            customerExperience.setEmailInCopy(request.emailInCopy().stream().collect(Collectors.joining(",")));
        }

        customerExperience.setCustomFields(mapper.valueToTree(request.customFields()));

        return customerExperienceRepository.save(customerExperience);
    }

    @Override
    public Page<CustomerExperience> getCustomerExperience(String searchParam, LocalDate startDate,
                                                          LocalDate endDate,
                                                          UUID issueTypePublicId,
                                                          Boolean isActive,
                                                          TicketDto.CustomerType customerType,
                                                          Pageable pageable) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<CustomerExperience> cq = cb.createQuery(CustomerExperience.class);
        Root<CustomerExperience> root = cq.from(CustomerExperience.class);
        Predicate[] predicates = buildPredicateCustomerExperience(cb, root, startDate, endDate, searchParam, isActive, issueTypePublicId, customerType);

        cq.where(predicates).orderBy(cb.desc(root.get("id")));

        TypedQuery<CustomerExperience> query = entityManager.createQuery(cq)
                .setMaxResults(pageable.getPageSize())
                .setFirstResult((int) pageable.getOffset());

        List<CustomerExperience> tickets = query.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<CustomerExperience> countRoot = countQuery.from(CustomerExperience.class);
        Predicate[] countPredicates = buildPredicateCustomerExperience(cb, root, startDate, endDate, searchParam, isActive, issueTypePublicId, customerType);

        countQuery.select(cb.countDistinct(countRoot)).where(countPredicates);

        return new PageImpl<>(tickets, pageable, tickets.size());
    }

    private Predicate[] buildPredicateCustomerExperience(CriteriaBuilder cb, Root<CustomerExperience> root, LocalDate startDate, LocalDate endDate, String searchParam, Boolean isActive, UUID issueTypePublicId, TicketDto.CustomerType customerType) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.isFalse(root.get("deleted")));

        if (issueTypePublicId != null) {
            predicates.add(cb.equal(root.get("issueType").get("publicId"), issueTypePublicId));
        }

        if (isActive != null) {
            predicates.add(cb.equal(root.get("isActive"), isActive));
        }

        if (customerType != null) {
            predicates.add(cb.equal(root.get("issueType").get("customerType"), customerType));
        }

        if (searchParam != null && searchParam.trim().length() >= 3) {
            String search = "%" + searchParam.trim().toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(root.get("templateName")), search));
        }

        return predicates.toArray(new Predicate[0]);
    }

    @Override
    public PrioritySettingResponse createPrioritySetting(PrioritySettingRequest request, Principal principal) {
        Person createdBy = getPerson(principal.getName());
        PrioritySetting prioritySetting = prioritySettingRepository.findByPriorityNameIgnoreCaseAndDeletedIsFalse(request.priorityName().trim()).orElse(null);
        if (prioritySetting == null) {
            prioritySetting = new PrioritySetting();
            prioritySetting.setPriorityName(request.priorityName().trim());
            prioritySetting.setSlaType(request.slaType());
            prioritySetting.setSlaDuration(request.slaDuration());
            prioritySetting.setColorName(request.colorName());
            prioritySetting.setCreatedBy(createdBy);
            prioritySetting.setIsActive(Boolean.TRUE);
            prioritySetting.setFirstTimeResponse(request.firstTimeResponse());
            return prioritySettingToResponse.convert(prioritySettingRepository.save(prioritySetting));
        } else {
            return prioritySettingToResponse.convert(prioritySetting);
        }
    }

    @Override
    public Page<PrioritySettingResponse> getPrioritySettings(String searchParam, LocalDate startDate, LocalDate endDate, Boolean isActive, Pageable pageable) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<PrioritySetting> cq = cb.createQuery(PrioritySetting.class);
        Root<PrioritySetting> root = cq.from(PrioritySetting.class);
        Predicate[] predicates = buildPredicates(cb, root, searchParam, isActive);

        cq.where(predicates).orderBy(cb.desc(root.get("id")));

        TypedQuery<PrioritySetting> query = entityManager.createQuery(cq)
                .setMaxResults(pageable.getPageSize())
                .setFirstResult((int) pageable.getOffset());

        List<PrioritySetting> tickets = query.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<PrioritySetting> countRoot = countQuery.from(PrioritySetting.class);
        Predicate[] countPredicates = buildPredicates(cb, countRoot, searchParam, isActive);

        countQuery.select(cb.countDistinct(countRoot)).where(countPredicates);
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        List<PrioritySettingResponse> dtoList = tickets.stream()
                .map(prioritySettingToResponse::convert)
                .toList();

        return new PageImpl<>(dtoList, pageable, total);
    }

    private Predicate[] buildPredicates(CriteriaBuilder cb, Root<PrioritySetting> root, String searchParam, Boolean isActive) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.isFalse(root.get("deleted")));

        if (isActive != null) {
            predicates.add(cb.equal(root.get("isActive"), isActive));
        }

        if (searchParam != null && searchParam.trim().length() >= 3) {
            String search = "%" + searchParam.trim().toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(root.get("priorityName")), search));
        }

        return predicates.toArray(new Predicate[0]);
    }


    @Override
    public PrioritySettingResponse updatePrioritySetting(UUID publicId, PrioritySettingRequest prioritySettingRequest, Principal principal) {
        Person updatedBy = getPerson(principal.getName());
        PrioritySetting prioritySetting = getPrioritySetting(publicId);
        PrioritySetting prioritySetting1 = prioritySettingRepository.findByPriorityNameIgnoreCaseAndDeletedIsFalse(prioritySettingRequest.priorityName()).orElse(null);
        if (prioritySetting1 != null && !prioritySetting.getPriorityName().equals(prioritySetting1.getPriorityName())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Priority name already exists")
                    .userMessage("Sorry, priority name already exists");
        }

        prioritySetting.setPriorityName(prioritySettingRequest.priorityName().trim());
        prioritySetting.setSlaType(prioritySettingRequest.slaType());
        prioritySetting.setSlaDuration(prioritySettingRequest.slaDuration());
        prioritySetting.setColorName(prioritySettingRequest.colorName());
        prioritySetting.setCreatedBy(updatedBy);
        return prioritySettingToResponse.convert(prioritySettingRepository.save(prioritySetting));
    }

    @Override
    public PrioritySettingResponse fetchPrioritySetting(UUID publicId) {
        Optional<PrioritySetting> optionPrioritySetting = prioritySettingRepository.findByPublicIdAndDeletedIsFalse(publicId);
        if (optionPrioritySetting.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Priority not found")
                    .userMessage("Sorry, priority not found");
        }

        return prioritySettingToResponse.convert(optionPrioritySetting.get());
    }

    @Override
    public void deletePrioritySetting(UUID publicId, Principal principal) {
        Person updatedBy = getPerson(principal.getName());
        PrioritySetting prioritySetting = getPrioritySetting(publicId);
        prioritySetting.setDeleted(true);
        prioritySetting.setUpdatedBy(updatedBy);
        prioritySettingRepository.save(prioritySetting);
    }

    public PrioritySetting getPrioritySetting(UUID publicId) {
        Optional<PrioritySetting> optionPrioritySetting = prioritySettingRepository.findByPublicIdAndDeletedIsFalse(publicId);
        if (optionPrioritySetting.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Priority not found")
                    .userMessage("Sorry, priority not found");
        }

        return optionPrioritySetting.get();
    }

    @Override
    public PrioritySetting actionOnPrioritySetting(UUID publicId, Boolean isActive, Principal principal) {
        Person updatedBy = getPerson(principal.getName());
        if (isActive == null) {
            isActive = Boolean.FALSE;
        }
        PrioritySetting prioritySetting = prioritySettingRepository.findByPublicIdAndDeletedIsFalse(publicId).orElse(null);
        if (prioritySetting == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("priority type not found")
                    .userMessage("Sorry, priority type not found");
        }
        prioritySetting.setIsActive(isActive);
        prioritySetting.setUpdatedBy(updatedBy);

        return prioritySettingRepository.save(prioritySetting);
    }

    @Override
    public CustomerExperience fetchCustomerExperience(UUID publicId, Principal principal) {
        CustomerExperience customerExperience = customerExperienceRepository.findByPublicIdAndDeletedIsFalse(publicId).orElse(null);
        if (customerExperience == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer experience not found")
                    .userMessage("Sorry, Customer experience not found");
        }

        return customerExperience;
    }

    @Override
    public CustomerExperience actionOnCustomerExperience(UUID publicId, Boolean isActive, Principal principal) {
        Person updatedBy = getPerson(principal.getName());
        CustomerExperience customerExperience = fetchCustomerExperience(publicId, principal);

        if (isActive == null) {
            isActive = Boolean.FALSE;
        }

        customerExperience.setIsActive(isActive);
        customerExperience.setUpdatedBy(updatedBy);

        return customerExperienceRepository.save(customerExperience);
    }

    @Override
    public CustomerExperience updateCustomerExperience(UUID publicId, CustomerExperienceRequest request, Principal principal) {
        Person createdBy = getPerson(principal.getName());
        ObjectMapper mapper = new ObjectMapper();
        CustomerExperience customerExperience1 = customerExperienceRepository.findByPublicIdAndDeletedIsFalse(publicId).orElse(null);

        if (customerExperience1 == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer experience not found")
                    .userMessage("Sorry, Customer experience found");
        }

        IssueType issueType = issueTypeRepository.findByPublicIdAndDeletedIsFalse(request.issueTypePublicId()).orElse(null);

        if (issueType == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Issue type not found")
                    .userMessage("Sorry, Issue type found");
        }

        List<Department> department = new ArrayList<>();
        if (request.departmentInCopyPublicId() != null && !request.departmentInCopyPublicId().isEmpty()) {
            for (UUID uuid : request.departmentInCopyPublicId()) {
                Department foundDepartment = departmentRepository.findByPublicIdAndDeletedIsFalse(uuid).orElse(null);
                if (foundDepartment == null) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Department not found")
                            .userMessage("Sorry, department found");
                }
                department.add(foundDepartment);
            }
        }

        customerExperience1.setTemplateName(request.templateName());
        customerExperience1.setIssueType(issueType);
        if (!department.isEmpty()) {
            customerExperience1.getDepartmentInCopy().clear();
            customerExperience1.getDepartmentInCopy().addAll(department);
        }
        customerExperience1.setPocEmail(request.pocEmail());
        customerExperience1.setIsActive(Boolean.TRUE);
        customerExperience1.setCreatedBy(createdBy);
        customerExperience1.setAllowNotification(request.allowNotification());

        customerExperience1.setAssignTo(request.assignTo() != null ? request.assignTo() : AssignToEnum.department);
        if (!request.emailInCopy().isEmpty()) {
            customerExperience1.setEmailInCopy(request.emailInCopy().stream().collect(Collectors.joining(",")));
        }

        customerExperience1.setCustomFields(mapper.valueToTree(request.customFields()));

        return customerExperienceRepository.save(customerExperience1);
    }

    @Override
    public void actionDeleteTemplate(UUID publicId, Principal principal) {
        Person updatedBy = getPerson(principal.getName());
        CustomerExperience customerExperience = customerExperienceRepository.findByPublicIdAndDeletedIsFalse(publicId).orElse(null);
        if (customerExperience == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Template not found.").userMessage("Sorry, template not found.");
        }
        customerExperience.setDeleted(true);
        customerExperience.setUpdatedBy(updatedBy);
        customerExperienceRepository.save(customerExperience);
    }

    @Override
    public CustomerNotesDTO addCustomerNotes(UUID customerPublicId, CustomerNotesRequest customerNotesRequest, Principal principal) {

        Customers customers = customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId).orElse(null);
        if (customers == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Mteja hapatikani")
                    .userMessage("Samahani, mteja hapatikani");
        }
        Person person = getPerson(principal.getName());
        CustomerNotes customerNotes = new CustomerNotes();

        if (customerNotesRequest.noteType().equals(NoteType.CS_NOTES)) {
            if (customerNotesRequest.reasonCodePublicId() == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Missing reason code")
                        .userMessage("Sorry, Select reason code");
            }

            RejectionsCode rejectionsCode = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(customerNotesRequest.reasonCodePublicId()).orElse(null);
            if (rejectionsCode == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Reason code not found")
                        .userMessage("Sorry, Reason code not found");
            }
            customerNotes.setReasonCode(rejectionsCode);

        }

        customerNotes.setCreatedBy(person);
        customerNotes.setNoteType(customerNotesRequest.noteType());
        customerNotes.setContent(customerNotesRequest.content());
        customerNotes.setCustomer(customers);

        CustomerNotes saveCustomerNote = customerNotesRepository.save(customerNotes);
        if (saveCustomerNote.getNoteType().equals(NoteType.CS_NOTES)) {
            sendNotificationToGChart(saveCustomerNote, person.getFullName());
        }
        return customerNotesToResponse.convert(saveCustomerNote);
    }

    private void sendNotificationToGChart(CustomerNotes saveCustomerNote, String actionedBy) {
        ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");
        LocalDateTime nairobiDate = LocalDateTime.now(nairobiZone);
        String actionTime = DateFormatter.formatDateToEastAfrica(nairobiDate);

        String customerName = saveCustomerNote.getCustomer().getName();
        String customerPhone = saveCustomerNote.getCustomer().getPhoneNumber();
        String notes = saveCustomerNote.getReasonCode().getCode();
        String description = saveCustomerNote.getReasonCode() != null ? String.format("-%s", saveCustomerNote.getReasonCode().getDescription()) : "";
        String department = saveCustomerNote.getReasonCode() != null ? (saveCustomerNote.getReasonCode().getDepartment() != null ? saveCustomerNote.getReasonCode().getDepartment().getName() : null) : null;

        String webHookTicketCreatedMessage = String.format("[%s] %s added note “%s%s” to customer %s (%s).",
                actionTime, actionedBy, notes, description, customerName, customerPhone);
        String payload = String.format("{\"text\": \"%s\"}", webHookTicketCreatedMessage);

        if (department != null) {
            webhookClient.acceptAndSend(department, payload);
        }
    }

    @Override
    public CustomerNotesDTO updateCustomerNotes(UUID publicId, CustomerNotesRequest customerNotesRequest, Principal principal) {
        Person person = getPerson(principal.getName());

        CustomerNotes customerNotes = validateCustomerNote(publicId);

        if (customerNotesRequest.noteType().equals(NoteType.CS_NOTES)) {
            if (customerNotesRequest.reasonCodePublicId() == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Missing reason code")
                        .userMessage("Sorry, Select reason code");
            }

            RejectionsCode rejectionsCode = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(customerNotesRequest.reasonCodePublicId()).orElse(null);
            if (rejectionsCode == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Reason code not found")
                        .userMessage("Sorry, Reason code not found");
            }
            customerNotes.setReasonCode(rejectionsCode);
        }

        customerNotes.setNoteType(customerNotesRequest.noteType());
        customerNotes.setContent(customerNotesRequest.content());
        customerNotes.setUpdatedBy(person);
        customerNotes.preUpdate();
        return customerNotesToResponse.convert(customerNotesRepository.save(customerNotes));
    }

    @Override
    public CustomerNotesDTO getCustomerNotesDTO(UUID publicId, Principal principal) {
        return customerNotesToResponse.convert(validateCustomerNote(publicId));
    }

    @Override
    public Page<CustomerNotesDTO> listCustomerNotes(UUID publicId, Pageable pageable) {
        Customers customers = customersRepository.findByPublicIdAndDeletedIsFalse(publicId).orElse(null);
        if (customers == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Mteja hapatikani")
                    .userMessage("Samahani, mteja hapatikani");
        }
        return customerNotesRepository.findAllByCustomerAndDeletedIsFalse(customers, pageable).map(customerNotesToResponse::convert);
    }

    @Override
    public Map<String, String> deleteCustomerNotes(UUID publicId, Principal principal) {
        Person person = getPerson(principal.getName());

        CustomerNotes customerNotes = validateCustomerNote(publicId);
        customerNotes.setUpdatedBy(person);
        customerNotes.setDeleted(true);
        CustomerNotes saveCustomerNotes = customerNotesRepository.save(customerNotes);

        Map<String, String> response = Map.of("message", String.format("Customer note of id (%d) deleted successfully.", saveCustomerNotes.getId()));
        return response;
    }

    @Override
    public SmsTemplateResponse createSmsTemplateRequest(SmsTemplateRequest smsTemplateRequest, Principal principal) {
        Person actionedBy = getPerson(principal.getName());
        verifyNameExists(smsTemplateRequest.name());
        SmsTemplate smsTemplate = new SmsTemplate();
        smsTemplate.setName(smsTemplateRequest.name());
        smsTemplate.setSmsChannel(smsTemplateRequest.smsChannel());
        smsTemplate.setMessage(smsTemplateRequest.message());
        smsTemplate.setActive(true);
        smsTemplate.setCreatedBy(actionedBy);
        return covertToResponse(smsTemplateRepository.save(smsTemplate));
    }

    @Override
    public Page<SmsTemplateResponse> getSmsTemplates(String searchParam, Boolean status, SmsChannel channel, Pageable pageable, Principal principal) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<SmsTemplate> cq = cb.createQuery(SmsTemplate.class);
        Root<SmsTemplate> root = cq.from(SmsTemplate.class);
        Predicate[] predicates = buildSmsTemplatePredicates(cb, root, searchParam, status, channel);

        cq.where(predicates).orderBy(cb.desc(root.get("id")));

        TypedQuery<SmsTemplate> query = entityManager.createQuery(cq)
                .setMaxResults(pageable.getPageSize())
                .setFirstResult((int) pageable.getOffset());

        List<SmsTemplate> tickets = query.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<SmsTemplate> countRoot = countQuery.from(SmsTemplate.class);
        Predicate[] countPredicates = buildSmsTemplatePredicates(cb, countRoot, searchParam, status, channel);

        countQuery.select(cb.countDistinct(countRoot)).where(countPredicates);
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        List<SmsTemplateResponse> dtoList = tickets.stream()
                .map(this::covertToResponse)
                .toList();

        return new PageImpl<>(dtoList, pageable, total);
    }

    @Override
    public SmsTemplateResponse updateSmsTemplateRequest(UUID publicId, SmsTemplateRequest smsTemplateRequest, Principal principal) {
        Person actionedBy = getPerson(principal.getName());
        SmsTemplate smsTemplate = getSmsTemplate(publicId);
        if (!smsTemplate.getName().equalsIgnoreCase(smsTemplateRequest.name()) && smsTemplateRepository.existsByNameIgnoreCaseAndDeletedIsFalse(smsTemplateRequest.name())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Template name already exists")
                    .userMessage("Sorry, template name already exits");
        }

        smsTemplate.setName(smsTemplateRequest.name());
        smsTemplate.setSmsChannel(smsTemplateRequest.smsChannel());
        smsTemplate.setMessage(smsTemplateRequest.message());
        smsTemplate.setUpdatedBy(actionedBy);
        return covertToResponse(smsTemplateRepository.save(smsTemplate));
    }

    @Override
    public void deleteSmsTemplateRequest(UUID publicId, Principal principal) {
        Person actionedBy = getPerson(principal.getName());
        SmsTemplate smsTemplate = getSmsTemplate(publicId);
        smsTemplate.setDeleted(true);
        smsTemplate.setUpdatedBy(actionedBy);
        smsTemplateRepository.save(smsTemplate);
    }

    @Override
    public void actionSmsTemplateRequest(UUID publicId, Principal principal) {
        Person actionedBy = getPerson(principal.getName());
        SmsTemplate smsTemplate = getSmsTemplate(publicId);
        smsTemplate.setActive(smsTemplate.isActive() == true ? false : true);
        smsTemplate.setUpdatedBy(actionedBy);
        smsTemplateRepository.save(smsTemplate);
    }

    @Override
    public void sendCustomerNotesToDepartmentOfficers() throws IOException {
        List<Department> departmentList = departmentRepository.findByDeletedIsFalse();
        for (Department department : departmentList) {
            List<CustomerNotes> customerNotesList = customerNotesRepository.findByNoteTypeAndNotificationSendIsFalseAndDeletedIsFalseAndReasonCode_Department(NoteType.CS_NOTES, department);

            if (!customerNotesList.isEmpty()) {
                File toFile = csNotesToExcel.convertToFile(customerNotesList, department.getName());

                sendNotificationToDepartmentOfficer(toFile, department);

                updateListings(customerNotesList);
            }
        }
    }

    private void updateListings(List<CustomerNotes> customerNotesList) {
        if (!customerNotesList.isEmpty()) {
            for (CustomerNotes customerNotes : customerNotesList) {
                customerNotes.setNotificationSend(true);
                customerNotesRepository.save(customerNotes);
            }
        }
    }

    private void sendNotificationToDepartmentOfficer(File toFile, Department department) {
        List<Person> personList = personRepository.findByDepartmentAndDeletedIsFalse(department);
        if (!personList.isEmpty()) {
            for (Person person : personList) {
                sendEmail(person.getEmail(), person.getFullName(), toFile, department.getName());
            }
        }
    }

    private void sendEmail(String emailTo, String recipientFullName, File toFile, String departmentName) {
        String message = String.format(
                "Please find attached the Churned Customers Report. This report has been tagged to you as members of the %s Department and is pending your action.\n" +
                        "\n" +
                        "Kindly review the report and take the necessary steps at the earliest convenience.\n" +
                        "\n" +
                        "Best regards.",
                departmentName);
        String emailBody = emailBodyCrafter.craftDepartment2Email(recipientFullName, message);
        emailService.sendMessage(emailTo, "Churned Customer CS Notes Notification", toFile, emailBody, Collections.emptyList());
    }

    private SmsTemplate getSmsTemplate(UUID publicId) {
        return smsTemplateRepository.findByPublicIdAndDeletedIsFalse(publicId)
                .orElseThrow(() -> new BadRequestRestApiException().developerMessage("Sms template not found").userMessage("Sorry, sms template not found."));
    }

    private Predicate[] buildSmsTemplatePredicates(CriteriaBuilder cb, Root<SmsTemplate> root, String searchParam, Boolean status, SmsChannel smsChannel) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("deleted"), Boolean.FALSE));

        if (status != null) {
            predicates.add(cb.equal(root.get("active"), status));
        }

        if (smsChannel != null) {
            predicates.add(cb.equal(root.get("smsChannel"), smsChannel));
        }

        if (searchParam != null && searchParam.trim().length() >= 3) {
            String search = "%" + searchParam.toUpperCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.upper(root.get("name")), search)
            ));
        }
        return predicates.toArray(new Predicate[0]);
    }

    private SmsTemplateResponse covertToResponse(SmsTemplate savedSmsTemplate) {
        return SmsTemplateResponse.builder()
                .dateCreated(DateFormatter.formatDate(savedSmsTemplate.getCreationTime()))
                .publicId(savedSmsTemplate.getPublicId())
                .createdBy(savedSmsTemplate.getCreatedBy().getFullName())
                .name(savedSmsTemplate.getName())
                .channel(savedSmsTemplate.getSmsChannel().name())
                .message(savedSmsTemplate.getMessage())
                .active(savedSmsTemplate.isActive())
                .build();
    }

    private void verifyNameExists(@NotEmpty String name) {
        if (smsTemplateRepository.existsByNameIgnoreCaseAndDeletedIsFalse(name)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sms name %s requested already exists")
                    .userMessage("Sorry, Sms name %s requested already exists");
        }
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

    private CustomerNotes validateCustomerNote(UUID publicId) {
        CustomerNotes customerNotes = customerNotesRepository.findByPublicIdAndDeletedIsFalse(publicId).orElse(null);
        if (customerNotes == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer notes not found")
                    .userMessage("Sorry, customer notes not found");
        }
        return customerNotes;
    }
}
