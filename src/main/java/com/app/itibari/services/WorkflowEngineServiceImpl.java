package com.app.itibari.services;

import com.africastalking.AfricasTalking;
import com.africastalking.SmsService;
import com.africastalking.sms.Recipient;
import com.app.itibari.converter.Converter;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.*;
import com.app.itibari.models.response.HelpDeskDTOResponse;
import com.app.itibari.models.response.TransitionMinifiedResponse;
import com.app.itibari.repository.*;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.formatter.DateFormatter;
import com.app.itibari.utils.helpers.EmailBodyCrafter;
import com.app.itibari.utils.helpers.WebhookClient;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowEngineServiceImpl implements WorkflowEngineService {

    private final HelpDeskRepository helpDeskRepository;
    private final WorkflowStatusRepository workflowStatusRepository;
    private final CustomerExperienceRepository customerExperienceRepository;
    private final TumaSmsRepository tumaSmsRepository;
    @PersistenceContext
    private EntityManager entityManager;
    private final WorkflowRepository workflowRepository;
    private final TransitionRepository transitionRepository;
    private final TicketRepository ticketRepository;
    private final PersonRepository personRepository;
    private final DepartmentRepository departmentRepository;
    private final Converter<HelpDesk, HelpDeskDTOResponse> converter;

    private final EmailService emailService;
    private final EmailBodyCrafter emailBodyCrafter;
    private final HelpDeskNotificationRepository helpDeskNotificationRepository;
    private final TicketHistoryRepository ticketHistoryRepository;

    private final WebhookClient webhookClient;

    @Value("${africastalking.senderId}")
    private String senderId;

    private Map<Long, Workflow> workflows = new HashMap<>();
    private Map<Long, Ticket> tickets = new HashMap<>();

    @Override
    public Workflow addWorkflow(WorkflowRequest workflowRequest, Principal principal) {
        Person person = getPerson(principal.getName());
        Workflow workflow = new Workflow();
        List<Transition> transitionList = new ArrayList<>();

        Department department = departmentRepository.findByPublicIdAndDeletedIsFalse(workflowRequest.assignedDepartmentPublicId()).orElse(null);
        if (department == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Department not found")
                    .userMessage("Department not found");
        }

        CustomerExperience customerExperience = customerExperienceRepository.findByPublicIdAndDeletedIsFalse(workflowRequest.templatePublicId()).orElse(null);
        if (customerExperience == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("template from not found")
                    .userMessage("template from not found");
        }

        Set<WorkflowStatus> workflowStatus = workflowStatusRepository.findByNameIgnoreCaseAndTemplateAndDeletedIsFalseAndDepartment("new", customerExperience, department);

        List<WorkflowRequest.Transitions> newTransitions = new ArrayList<>();

        for (WorkflowRequest.Transitions transitions : workflowRequest.transitionsList()) {
            if (transitions.index() == 0 && !workflowStatus.isEmpty()) {
                UUID fromNew = workflowStatus.stream().findFirst().get().getPublicId();
                UUID toId = transitions.from();
                WorkflowRequest.Transitions newBuild = WorkflowRequest.Transitions.builder()
                        .from(fromNew)
                        .to(toId)
                        .durationNumber("0")
                        .index(-1)
                        .build();
                newTransitions.add(newBuild);
            }
        }

        workflowRequest.transitionsList().addAll(newTransitions);

        List<WorkflowRequest.Transitions> newTransitionList = new ArrayList<>();
        for (WorkflowRequest.Transitions transitions : workflowRequest.transitionsList()) {
            int newIndex = transitions.index() + 1;

            WorkflowRequest.Transitions updated = WorkflowRequest.Transitions.builder()
                    .from(transitions.from())
                    .to(transitions.to())
                    .index(newIndex)
                    .durationNumber(transitions.durationNumber())
                    .build();
            newTransitionList.add(updated);
        }

        for (WorkflowRequest.Transitions transitions : newTransitionList) {

            StringBuilder stringBuilder = new StringBuilder();
            WorkflowStatus workflowStatusFrom = workflowStatusRepository.findByPublicIdAndDeletedIsFalse(transitions.from()).orElse(null);
            if (workflowStatusFrom == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Status from not found")
                        .userMessage("Status from not found");
            }
            stringBuilder.append("TRANSIT_").append(workflowStatusFrom.getName().toUpperCase());

            WorkflowStatus workflowStatusTo = workflowStatusRepository.findByPublicIdAndDeletedIsFalse(transitions.to()).orElse(null);
            if (workflowStatusTo == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Status to not found")
                        .userMessage("Status to not found");
            }

            stringBuilder.append("_").append(workflowStatusTo.getName().toUpperCase());

            Transition transition = new Transition();
            transition.setStatusFrom(workflowStatusFrom);
            transition.setStatusTo(workflowStatusTo);
            transition.setName(stringBuilder.toString());
            transition.setDurationNumber(transitions.durationNumber());
            transition.setTransitionIndex(transitions.index());
            Transition saveTransition = transitionRepository.save(transition);
            transitionList.add(saveTransition);
        }

        workflow.getTransitions().addAll(transitionList);
        workflow.setTemplate(customerExperience);
        workflow.setName(customerExperience.getTemplateName());
        workflow.setCreatedBy(person);
        workflow.setAssignedDepartment(department);

        return workflowRepository.save(workflow);
    }

    @Override
    public ResponseEntity<List<Workflow>> fetchWorkflows() {
        return ResponseEntity.ok(workflowRepository.findAll());
    }

    @Override
    public void executeTransition(UUID ticketId, UUID transitionPublicId, Principal principal) {
        Person person = getPerson(principal.getName());
        HelpDesk helpDesk = helpDeskRepository.findByPublicId(ticketId).orElse(null);

        final String currentDepartment = helpDesk.getWorkflowStatus();
        final String currentStatus = helpDesk.getWorkflowNewStatus();

        if (helpDesk == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Ticket not found")
                    .userMessage("Sorry, Ticket not found");
        }

        CustomerExperience customerExperience = customerExperienceRepository.findByPublicIdAndDeletedIsFalse(helpDesk.getTemplatePublicId()).orElse(null);
        if (customerExperience == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Missing Template")
                    .userMessage("Sorry, Missing Template");
        }

        Workflow wf = workflowRepository.findTopByTemplateAndDeletedIsFalseOrderByCreationTimeDesc(customerExperience).orElse(null);
        if (wf == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Workflow not found")
                    .userMessage("Samahi, workflow haipo");
        }

        Transition transition = wf.getTransitions().stream()
                .filter(t -> {
                    return t.getPublicId().equals(transitionPublicId)
                            && t.getStatusFrom().getName().equalsIgnoreCase(helpDesk.getWorkflowNewStatus());
                })
                .findFirst()
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Wrong transition")
                        .userMessage("Sorry, wrong transition"));

        boolean sameDepartment = transition.getStatusTo().getDepartment().getName().equalsIgnoreCase(helpDesk.getWorkflowStatus());
        if (transition.getStatusTo().getName().equalsIgnoreCase("RESOLVE") ||
                transition.getStatusTo().getName().equalsIgnoreCase("RESOLVED")) {
//            if (!transition.getStatusFrom().getDepartment().getName().equalsIgnoreCase("Customer Service") &&
//                    !transition.getStatusFrom().getDepartment().getName().equalsIgnoreCase("CX")) {
//                throw new BadRequestRestApiException()
//                        .developerMessage("Tick not yet at Customer Service department")
//                        .userMessage("Sorry, Tick not yet at Customer Service department");
//            }

            if (transition.getStatusFrom().getDepartment().getName().equalsIgnoreCase("NEW")) {
                throw new BadRequestRestApiException()
                        .developerMessage("Tick can not be closed at stage New")
                        .userMessage("Sorry, Tick can not be closed at stage New");
            }
        }

        helpDesk.setWorkflowStatus(transition.getStatusTo().getDepartment().getName());
        helpDesk.setWorkflowNewStatus(transition.getStatusTo().getName());

        HelpDesk saveTicket = helpDeskRepository.save(helpDesk);
        HelpDeskDTOResponse helpDeskDTOResponse = converter.convert(saveTicket);

        updateTransitionAllocatedTime(helpDesk, wf.getTransitions());

        if (!sameDepartment) {
            saveTicket.setRead(Boolean.FALSE);
            HelpDesk saveDesk = helpDeskRepository.save(saveTicket);

            String notificationHeader = String.format("Ticket %s, Assigned pending your action.", saveTicket.getTicketNumber());

            List<HelpDeskDTOResponse.DepartmentInCopy> departmentInCopyList = helpDeskDTOResponse.departmentInCopy();
            List<HelpDeskDTOResponse.DepartmentInCopy> departInCopy = new ArrayList<>();
            if (!departmentInCopyList.isEmpty()) {
                departInCopy.addAll(departmentInCopyList);
            }

            Department department = departmentRepository.findByNameIgnoreCaseAndDeletedIsFalse(transition.getStatusTo().getDepartment().getName()).orElse(null);
            if (department == null) {
                throw new BadRequestRestApiException()
                        .developerMessage(String.format("Department %s not found", transition.getStatusTo().getName()))
                        .userMessage(String.format("Department %s not found", transition.getStatusTo().getName()));
            }
            recordTicketHistory(saveDesk, "Ticket Transition", currentDepartment, currentStatus, person.getFullName());
            createHelpDeskNotificationToGroup(department, notificationHeader, departInCopy);
            dropMessageToTransitedDepartmentGChat(department, helpDesk.getTicketNumber(), helpDesk.getResolvedDate(), helpDesk.getTemplate().getIssueType().getName());
        }
        if (saveTicket.getWorkflowNewStatus().equalsIgnoreCase("RESOLVE") ||
                saveTicket.getWorkflowNewStatus().equalsIgnoreCase("RESOLVED")) {
            runSendMessageToCustomer(saveTicket.getHelpDeskCustomer(), saveTicket.getTicketNumber(), customerExperience.isAllowNotification());
        }
    }

    private void updateTransitionAllocatedTime(HelpDesk helpDesk, List<Transition> transitions) {
        ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");
        LocalDateTime nairobiDate = LocalDateTime.now(nairobiZone);

        List<Transition> list = transitions.stream()
                .filter(t -> t.getStatusTo().getDepartment().getName()
                        .equalsIgnoreCase(helpDesk.getWorkflowStatus()))
                .collect(Collectors.toList());

        if (!list.isEmpty()) {

            list.sort(Comparator.comparing(
                    Transition::getTransitionIndex,
                    Comparator.nullsLast(Comparator.naturalOrder())
            ));

            Transition transition = list.get(list.size() - 1);
            transition.setTicketArrivalTime(nairobiDate);
            transitionRepository.save(transition);
        }
    }

    private void dropMessageToTransitedDepartmentGChat(Department section, String ticketNumber, LocalDateTime resolvedDate, String issueType) {
        String safeTicketNumber = ticketNumber != null ? ticketNumber : "N/A";
        String safeIssueType = issueType != null ? issueType : "Unknown Issue";
        String safeDepartment = (section != null && section.getName() != null) ? section.getName() : "Unknown Department";
        String expectedResolveDate = resolvedDate != null ? DateFormatter.formatDate(resolvedDate) : "Unspecified Date";

        String webHookTicketCreatedMessage;
        webHookTicketCreatedMessage = String.format(
                "Ticket Number - %s of issue Type ( %s ), Assigned to Department %s. Ticket set to be solved on %s",
                safeTicketNumber,
                safeIssueType,
                safeDepartment,
                expectedResolveDate
        );

        String payload = String.format("{\"text\": \"%s\"}", webHookTicketCreatedMessage);

        webhookClient.acceptAndSend(section.getName(), payload);
    }

    private void createHelpDeskNotificationToGroup(Department foundDepartment, String notificationHeader, List<HelpDeskDTOResponse.DepartmentInCopy> departInCopy) {
        List<String> emailToCc = new ArrayList<>();

        for (HelpDeskDTOResponse.DepartmentInCopy departmentInCopy : departInCopy) {
            Department department = departmentRepository.findByPublicIdAndDeletedIsFalse(departmentInCopy.publicId()).orElse(null);
            if (department != null) {
                List<Person> allUsersInRoleDep = personRepository.findByDepartmentAndDeletedIsFalse(department);
                if (!allUsersInRoleDep.isEmpty()) {
                    allUsersInRoleDep.forEach(item -> {
                        emailToCc.add(item.getEmail());
                    });
                }
            }
        }

        List<Person> allUsersInRole = personRepository.findByDepartmentAndDeletedIsFalse(foundDepartment);

        if (!allUsersInRole.isEmpty()) {
            if (allUsersInRole.size() == 1) {
                String emailBody = emailBodyCrafter.craftDepartmentEmail(allUsersInRole.stream().findFirst().get().getFirstName(), notificationHeader);
                emailService.sendMessage(allUsersInRole.stream().findFirst().get().getEmail(), "Ticket Allocation", null, emailBody, emailToCc);
                createHelpDeskNotification(allUsersInRole.stream().findFirst().get().getPublicId(), notificationHeader);
            } else {
                Person person = allUsersInRole.stream().findFirst().get();
                allUsersInRole.remove(person);

                String emailBody = emailBodyCrafter.craftSecondDepartmentEmail(notificationHeader);
                for (Person person1 : allUsersInRole) {
                    emailToCc.add(person1.getEmail());
                }

                emailService.sendMessage(person.getEmail(), "Ticket Allocation", null, emailBody, emailToCc);
            }
        }
    }

    private void createHelpDeskNotification(UUID publicId, String notificationHeader) {
        HelpDeskNotification newHelpNotification = new HelpDeskNotification();
        newHelpNotification.setRead(Boolean.FALSE);
        newHelpNotification.setMessageAction(notificationHeader);
        newHelpNotification.setAssignedToPublicId(publicId);
        newHelpNotification.setCreationTime(LocalDateTime.now());
        helpDeskNotificationRepository.save(newHelpNotification);
    }

    @Override
    public ResponseEntity<Page<Workflow>> listWorkflows(
            String searchParam, LocalDate startDate, LocalDate endDate, Principal principal, UUID templatePublicId, UUID issueTypePublicId, Pageable pageable) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Workflow> cq = cb.createQuery(Workflow.class);
        Root<Workflow> root = cq.from(Workflow.class);
        Predicate[] predicates = buildPredicates(cb, root, searchParam, startDate, endDate, templatePublicId, issueTypePublicId);

        cq.where(predicates).orderBy(cb.desc(root.get("id")));

        TypedQuery<Workflow> query = entityManager.createQuery(cq)
                .setMaxResults(pageable.getPageSize())
                .setFirstResult((int) pageable.getOffset());

        List<Workflow> workflows = query.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Workflow> countRoot = countQuery.from(Workflow.class);
        Predicate[] countPredicates = buildPredicates(cb, countRoot, searchParam, startDate, endDate, templatePublicId, issueTypePublicId);

        countQuery.select(cb.countDistinct(countRoot)).where(countPredicates);
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        Page<Workflow> page = new PageImpl<>(workflows, pageable, total);

        return ResponseEntity.ok(page);
    }

    @Override
    public void addTicketStatus(UUID templatePublicId, StatusRequest statusRequest, UUID departmentPublicId, Principal principal) {
        Department department = departmentRepository.findByPublicIdAndDeletedIsFalse(departmentPublicId).orElse(null);
        if (department == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Department not found")
                    .userMessage("Sorry, Department not found");
        }
        CustomerExperience customerExperience = customerExperienceRepository.findByPublicIdAndDeletedIsFalse(templatePublicId).orElse(null);
        if (customerExperience == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Missing customer experience")
                    .userMessage("Sorry, Missing customer experience");
        }

        Set<String> names = new HashSet<>();
        for (String name : statusRequest.names()) {
            name.trim();
            String result = name.replace(" ", "_").toUpperCase();
            names.add(result);
        }
        names.add("new");
//        names.add("resolve");

        if (!names.isEmpty()) {
            for (String name : names) {
                WorkflowStatus workflowStatus = new WorkflowStatus();
                workflowStatus.setName(name);
                workflowStatus.setDepartment(department);
                workflowStatus.setTemplate(customerExperience);
                workflowStatusRepository.save(workflowStatus);
            }
        }
    }

    @Override
    public Page<WorkflowStatus> listWorkflowStatuses(
            UUID templatePublicId,
            String searchParam,
            UUID departmentPublicId,
            Principal principal,
            Pageable pageable) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<WorkflowStatus> cq = cb.createQuery(WorkflowStatus.class);
        Root<WorkflowStatus> root = cq.from(WorkflowStatus.class);
        Predicate[] predicates = buildWorkflowStatusPredicates(cb, root, templatePublicId, searchParam, departmentPublicId);

        cq.where(predicates).orderBy(cb.desc(root.get("id")));

        TypedQuery<WorkflowStatus> query = entityManager.createQuery(cq)
                .setMaxResults(pageable.getPageSize())
                .setFirstResult((int) pageable.getOffset());

        List<WorkflowStatus> tickets = query.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<WorkflowStatus> countRoot = countQuery.from(WorkflowStatus.class);
        Predicate[] countPredicates = buildWorkflowStatusPredicates(cb, countRoot, templatePublicId, searchParam, departmentPublicId);

        countQuery.select(cb.count(countRoot)).where(countPredicates);
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(tickets, pageable, total);
    }


    private Predicate[] buildWorkflowStatusPredicates(CriteriaBuilder cb, Root<WorkflowStatus> root, UUID templatePublicId, String searchParam, UUID departmentPublicId) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("deleted"), Boolean.FALSE));

        if (templatePublicId != null) {
            predicates.add(cb.equal(root.get("template").get("publicId"), templatePublicId));
        }

        if (departmentPublicId != null) {
            predicates.add(cb.equal(root.get("department").get("publicId"), departmentPublicId));
        }

        if (searchParam != null && searchParam.trim().length() >= 3) {
            String search = "%" + searchParam.toUpperCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.upper(root.get("name")), search)
            ));
        }
        return predicates.toArray(new Predicate[0]);
    }

    @Override
    public Page<WorkflowStatus> listAllWorkflowStatuses(UUID templatePublicId, String searchParam, UUID departmentPublicId, Principal principal, Pageable pageable) {
        List<WorkflowStatus> workflowStatusList = listWorkflowStatuses(
                templatePublicId,
                searchParam,
                departmentPublicId,
                principal,
                pageable).stream().filter(item -> !item.getName().equalsIgnoreCase("new")).toList();

        return new PageImpl<>(workflowStatusList, pageable, workflowStatusList.size());
    }

    @Override
    public List<Transition> listTransitions(UUID ticketPublicId, Principal principal) {
        HelpDesk helpDesk = helpDeskRepository.findByPublicIdAndDeletedIsFalse(ticketPublicId).orElse(null);
        if (helpDesk == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Missing ticket")
                    .userMessage("Sorry, missing ticket");
        }

        UUID templatePublicId = helpDesk.getTemplatePublicId();
        CustomerExperience customerExperience = customerExperienceRepository.findByPublicIdAndDeletedIsFalse(templatePublicId).orElse(null);
        if (customerExperience == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Missing template")
                    .userMessage("Sorry, missing template");
        }

        Workflow workflow = workflowRepository.findTopByTemplateAndDeletedIsFalseOrderByCreationTimeDesc(customerExperience).orElse(null);
        if (workflow == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Missing workflow")
                    .userMessage("Sorry, missing workflow");
        }

        if (workflow.getTransitions().isEmpty()) {
            return new ArrayList<>();
        }
        return workflow.getTransitions();
    }

    @Override
    public void actionDeleteTicketStatus(UUID publicId, Principal principal) {
        Person deletedBy = getPerson(principal.getName());
        WorkflowStatus workflowStatus = workflowStatusRepository.findByPublicIdAndDeletedIsFalse(publicId).orElse(null);
        if (workflowStatus == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Missing status")
                    .userMessage("Sorry, missing status");
        }

        workflowStatus.setDeleted(true);
        workflowStatus.setUpdatedBy(deletedBy);
        workflowStatusRepository.save(workflowStatus);
    }

    @Override
    public void actionDeleteWorkflow(UUID publicId, Principal principal) {
        Person deletedBy = getPerson(principal.getName());
        Workflow workflow = workflowRepository.findByPublicIdAndDeletedIsFalse(publicId).orElse(null);
        if (workflow == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("workflow not found")
                    .userMessage("Sorry, workflow not found");
        }
        workflow.setDeleted(true);
        workflow.setUpdatedBy(deletedBy);
        workflowRepository.save(workflow);
    }

    @Override
    public void updateTicketStatus(UUID statusPublicId, UUID templatePublicId,
                                   StatusUpdateRequest statusUpdateRequest, Principal principal) {
        Person updatedBy = getPerson(principal.getName());

        CustomerExperience customerExperience = customerExperienceRepository
                .findByPublicIdAndDeletedIsFalse(templatePublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Template not found for publicId: " + templatePublicId)
                        .userMessage("Sorry, missing template"));

        WorkflowStatus workflowStatus = workflowStatusRepository
                .findByPublicIdAndTemplateAndDeletedIsFalse(statusPublicId, customerExperience)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Status not found for publicId: " + statusPublicId)
                        .userMessage("Sorry, status not found"));

        // Check for duplicate name against other statuses in the same template
        WorkflowStatus duplicateStatus = workflowStatusRepository
                .findByNameAndTemplateAndDeletedIsFalse(statusUpdateRequest.statusName(), customerExperience)
                .orElse(null);

        if (duplicateStatus != null && !duplicateStatus.getPublicId().equals(statusPublicId)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Status name '" + statusUpdateRequest.statusName() + "' already exists")
                    .userMessage("Sorry, status name already defined");
        }

        workflowStatus.setName(statusUpdateRequest.statusName());
        workflowStatus.setUpdatedBy(updatedBy);

        workflowStatusRepository.save(workflowStatus);
    }

    @Override
    public void actionDeleteTransition(UUID transitionPublicId, Principal principal) {
        Person updatedBy = getPerson(principal.getName());
        Transition transition = transitionRepository.findByPublicIdAndDeletedIsFalse(transitionPublicId).orElse(null);
        if (transition != null) {
            transition.setDeleted(true);
            transition.setUpdatedBy(updatedBy);
            transitionRepository.save(transition);
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Transition not found.")
                    .userMessage("Sorry, transition not found");
        }
    }

    @Override
    public Workflow updateWorkflowStep(UUID workflowPublicId, AddWorkflowRequest workflowRequest, Principal principal) {
        Workflow workflow = workflowRepository.findByPublicIdAndDeletedIsFalse(workflowPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Workflow not found")
                        .userMessage("Workflow not found"));

        Person person = getPerson(principal.getName());

        List<AddWorkflowRequest.Transitions> combinedTransitions = buildCombinedTransitions(workflow, workflowRequest);

        List<Transition> transitionList = combinedTransitions.stream()
                .map(t -> processTransition(t))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        transitionList.forEach(t -> {
            if (!workflow.getTransitions().contains(t)) {
                workflow.getTransitions().add(t);
            }
        });

        workflow.setCreatedBy(person);
        return workflowRepository.save(workflow);
    }

    private List<AddWorkflowRequest.Transitions> buildCombinedTransitions(Workflow workflow, AddWorkflowRequest workflowRequest) {
        List<AddWorkflowRequest.Transitions> combined = new ArrayList<>(workflowRequest.transitionsList());

        Set<WorkflowStatus> newStatuses = workflowStatusRepository
                .findByNameIgnoreCaseAndTemplateAndDeletedIsFalseAndDepartment(
                        "new", workflow.getTemplate(), workflow.getAssignedDepartment());

        if (!newStatuses.isEmpty()) {
            WorkflowStatus fromNew = newStatuses.iterator().next();
            workflowRequest.transitionsList().stream()
                    .filter(t -> t.index() == 0)
                    .forEach(t -> combined.add(AddWorkflowRequest.Transitions.builder()
                            .from(fromNew.getPublicId())
                            .to(t.from())
                            .durationNumber("0")
                            .oldPublicId(t.oldPublicId())
                            .build()));
        }
        return combined;
    }

    private Transition processTransition(AddWorkflowRequest.Transitions t) {
        WorkflowStatus from = workflowStatusRepository.findByPublicIdAndDeletedIsFalse(t.from())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Status from not found")
                        .userMessage("Status from not found"));

        WorkflowStatus to = workflowStatusRepository.findByPublicIdAndDeletedIsFalse(t.to())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Status to not found")
                        .userMessage("Status to not found"));

        String name = String.format("TRANSIT_%s_%s",
                from.getName().toUpperCase(),
                to.getName().toUpperCase());

        if (t.oldPublicId() == null) {
            return createTransition(from, to, name, t.durationNumber());
        }

        Transition existing = transitionRepository.findByPublicIdAndDeletedIsFalse(t.oldPublicId()).orElse(null);
        if (existing != null) {
            existing.setStatusFrom(from);
            existing.setStatusTo(to);
            existing.setName(name);
            existing.setDurationNumber(t.durationNumber());
            return transitionRepository.save(existing);
        }

        return createTransition(from, to, name, t.durationNumber());
    }

    private Transition createTransition(WorkflowStatus from, WorkflowStatus to, String name, String duration) {
        Transition transition = new Transition();
        transition.setStatusFrom(from);
        transition.setStatusTo(to);
        transition.setName(name);
        transition.setDurationNumber(duration);
        return transitionRepository.save(transition);
    }

    @Override
    public TransitionMinifiedResponse getTransitionMinifiedResponse(UUID ticketPublicId, Principal principal) {
        HelpDesk helpDesk = helpDeskRepository.findByPublicIdAndDeletedIsFalse(ticketPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("helpDesk not found")
                        .userMessage("helpDesk not found"));

        CustomerExperience customerExperience = customerExperienceRepository.findByPublicIdAndDeletedIsFalse(helpDesk.getTemplatePublicId()).orElse(null);
        if (customerExperience == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Missing Template")
                    .userMessage("Sorry, Missing Template");
        }

        Workflow wf = workflowRepository.findTopByTemplateAndDeletedIsFalseOrderByCreationTimeDesc(customerExperience).orElse(null);
        if (wf == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Workflow not found")
                    .userMessage("Sorry, workflow not found");
        }
        List<Transition> transitions = wf.getTransitions();

        List<TransitionMinifiedResponse.TransitionDTO> transitionDTOList =
                transitions.stream()
                        .filter(item -> item.getStatusFrom().getDepartment().getName()
                                .equalsIgnoreCase(helpDesk.getWorkflowStatus()))
                        .filter(item -> item.getStatusFrom().getName().equalsIgnoreCase(helpDesk.getWorkflowNewStatus()))
                        .map(transition -> TransitionMinifiedResponse.TransitionDTO.builder()
                                .publicId(transition.getPublicId())
                                .name(transition.getName())
                                .departmentName(transition.getStatusTo().getDepartment().getName())
                                .modifiedName(String.format("(%s) %s -> (%s) %s", transition.getStatusFrom().getDepartment().getName(), transition.getStatusFrom().getName(), transition.getStatusTo().getDepartment().getName(), transition.getStatusTo().getName()))
                                .build())
                        .toList();

        return TransitionMinifiedResponse.builder()
                .transitionDTOList(transitionDTOList)
                .build();
    }

    @Override
    public TransitionLineDTO getTransitionLinePointResponse(UUID ticketPublicId, Principal principal) {
        HelpDesk helpDesk = helpDeskRepository.findByPublicIdAndDeletedIsFalse(ticketPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("helpDesk not found")
                        .userMessage("helpDesk not found"));

        CustomerExperience customerExperience = customerExperienceRepository
                .findByPublicIdAndDeletedIsFalse(helpDesk.getTemplatePublicId())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Missing Template")
                        .userMessage("Sorry, Missing Template"));

        Workflow wf = workflowRepository
                .findTopByTemplateAndDeletedIsFalseOrderByCreationTimeDesc(customerExperience)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Workflow not found")
                        .userMessage("Samahi, workflow haipo"));

        List<TransitionLineDTO.TransitionLine> transitionLineList = new ArrayList<>();
        wf.getTransitions().stream()
                .sorted(Comparator.comparing(Transition::getTransitionIndex))
                .forEach(transition -> {
                    TransitionLineDTO.TransitionLine buildTransition = TransitionLineDTO.TransitionLine.builder()
                            .from(transition.getStatusFrom().getName())
                            .fromDepartment(transition.getStatusFrom().getDepartment().getName())
                            .index(transition.getTransitionIndex())
                            .to(transition.getStatusTo().getName())
                            .toDepartment(transition.getStatusTo().getDepartment().getName())
                            .duration(transition.getDurationNumber())
                            .build();
                    transitionLineList.add(buildTransition);
                });

        return TransitionLineDTO.builder().transitionLineList(transitionLineList).build();
    }

    @Override
    public Workflow addDefineWorkflow(UUID workflowPublicId, AddWorkflowRequest addWorkflowRequest, Principal principal) {
        Person person = getPerson(principal.getName());
        Workflow workflow = validateWorkflow(workflowPublicId);
        List<Transition> transitionList = new ArrayList<>();

        for (AddWorkflowRequest.Transitions transitions : addWorkflowRequest.transitionsList()) {
            StringBuilder stringBuilder = new StringBuilder();
            WorkflowStatus workflowStatusFrom = workflowStatusRepository.findByPublicIdAndDeletedIsFalse(transitions.from()).orElse(null);
            if (workflowStatusFrom == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Status from not found")
                        .userMessage("Status from not found");
            }
            stringBuilder.append("TRANSIT_").append(workflowStatusFrom.getName().toUpperCase());

            WorkflowStatus workflowStatusTo = workflowStatusRepository.findByPublicIdAndDeletedIsFalse(transitions.to()).orElse(null);
            if (workflowStatusTo == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Status to not found")
                        .userMessage("Status to not found");
            }

            stringBuilder.append("_").append(workflowStatusTo.getName().toUpperCase());

            Transition transition = new Transition();
            transition.setStatusFrom(workflowStatusFrom);
            transition.setStatusTo(workflowStatusTo);
            transition.setName(stringBuilder.toString());
            transition.setDurationNumber(transitions.durationNumber());
            Transition saveTransition = transitionRepository.save(transition);
            transitionList.add(saveTransition);
        }

        workflow.getTransitions().addAll(transitionList);
        workflow.setCreatedBy(person);

        return workflowRepository.save(workflow);
    }

    @Override
    public Page<Department> getDepartmentLists(UUID templatePublicId, UUID ticketPublicId, Principal principal, Pageable pageable) {
        HelpDesk helpDesk = helpDeskRepository.findByPublicIdAndDeletedIsFalse(ticketPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("helpDesk not found")
                        .userMessage("helpDesk not found"));

        CustomerExperience customerExperience = customerExperienceRepository.findByPublicIdAndDeletedIsFalse(templatePublicId).orElse(null);
        if (customerExperience == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Template not found")
                    .userMessage("Template not found");
        }

        List<WorkflowStatus> workflowStatuses = workflowStatusRepository.findByTemplateAndDeletedIsFalse(customerExperience, pageable).getContent();
        Set<Department> fetchedDepartment = new HashSet<>();
        workflowStatuses.forEach(item -> fetchedDepartment.add(item.getDepartment()));

        TransitionLineDTO transitionLineDTO = getTransitionLinePointResponse(ticketPublicId, principal);

        Optional<Integer> targetIndexOpt = transitionLineDTO.transitionLineList().stream()
                .filter(t -> helpDesk.getWorkflowNewStatus().equals(t.from()) &&
                        helpDesk.getWorkflowStatus().equals(t.fromDepartment()))
                .map(TransitionLineDTO.TransitionLine::index)
                .findFirst();

        List<Department> resultList = new ArrayList<>();

        if (targetIndexOpt.isPresent()) {
            int targetIndex = targetIndexOpt.get();

            Set<String> uniqueDepartments = transitionLineDTO.transitionLineList().stream()
                    .filter(t -> t.index() > targetIndex)
                    .map(TransitionLineDTO.TransitionLine::toDepartment)
                    .collect(Collectors.toSet());

            resultList = fetchedDepartment.stream()
                    .filter(dep -> !uniqueDepartments.contains(dep.getName()))
                    .toList();
        }

        return new PageImpl<>(resultList, pageable, resultList.size());
    }


    @Override
    public void hitReverseTransitionLinePointResponse(UUID ticketPublicId, Principal principal) {
        Person reversedBy = getPerson(principal.getName());
        HelpDesk helpDesk = helpDeskRepository.findByPublicIdAndDeletedIsFalse(ticketPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("helpDesk not found")
                        .userMessage("helpDesk not found"));

        TransitionLineDTO transitionLineDTO = getTransitionLinePointResponse(ticketPublicId, principal);

        String workflowCurrentStatus = helpDesk.getWorkflowNewStatus();
        String workflowCurrentDepartment = helpDesk.getWorkflowStatus();

        TransitionLineDTO.TransitionLine transitionLine = transitionLineDTO.transitionLineList()
                .stream()
                .filter(item -> item.toDepartment().equalsIgnoreCase(workflowCurrentDepartment)
                        && item.to().equalsIgnoreCase(workflowCurrentStatus))
                .findFirst()
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Matching transition not found")
                        .userMessage("Sorry, Matching transition not found"));

        helpDesk.setWorkflowNewStatus(transitionLine.from());
        helpDesk.setWorkflowStatus(transitionLine.fromDepartment());
        helpDesk.setUpdatedBy(reversedBy);
        helpDeskRepository.save(helpDesk);
    }

    @Override
    public List<Map<String, Object>> ticketSlaPerDepartment(UUID ticketPublicId, Principal principal) {

        TransitionLineDTO transitionLineDTO = getTransitionLinePointResponse(ticketPublicId, principal);

        List<Map<String, Object>> result = transitionLineDTO.transitionLineList().stream()
                .collect(Collectors.groupingBy(
                        TransitionLineDTO.TransitionLine::toDepartment,
                        Collectors.summingInt(t -> Integer.parseInt(t.duration()))
                ))
                .entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("department", entry.getKey());
                    map.put("duration", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());

        return result;
    }

    @Override
    public void reverseToDepartment(UUID ticketPublicId, ReverseRequest request, Principal principal) {
        Person reversedBy = getPerson(principal.getName());
        HelpDesk helpDesk = helpDeskRepository.findByPublicIdAndDeletedIsFalse(ticketPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("helpDesk not found")
                        .userMessage("Ticket not found"));

        CustomerExperience customerExperience = customerExperienceRepository.findByPublicIdAndDeletedIsFalse(helpDesk.getTemplatePublicId()).orElse(null);
        if (customerExperience == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Missing Template")
                    .userMessage("Sorry, Missing Template");
        }


        Workflow wf = workflowRepository.findTopByTemplateAndDeletedIsFalseOrderByCreationTimeDesc(customerExperience).orElse(null);
        if (wf == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Workflow not found")
                    .userMessage("Sorry , workflow not found");
        }

        TransitionLineDTO transitionLineDTO = getTransitionLinePointResponse(ticketPublicId, principal);

        String workflowCurrentDepartment = helpDesk.getWorkflowStatus();

        final String currentDepartment = helpDesk.getWorkflowStatus();
        final String currentStatus = helpDesk.getWorkflowNewStatus();

        if (request.department().equalsIgnoreCase(workflowCurrentDepartment)) {
            throw new BadRequestRestApiException()
                    .developerMessage("You can not transit to the same department")
                    .userMessage("Sorry, You can not transit to the same department");
        }

        updateTransitionAllocatedTime(helpDesk, wf.getTransitions());

        TransitionLineDTO.TransitionLine transitionLine = transitionLineDTO.transitionLineList()
                .stream()
                .filter(item -> item.fromDepartment().equalsIgnoreCase(request.department()))
                .min(Comparator.comparingInt(TransitionLineDTO.TransitionLine::index))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Matching transition not found")
                        .userMessage("Sorry, Matching transition not found"));


        helpDesk.setWorkflowNewStatus(transitionLine.from());
        helpDesk.setWorkflowStatus(transitionLine.fromDepartment());
        helpDesk.setUpdatedBy(reversedBy);
        helpDesk.setRead(Boolean.FALSE);
        HelpDesk saveHelpDesk = helpDeskRepository.save(helpDesk);

        recordTicketHistory(saveHelpDesk, request.reason(), currentDepartment, currentStatus, reversedBy.getFullName());
    }

    @Override
    public Page<Department> getDepartments(UUID templatePublicId, Principal principal, Pageable pageable) {
        CustomerExperience customerExperience = customerExperienceRepository
                .findByPublicIdAndDeletedIsFalse(templatePublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Template not found")
                        .userMessage("Template not found"));

        List<WorkflowStatus> workflowStatuses = workflowStatusRepository
                .findByTemplateAndDeletedIsFalse(customerExperience, pageable)
                .getContent();

        List<Department> departments = workflowStatuses.stream()
                .map(WorkflowStatus::getDepartment)
                .distinct()
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), departments.size());
        List<Department> pagedDepartments = departments.subList(start, end);

        return new PageImpl<>(pagedDepartments, pageable, departments.size());
    }


    private void recordTicketHistory(HelpDesk saveHelpDesk, @NotEmpty String reason, String currentDepartment, String currentStatus, String fullName) {
        TicketHistory newTicketHistory = new TicketHistory();
        newTicketHistory.setReason(reason);
        newTicketHistory.setActionedBy(fullName);
        newTicketHistory.setFromDepartment(currentDepartment);
        newTicketHistory.setToDepartment(saveHelpDesk.getWorkflowStatus());
        newTicketHistory.setStatusFrom(currentStatus);
        newTicketHistory.setStatusTo(saveHelpDesk.getWorkflowNewStatus());
        newTicketHistory.setActionedTime(LocalDateTime.now());
        newTicketHistory.setHelpDesk(saveHelpDesk);
        ticketHistoryRepository.save(newTicketHistory);

        if (!currentDepartment.equalsIgnoreCase(saveHelpDesk.getWorkflowStatus())) {
            String actionedBy = fullName.toUpperCase();
            String webHookTicketCreatedMessage = String.format(
                    "Ticket Number - %s of issue Type ( %s ), reversed from department %s to %s. Reason : (%s). Actioned by %s",
                    saveHelpDesk.getTicketNumber(),
                    saveHelpDesk.getIssueType() != null ? saveHelpDesk.getIssueType().getName() : "--",
                    currentDepartment.toUpperCase(),
                    saveHelpDesk.getWorkflowStatus().toUpperCase(),
                    reason,
                    actionedBy
            );

            String payload = String.format("{\"text\": \"%s\"}", webHookTicketCreatedMessage);
            webhookClient.acceptAndSend(saveHelpDesk.getWorkflowStatus(), payload);
        }
    }


    private Predicate[] buildPredicates(CriteriaBuilder cb, Root<Workflow> root, String searchParam, LocalDate startDate, LocalDate endDate, UUID templatePublicId, UUID issueTypePublicId) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.isFalse(root.get("deleted")));

        if (templatePublicId != null) {
            predicates.add(cb.equal(root.get("template").get("publicId"), templatePublicId));
        }

        if (issueTypePublicId != null) {
            predicates.add(cb.equal(root.get("template").get("issueType").get("publicId"), issueTypePublicId));
        }

        if (searchParam != null && searchParam.trim().length() >= 3) {
            String search = "%" + searchParam.trim().toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(root.get("name")), search));
        }

        return predicates.toArray(new Predicate[0]);
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

    private void runSendMessageToCustomer(HelpDeskCustomer helpDeskCustomer, String ticketNumber, boolean isAllowNotification) {
        String storeName = helpDeskCustomer.getStoreName();
        storeName = (storeName != null && !storeName.isEmpty()) ? storeName : "";

        String message = String.format("Mpendwa mteja %s, tiketi %s imetatuliwa na kufungwa. Asante kwa kuchagua Itibari", storeName, ticketNumber);

        if (!isAllowNotification) {
            message = String.format("Asante kwa kuwasiliana nasi. Tunathamini muda na imani yako kwetu.");
        }

        tumaUjumbeKwaMteja(helpDeskCustomer.getPhoneNumber(), message, helpDeskCustomer.getName());
    }

    private void tumaUjumbeKwaMteja(String phoneNumber, String message, String customerName) {

        try {
            String[] recipients = {phoneNumber};

            List<Recipient> response = sendSms(recipients, message);

            log.info("SMS sent successfully to {}", response);

            for (Recipient recipient : response) {
                log.info("SMS sent to {} with status: {}, messageId {}", recipient.number, recipient.status, recipient.messageId);
                saveCustomer(phoneNumber, customerName, message, recipient.status);
            }
        } catch (Exception e) {
            log.error("Error sending SMS via AfricaStalking", e);
        }
    }

    private List<Recipient> sendSms(String[] recipients, String message) {

        try {
            SmsService sms = AfricasTalking.getService(AfricasTalking.SERVICE_SMS);

            log.info("Attempting to send SMS to: {}, Message: {}, SenderID: {}",
                    Arrays.toString(recipients), message, senderId);

            return sms.send(message, senderId, recipients, true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send SMS: " + e.getMessage(), e);
        }
    }

    private void saveCustomer(String phone, String name, String message, String status) {
        TumaSms tumaSms = new TumaSms();
        tumaSms.setTextMessage(message);
        tumaSms.setPhoneNumber(phone);
        tumaSms.setRecipientName(name);
        tumaSms.setIsSuccess(status.equals("Success") ? Boolean.TRUE : Boolean.FALSE);
        tumaSms.setIsCustomer(Boolean.TRUE);
        tumaSmsRepository.save(tumaSms);
    }

    public Workflow validateWorkflow(UUID publicId) {
        Workflow workflow = workflowRepository.findByPublicIdAndDeletedIsFalse(publicId).orElse(null);
        if (workflow == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("workflow not found")
                    .userMessage("Sorry, workflow not found");
        }
        return workflow;
    }
}
