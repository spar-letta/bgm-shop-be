package com.app.itibari.services;

import com.africastalking.AfricasTalking;
import com.africastalking.SmsService;
import com.africastalking.sms.Recipient;
import com.app.itibari.converter.Converter;
import com.app.itibari.converter.CustomerFAQDtoToResponse;
import com.app.itibari.converter.TicketHistoryToResponse;
import com.app.itibari.converter.TicketViewerToResponse;
import com.app.itibari.dataTypes.*;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.*;
import com.app.itibari.models.response.*;
import com.app.itibari.repository.*;
import com.app.itibari.services.helper.BnplHelper;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.formatter.DateFormatter;
import com.app.itibari.utils.helpers.EmailBodyCrafter;
import com.app.itibari.utils.helpers.PhoneNumberHelper;
import com.app.itibari.utils.helpers.TumaMessage;
import com.app.itibari.utils.helpers.WebhookClient;
import com.app.itibari.utils.reportHelper.TicketReportHelper;
import com.app.itibari.utils.specification.CustomerFAQSpecification;
import com.app.itibari.utils.specification.TumaSmsSpecification;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.text.DocumentException;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.app.itibari.utils.helpers.PhoneNumberHelper.checkPhoneNumber;
import static com.app.itibari.utils.helpers.PhoneNumberHelper.getLastNineDigits;
import static com.app.itibari.utils.helpers.PhoneNumberHelper.toInternationalFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class HelpDeskServiceImpl implements HelpDeskService {

    private final DeliveryAgentsRepository deliveryAgentsRepository;
    private final SalesAgentsRepository salesAgentsRepository;
    private final SmsGroupRepository smsGroupRepository;
    private final SmsGroupEntityRepository smsGroupEntityRepository;
    private final CustomerFAQRepository customerFAQRepository;
    private final CustomerFAQNoteRepository customerFAQNoteRepository;
    private final ProductRepository productRepository;
    private final CustomerFAQDtoToResponse customerFAQDtoToResponse;
    private final SupplierRepository supplierRepository;
    private final FollowUpRepository followUpRepository;
    private final CustomerExperienceRepository customerExperienceRepository;
    private final WorkflowStatusRepository workflowStatusRepository;
    private final WorkflowEngineService workflowEngineService;
    private final TransitionRepository transitionRepository;
    private final TicketHistoryToResponse ticketHistoryToResponse;
    private final TicketViewerRepository ticketViewerRepository;
    private final TicketViewerToResponse ticketViewerToResponse;

    private final WebhookClient webhookClient;
    private final PhoneNumberHelper phoneNumberHelper;
    private final TumaMessage tumaMessage;
    private final SmsTemplateRepository smsTemplateRepository;
    private final JobService jobService;
    private final TicketReportHelper ticketReportHelper;
    private final BnplHelper bnplHelper;

    @Value("${africastalking.senderId}")
    private String senderId;

    @PersistenceContext
    private EntityManager entityManager;

    ObjectMapper mapper = new ObjectMapper();

    private Workbook workbook;
    private Sheet sheet;

    // single thread so one group blast at a time goes to Africa's Talking
    private final ExecutorService smsGroupExecutor = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "sms-group-sender"));

    private record GroupSmsRecipient(String name, String phone) {
    }

    private static final Pattern MESSAGE_PLACEHOLDER = Pattern.compile("\\{([^{}]+)}");

    private final HelpDeskRepository helpDeskRepository;
    private final CustomersRepository customersRepository;
    private final PersonRepository personRepository;
    private final CounterService counterService;
    private final DepartmentRepository departmentRepository;
    private final IssueTypeRepository issueTypeRepository;
    private final HelpDeskNotificationRepository helpDeskNotificationRepository;
    private final HelpDeskCommentRepository helpDeskCommentRepository;
    private final HelpDeskCustomerRepository helpDeskCustomerRepository;
    private final OrganizationRegionsRepository organizationRegionsRepository;
    private final OrganizationLocationsRepository organizationLocationsRepository;
    private final TumaSmsRepository tumaSmsRepository;
    private final WorkflowRepository workflowRepository;
    private final Converter<HelpDesk, HelpDeskDTOResponse> converter;
    private final Converter<HelpDeskComment, HelpDeskCommentResponseDTO.CommentDto> commentConverter;
    private final Converter<HelpDeskNotification, HelpDeskNotificationResponseDTO.DeskDto> notificationConverter;
    private final Converter<HelpDeskCustomer, HelpDeskCustomerDTOResponse> helpDeskCustomerDTOResponseConverter;

    private final EmailService emailService;
    private final EmailBodyCrafter emailBodyCrafter;
    private final TicketHistoryRepository ticketHistoryRepository;

    @Override
    public HelpDeskDTOResponse createTicket(TicketDto ticketDto, Principal principal) {
        ObjectMapper mapper = new ObjectMapper();
        Person raisedBy = getPerson(principal.getName());

        CustomerExperience customerExperience = customerExperienceRepository.findByPublicIdAndDeletedIsFalse(ticketDto.templatePublicId()).orElse(null);
        Department assignedDepartment = getTransitionName(customerExperience);

        LocalDateTime expectedResolveDate = getExpectedResolveDate(customerExperience);

        if (customerExperience == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Missing Template")
                    .userMessage("Sorry, template not found");
        }

        DeliveryAgents deliveryAgent = null;
        if (ticketDto.deliveryAgentPublicId() != null) {
            DeliveryAgents foundDeliveryAgent = deliveryAgentsRepository.findByPublicIdAndDeletedIsFalse(ticketDto.deliveryAgentPublicId()).orElse(null);
            if (foundDeliveryAgent == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Delivery Agent not found")
                        .userMessage("Sorry, delivery agent not found");
            }

            deliveryAgent = foundDeliveryAgent;
        }

        SalesAgents salesAgent = null;
        if (ticketDto.salesAgentPublicId() != null) {
            SalesAgents foundSalesAgents = salesAgentsRepository.findByPublicIdAndDeletedIsFalse(ticketDto.salesAgentPublicId()).orElse(null);
            if (foundSalesAgents == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Sales Agent not found")
                        .userMessage("Sorry, sales agent not found");
            }

            salesAgent = foundSalesAgents;
        }

        List<Product> productList = new ArrayList<>();
        List<Customers> customersList = new ArrayList<>();
        if (!ticketDto.productPublicIds().isEmpty()) {
            for (UUID productPublicId : ticketDto.productPublicIds()) {
                Product product = productRepository.findByPublicIdAndDeletedIsFalse(productPublicId).orElse(null);
                if (product != null) {
                    productList.add(product);
                }
            }
        }

        if (!ticketDto.customerPublicIds().isEmpty()) {
            for (UUID customerIds : ticketDto.customerPublicIds()) {
                Customers customers = customersRepository.findByPublicIdAndDeletedIsFalse(customerIds).orElse(null);
                if (customers != null) {
                    customersList.add(customers);
                }
            }
        }

        if (ticketDto.customerType() == TicketDto.CustomerType.Existing_Customer) {
            Department foundDepartment = fetchDepartment(ticketDto.departmentPublicId());
            IssueType issueTypeFound = fetchIssueType(ticketDto.issueTypeId());

            Customers customer = customersRepository
                    .findByPublicIdAndDeletedIsFalse(ticketDto.entityUserPublicId())
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Customer not found")
                            .userMessage("Sorry, Customer not found"));

            HelpDeskCustomer helpDeskCustomer = createHelpDeskCustomer(customer, ticketDto.customerType());

            HelpDesk newHelpDesk = new HelpDesk();
            newHelpDesk.setCreatedBy(raisedBy);
            newHelpDesk.setIssueType(issueTypeFound);
            newHelpDesk.setContent(ticketDto.content());
            newHelpDesk.setHelpDeskCustomer(helpDeskCustomer);

            newHelpDesk.setSection(foundDepartment);
            newHelpDesk.setTicketPriority(ticketDto.ticketPriority());
            newHelpDesk.setRead(Boolean.FALSE);
            newHelpDesk.setTicketNumber("TCK-" + counterService.getNextCounter(CounterType.HELP_TICKET));
            newHelpDesk.setDeliveryAgents(deliveryAgent);
            newHelpDesk.setSalesAgents(salesAgent);

            newHelpDesk.setAssignedToDepartment(ticketDto.assignedToDepartment());
            if (customerExperience != null) {
                newHelpDesk.setPocEmail(customerExperience.getPocEmail());
                newHelpDesk.getEmailInCopy().addAll(customerExperience.getEmailInCopyAsList());

                if (customerExperience.getAssignTo() == AssignToEnum.poc) {
                    newHelpDesk.setAssignedToPoc(true);
                } else {
                    newHelpDesk.setAssignedToPoc(true);
                }
            }
            newHelpDesk.setOrderNumber(ticketDto.orderNumber());
            newHelpDesk.setAmountPaid(ticketDto.amountPaid());
            newHelpDesk.setPaymentMode(ticketDto.paymentMode());
            newHelpDesk.setSummary(ticketDto.summary());
            newHelpDesk.setTemplate(customerExperience);
            newHelpDesk.setCustomField(mapper.valueToTree(ticketDto.customFields()));
            newHelpDesk.setTemplatePublicId(ticketDto.templatePublicId());
            newHelpDesk.setWorkflowStatus(assignedDepartment.getName());

            newHelpDesk.setResolvedDate(expectedResolveDate);

            if (!productList.isEmpty()) {
                newHelpDesk.getProductList().addAll(productList);
            }

            if (!customersList.isEmpty()) {
                newHelpDesk.getCustomersList().addAll(customersList);
            }

            HelpDesk savedHelpDesk = helpDeskRepository.save(newHelpDesk);
            HelpDeskDTOResponse helpDeskDTOResponse = converter.convert(savedHelpDesk);

            String notificationHeader = String.format("Ticket %s, created and assigned pending your action.(%s)", helpDeskDTOResponse.ticketNumber(), helpDeskDTOResponse.workflowStatus());

            List<HelpDeskDTOResponse.DepartmentInCopy> departmentInCopyList = helpDeskDTOResponse.departmentInCopy();
            List<HelpDeskDTOResponse.DepartmentInCopy> departInCopy = new ArrayList<>();
            if (!departmentInCopyList.isEmpty()) {
                departInCopy.addAll(departmentInCopyList);
            }
            createHelpDeskNotificationToGroup(assignedDepartment, notificationHeader, departInCopy);

            if (customerExperience.isAllowNotification()) {
                runSendMessageToCustomer(savedHelpDesk.getHelpDeskCustomer(), savedHelpDesk.getTicketNumber());
            }
            runDepartmentTicketCreatedMessageToWebHook(savedHelpDesk.getTicketNumber(), savedHelpDesk.getResolvedDate(), savedHelpDesk.getTemplate().getIssueType().getName(), savedHelpDesk.getSection(), helpDeskDTOResponse.departmentInCopy());

            runUpdateArrivalTime(savedHelpDesk);
            return helpDeskDTOResponse;
        } else if (ticketDto.customerType() == TicketDto.CustomerType.New_Customer) {
            IssueType issueTypeFound = null;

            if (ticketDto.issueTypeId() != null) {
                issueTypeFound = fetchIssueType(ticketDto.issueTypeId());
            }

            HelpDeskCustomer helpDeskCustomer = createHelpDeskNewCustomer(ticketDto);
            HelpDesk newHelpDesk = new HelpDesk();
            newHelpDesk.setCreatedBy(raisedBy);
            newHelpDesk.setIssueType(issueTypeFound);
            newHelpDesk.setContent(ticketDto.content());
            newHelpDesk.setTicketPriority(ticketDto.ticketPriority());
            newHelpDesk.setHelpDeskCustomer(helpDeskCustomer);
            newHelpDesk.setResolutionStatus(ticketDto.resolutionStatus());

            newHelpDesk.setAssignedToDepartment(ticketDto.assignedToDepartment());
            newHelpDesk.setAssignedToPoc(ticketDto.assignedToPoc());

            if (customerExperience != null) {
                newHelpDesk.setPocEmail(customerExperience.getPocEmail());
                newHelpDesk.getEmailInCopy().addAll(customerExperience.getEmailInCopyAsList());

                if (customerExperience.getAssignTo() == AssignToEnum.poc) {
                    newHelpDesk.setAssignedToPoc(true);
                } else {
                    newHelpDesk.setAssignedToPoc(true);
                }
            }

            newHelpDesk.setOrderNumber(ticketDto.orderNumber());
            newHelpDesk.setAmountPaid(ticketDto.amountPaid());
            newHelpDesk.setPaymentMode(ticketDto.paymentMode());
            newHelpDesk.setSummary(ticketDto.summary());
            newHelpDesk.setTemplate(customerExperience);
            newHelpDesk.setCustomField(mapper.valueToTree(ticketDto.customFields()));
            newHelpDesk.setTicketNumber("TCK-" + counterService.getNextCounter(CounterType.HELP_TICKET));
            newHelpDesk.setSection(issueTypeFound.getDepartment());
            newHelpDesk.setWorkflowStatus(assignedDepartment.getName());
            newHelpDesk.setDeliveryAgents(deliveryAgent);
            newHelpDesk.setSalesAgents(salesAgent);
            newHelpDesk.setResolvedDate(expectedResolveDate);

            if (!productList.isEmpty()) {
                newHelpDesk.getProductList().addAll(productList);
            }

            if (!customersList.isEmpty()) {
                newHelpDesk.getCustomersList().addAll(customersList);
            }
            newHelpDesk.setTemplatePublicId(ticketDto.templatePublicId());

            HelpDesk savedHelpDesk = helpDeskRepository.save(newHelpDesk);
            HelpDeskDTOResponse helpDeskDTOResponse = converter.convert(savedHelpDesk);

            String notificationHeader = String.format("Ticket %s, created and assigned pending your action.(%s)", issueTypeFound.getName(), helpDeskDTOResponse.workflowStatus());
            List<HelpDeskDTOResponse.DepartmentInCopy> departmentInCopyList = helpDeskDTOResponse.departmentInCopy();
            List<HelpDeskDTOResponse.DepartmentInCopy> departInCopy = new ArrayList<>();
            if (!departmentInCopyList.isEmpty()) {
                departInCopy.addAll(departmentInCopyList);
            }
            createHelpDeskNotificationToGroup(issueTypeFound.getDepartment(), notificationHeader, departInCopy);

            runDepartmentTicketCreatedMessageToWebHook(savedHelpDesk.getTicketNumber(), savedHelpDesk.getResolvedDate(), savedHelpDesk.getTemplate().getIssueType().getName(), savedHelpDesk.getSection(), helpDeskDTOResponse.departmentInCopy());

            runUpdateArrivalTime(savedHelpDesk);
            return helpDeskDTOResponse;
        }

        return null; // fallback for unexpected customer types
    }

    private void runUpdateArrivalTime(HelpDesk helpDesk) {
        CustomerExperience customerExperience = customerExperienceRepository
                .findByPublicIdAndDeletedIsFalse(helpDesk.getTemplatePublicId())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Missing Template")
                        .userMessage("Sorry, Missing Template"));

        Workflow wf = workflowRepository
                .findTopByTemplateAndDeletedIsFalseOrderByCreationTimeDesc(customerExperience)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Workflow not found")
                        .userMessage("Sorry, workflow not found"));

        List<Transition> list = wf.getTransitions().stream()
                .filter(t -> t.getStatusTo().getDepartment().getName()
                        .equalsIgnoreCase(helpDesk.getWorkflowStatus()))
                .collect(Collectors.toList());

        if (!list.isEmpty()) {
            ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");
            LocalDateTime nairobiDate = LocalDateTime.now(Clock.system(nairobiZone));

            list.sort(Comparator.comparing(
                    Transition::getTransitionIndex,
                    Comparator.nullsLast(Comparator.naturalOrder())
            ));

            Transition transition = list.get(list.size() - 1);
            transition.setTicketArrivalTime(nairobiDate);
            transitionRepository.save(transition);
        }
    }

    private void runDepartmentTicketCreatedMessageToWebHook(
            String ticketNumber,
            LocalDateTime resolvedDate,
            String issueType,
            Department section,
            List<HelpDeskDTOResponse.DepartmentInCopy> departmentInCopies) {

        String safeTicketNumber = ticketNumber != null ? ticketNumber : "N/A";
        String safeIssueType = issueType != null ? issueType : "Unknown Issue";
        String safeDepartment = (section != null && section.getName() != null) ? section.getName() : "Unknown Department";
        String expectedResolveDate = resolvedDate != null ? DateFormatter.formatDate(resolvedDate) : "Unspecified Date";

        String departmentInCopy = "";
        if (departmentInCopies != null && !departmentInCopies.isEmpty()) {
            departmentInCopy = departmentInCopies.stream()
                    .map(item -> item.name() != null ? item.name() : "Unnamed")
                    .collect(Collectors.joining(", "));
        }

        String webHookTicketCreatedMessage;
        if (!departmentInCopy.isEmpty()) {
            webHookTicketCreatedMessage = String.format(
                    "Ticket Number - %s of issue Type ( %s ), created under Department %s and Copied to Departments [%s]. Ticket set to be solved on %s",
                    safeTicketNumber,
                    safeIssueType,
                    safeDepartment,
                    departmentInCopy,
                    expectedResolveDate
            );
        } else {
            webHookTicketCreatedMessage = String.format(
                    "Ticket Number - %s of issue Type ( %s ), created under Department %s. Ticket set to be solved on %s",
                    safeTicketNumber,
                    safeIssueType,
                    safeDepartment,
                    expectedResolveDate
            );
        }

        String payload = String.format("{\"text\": \"%s\"}", webHookTicketCreatedMessage);

        if (!departmentInCopy.isEmpty()) {
            for (HelpDeskDTOResponse.DepartmentInCopy inCopy : departmentInCopies) {
                webhookClient.acceptAndSend(inCopy.name(), payload);
            }
        }

        webhookClient.acceptAndSend(safeDepartment, payload);
    }


    private LocalDateTime getExpectedResolveDate(CustomerExperience customerExperience) {
        ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");
        LocalDateTime nairobiDate = LocalDateTime.now(nairobiZone);


        LocalDateTime resolutionDate = LocalDateTime.now().plusHours(2);

        IssueType issueType = customerExperience.getIssueType();
        if (issueType != null && issueType.getPrioritySetting() != null) {
            PrioritySetting prioritySetting = issueType.getPrioritySetting();

            if (prioritySetting.getSlaType() == SlaType.Day) {
                int days = safeParseInt(prioritySetting.getSlaDuration());
                resolutionDate = nairobiDate.plusDays(days);
            }

            if (prioritySetting.getSlaType() == SlaType.Hour) {
                int hours = safeParseInt(prioritySetting.getSlaDuration());
                resolutionDate = nairobiDate.plusHours(hours);
            }
        }

        return resolutionDate;
    }

    private Department getTransitionName(CustomerExperience customerExperience) {
        Workflow wf = workflowRepository.findTopByTemplateAndDeletedIsFalseOrderByCreationTimeDesc(customerExperience)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Workflow not found")
                        .userMessage("Sorry, flow stages not found"));
        return wf.getAssignedDepartment();
    }


    private void runSendMessageToCustomer(HelpDeskCustomer helpDeskCustomer, String ticketNumber) {
        String storeName = helpDeskCustomer.getStoreName();
        storeName = (storeName != null && !storeName.isEmpty()) ? storeName : "";

        String message = String.format("Mpendwa mteja %s, tatizo lako limepokelewa na tiketi %s imefunguliwa. Timu yetu inalifanyia kazi. Asante kwa kuchagua Itibari", storeName, ticketNumber);
        tumaUjumbeKwaMteja(helpDeskCustomer.getPhoneNumber(), message, helpDeskCustomer.getName());
    }

    private HelpDeskCustomer createHelpDeskNewCustomer(TicketDto ticketDto) {
        HelpDeskCustomer foundHelpDeskCustomer = helpDeskCustomerRepository.findByPhoneNumberIgnoreCaseAndDeletedIsFalse(ticketDto.phone()).orElse(null);
        OrganizationRegions organizationRegions = null;
        OrganizationLocations organizationLocation = null;
        if (ticketDto.district() != null) {
            organizationRegions = organizationRegionsRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(ticketDto.district())).orElse(null);
        }

        if (ticketDto.district() != null) {
            organizationLocation = organizationLocationsRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(ticketDto.ward())).orElse(null);
        }

        if (foundHelpDeskCustomer != null) {
            foundHelpDeskCustomer.setUpdateDate(LocalDateTime.now());
        } else {
            foundHelpDeskCustomer = new HelpDeskCustomer();
            foundHelpDeskCustomer.setName(ticketDto.customerName());
            foundHelpDeskCustomer.setPhoneNumber(ticketDto.phone());
            foundHelpDeskCustomer.setCustomerType(TicketDto.CustomerType.New_Customer);
            foundHelpDeskCustomer.setDistrict(organizationRegions != null ? organizationRegions.getName() : null);
            foundHelpDeskCustomer.setWard(organizationLocation != null ? organizationLocation.getName() : null);
            foundHelpDeskCustomer.setPostalAddress(ticketDto.postalAddress());
            foundHelpDeskCustomer.setStoreName(ticketDto.institution());

            foundHelpDeskCustomer.setDistrictId(organizationRegions != null ? String.valueOf(organizationRegions.getPublicId()) : null);
            foundHelpDeskCustomer.setWardId(organizationLocation != null ? String.valueOf(organizationLocation.getPublicId()) : null);
        }
        return helpDeskCustomerRepository.save(foundHelpDeskCustomer);
    }

    private HelpDeskCustomer createHelpDeskCustomer(Customers customer, TicketDto.@NotNull CustomerType customerType) {
        HelpDeskCustomer foundHelpDeskCustomer = helpDeskCustomerRepository.findByPhoneNumberIgnoreCaseAndDeletedIsFalse(customer.getPhoneNumber()).orElse(null);
        if (foundHelpDeskCustomer != null) {
            foundHelpDeskCustomer.setUpdateDate(LocalDateTime.now());
        } else {
            foundHelpDeskCustomer = new HelpDeskCustomer();
            foundHelpDeskCustomer.setName(customer.getName());
            foundHelpDeskCustomer.setPhoneNumber(customer.getPhoneNumber());
            foundHelpDeskCustomer.setCustomerType(customerType);
            foundHelpDeskCustomer.setDistrictId(customer.getOrganizationRegions().getPublicId().toString());
            foundHelpDeskCustomer.setWardId(customer.getOrganizationLocation().getPublicId().toString());
            foundHelpDeskCustomer.setPostalAddress(customer.getAddress());
            foundHelpDeskCustomer.setExistingCustomerPublicId(customer.getPublicId());
            foundHelpDeskCustomer.setStoreName(customer.getStoreName());

            foundHelpDeskCustomer.setDistrict(customer.getOrganizationRegions().getName());
            foundHelpDeskCustomer.setWard(customer.getOrganizationLocation().getName());
        }
        return helpDeskCustomerRepository.save(foundHelpDeskCustomer);
    }

    @Override
    public Page<HelpDeskDTOResponse> getHelpDeskList(
            String searchParam,
            String helpDeskStatus,
            TicketPriority ticketPriority,
            LocalDate startDate,
            LocalDate endDate,
            UUID customerPublicId, UUID existingCustomerPublicId,
            UUID departmentPublicId,
            TicketDto.CustomerType customerType,
            Pageable pageable) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<HelpDesk> cq = cb.createQuery(HelpDesk.class);
        Root<HelpDesk> root = cq.from(HelpDesk.class);
        Predicate[] predicates = buildPredicates(cb, root, helpDeskStatus, ticketPriority, startDate, endDate, searchParam, customerPublicId, existingCustomerPublicId, departmentPublicId, customerType);

        cq.where(predicates).orderBy(cb.desc(root.get("id")));

        TypedQuery<HelpDesk> query = entityManager.createQuery(cq)
                .setMaxResults(pageable.getPageSize())
                .setFirstResult((int) pageable.getOffset());

        List<HelpDesk> tickets = query.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HelpDesk> countRoot = countQuery.from(HelpDesk.class);
        Predicate[] countPredicates = buildPredicates(cb, countRoot, helpDeskStatus, ticketPriority, startDate, endDate, searchParam, customerPublicId, existingCustomerPublicId, departmentPublicId, customerType);

        countQuery.select(cb.countDistinct(countRoot)).where(countPredicates);
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        List<HelpDeskDTOResponse> dtoList = tickets.stream()
                .map(converter::convert)
                .toList();

        return new PageImpl<>(dtoList, pageable, total);
    }

    @Override
    public HelpDeskDTOResponse updateTicket(UUID publicId, TicketUpdateDto ticketUpdateDto, Principal principal) {
        Person updatedBy = getPerson(principal.getName());

        HelpDesk helpDesk = getHelpDesk(publicId);

        helpDesk.setContent(ticketUpdateDto.content());
        helpDesk.setTicketPriority(ticketUpdateDto.ticketPriority());
        helpDesk.setUpdatedBy(updatedBy);
        HelpDesk savedHelpDesk = helpDeskRepository.save(helpDesk);
        return converter.convert(savedHelpDesk);
    }

    @Override
    public HelpDeskDTOResponse getHelpDesk(UUID publicId, Principal principal) {
        HelpDesk helpDesk = getHelpDesk(publicId);
        return converter.convert(helpDesk);
    }

    @Override
    public HelpDeskCommentResponseDTO.CommentDto createComment(UUID publicId,
                                                               TicketCommentDto ticketCommentDto, Principal principal) {
        Person createdBy = getPerson(principal.getName());

        HelpDesk helpDesk = getHelpDesk(publicId);

        HelpDeskComment helpDeskComment = new HelpDeskComment();
        helpDeskComment.setCommentBody(ticketCommentDto.content());
        helpDeskComment.setHelpDesk(helpDesk);
        helpDeskComment.setCreatedBy(createdBy);
        helpDeskComment.setRead(Boolean.FALSE);
        helpDeskComment.setExternal(ticketCommentDto.isExternal() == null ? Boolean.FALSE : ticketCommentDto.isExternal());
        HelpDeskComment savedHelpDeskComment = helpDeskCommentRepository.save(helpDeskComment);
        markCommentAsRead(createdBy, helpDesk);

        return commentConverter.convert(savedHelpDeskComment);
    }

    private void markCommentAsRead(Person loggedIn, HelpDesk helpDesk) {
        List<HelpDeskComment> helpDeskCommentList = helpDeskCommentRepository.findAllByHelpDeskAndNotCreator(helpDesk,
                loggedIn);

        if (!helpDeskCommentList.isEmpty()) {
            for (HelpDeskComment helpDeskComment : helpDeskCommentList) {
                helpDeskComment.setRead(Boolean.TRUE);
                helpDeskComment.setUpdatedBy(loggedIn);
                helpDeskCommentRepository.save(helpDeskComment);
            }
        }
    }

    @Override
    public HelpDeskCommentResponseDTO getHelpDeskCommentList(UUID publicId, Principal principal) {
        Person loggedIn = getPerson(principal.getName());
        HelpDesk helpDesk = getHelpDesk(publicId);
        List<HelpDeskComment> helpDeskCommentList = helpDeskCommentRepository.findAllByHelpDesk(helpDesk);
        List<HelpDeskCommentResponseDTO.CommentDto> helpDeskCommentResponseDTOList =
                helpDeskCommentList.stream().map(commentConverter::convert).toList();

        long count = 0L;
        if (!helpDeskCommentList.isEmpty()) {
            count =
                    helpDeskCommentList.stream().filter(item -> !item.getCreatedBy().equals(loggedIn) && item.getRead() == Boolean.FALSE).count();
        }

        return HelpDeskCommentResponseDTO.builder().unReadComment(count).commentDtoList(helpDeskCommentResponseDTOList).build();
    }

    @Override
    public HelpDeskTicketSummary getTicketSummary(UUID customerPublicId, String searchParam, String helpDeskStatus, TicketDto.CustomerType customerType, UUID departmentPublicId, Principal principal) {
        ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");
        LocalDateTime nairobiDate = LocalDateTime.now(nairobiZone);

        List<String> allTicketStatus = getTicketStatus();
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<HelpDeskTicketSummary> cq = cb.createQuery(HelpDeskTicketSummary.class);
        Root<HelpDesk> root = cq.from(HelpDesk.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isFalse(root.get("deleted")));

        if (customerPublicId != null) {
            predicates.add(cb.equal(root.get("helpDeskCustomer").get("publicId"), customerPublicId));
        }

        if (departmentPublicId != null) {
            Join<HelpDesk, Department> deptJoin = root.join("template").join("departmentInCopy", JoinType.LEFT);

            predicates.add(cb.or(
                    cb.equal(root.get("section").get("publicId"), departmentPublicId),
                    cb.equal(deptJoin.get("publicId"), departmentPublicId)
            ));
        }

        if (helpDeskStatus != null && !helpDeskStatus.isEmpty() && helpDeskStatus.trim().length() >= 3) {
            String search = "%" + helpDeskStatus.toUpperCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.upper(root.get("workflowNewStatus")), search)
            ));
        }

        if (searchParam != null && searchParam.trim().length() >= 3) {
            String search = "%" + searchParam.toUpperCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.upper(root.get("ticketNumber")), search),
                    cb.like(cb.upper(root.get("workflowStatus")), search),
                    cb.like(cb.upper(root.get("helpDeskCustomer").get("name")), search),
                    cb.like(cb.upper(root.get("helpDeskCustomer").get("phoneNumber")), search)
            ));
        }

        if (customerType != null) {
            predicates.add(cb.equal(root.get("helpDeskCustomer").get("customerType"), customerType));
        }

        cq.where(predicates.toArray(new Predicate[0]));

        Expression<Long> totalTickets = cb.count(root);

        Expression<Long> totalResolved = cb.sum(
                cb.<Long>selectCase()
                        .when(root.get("workflowNewStatus").in("RESOLVE", "RESOLVED"), 1L
                        ).otherwise(0L));

        Expression<Long> totalPending = cb.sum(
                cb.<Long>selectCase()
                        .when(root.get("workflowNewStatus").in(allTicketStatus), 1L
                        ).otherwise(0L));

        Expression<Long> totalOverdue = cb.sum(
                cb.<Long>selectCase()
                        .when(cb.and(
                                        root.get("workflowNewStatus").in(allTicketStatus),
                                        cb.lessThan(root.get("resolvedDate"), nairobiDate)
                                ), 1L
                        ).otherwise(0L));


        cq.select(cb.construct(
                HelpDeskTicketSummary.class,
                totalTickets,
                totalResolved,
                totalPending,
                totalOverdue
        ));
        return entityManager.createQuery(cq).getSingleResult();
    }

    private List<String> getTicketStatus() {
        Set<String> allTicketStatus = listHelpDeskStatus(null, null);
        if (!allTicketStatus.isEmpty()) {
            return allTicketStatus.stream()
                    .filter(item -> !item.equalsIgnoreCase("resolve")
                            && !item.equalsIgnoreCase("resolved"))
                    .toList();
        } else {
            return Collections.emptyList();
        }
    }


    @Override
    public HelpDeskDTOResponse actionTicket(UUID ticketPublicId, TicketActionDto ticketActionDto, Principal
            principal) {
        Person updatedBy = getPerson(principal.getName());
        Random rand = new Random();

        HelpDesk helpDesk = getHelpDesk(ticketPublicId);
        HelpDesk saveHelpDesk = null;
        if (ticketActionDto.ticketActionType() == TicketActionDto.TicketActionType.Viewed) {
            if (helpDesk.getRead() == Boolean.FALSE) {
//                helpDesk.setAssignedTo(updatedBy);
//                helpDesk.setRead(Boolean.TRUE);
//                saveHelpDesk = helpDeskRepository.save(helpDesk);
//                recordFirstViewer(saveHelpDesk, updatedBy.getFullName());
            } else if (helpDesk.getStatus() == HelpDeskStatus.Assigned) {
                if (helpDesk.getAssignedTo().equals(updatedBy)) {
                    helpDesk.setRead(Boolean.TRUE);
                    saveHelpDesk = helpDeskRepository.save(helpDesk);
                }
            }
        }
        if (ticketActionDto.ticketActionType() == TicketActionDto.TicketActionType.Resolve) {

            helpDesk.setUpdatedBy(updatedBy);
            saveHelpDesk = helpDeskRepository.save(helpDesk);

            List<String> randomString = Arrays.asList(
                    "Resolved Successfully",
                    "Resolved. Thank you",
                    "Issue fixed",
                    "Problem solved",
                    "All good now",
                    "Handled and closed",
                    "Resolved with no further action",
                    "Successfully addressed",
                    "Case closed",
                    "Sorted out",
                    "Everything is fine now",
                    "Resolved and verified",
                    "Fixed permanently",
                    "No issues remaining"
            );
            String picked = randomString.get(rand.nextInt(randomString.size()));
            craftAndSaveComment(updatedBy, saveHelpDesk, picked);
        }

        if (ticketActionDto.ticketActionType() == TicketActionDto.TicketActionType.Assign) {
            Person assigned = getAssignedTo(ticketActionDto.assignedToId());
            if (updatedBy.equals(assigned)) {
                throw new BadRequestRestApiException()
                        .developerMessage("Do not assign yourself.")
                        .userMessage("Sorry, do not assign yourself.");
            }

            helpDesk.setAssignedTo(assigned);
            helpDesk.setUpdatedBy(updatedBy);
//            helpDesk.setStatus(HelpDeskStatus.Assigned);
//            helpDesk.setRead(Boolean.FALSE);
            saveHelpDesk = helpDeskRepository.save(helpDesk);

            String messageToOfficer = String.format("Hi %s, please help take action on ticket (%s)",
                    assigned.getFirstName(),
                    helpDesk.getTicketNumber());

            createHelpDeskNotification(assigned.getPublicId(), messageToOfficer);
        }
        return converter.convert(saveHelpDesk);
    }

    @Override
    public HelpDeskNotificationResponseDTO getTicketNotifications(Principal principal, Pageable pageable) {
        Person loggedInUser = getPerson(principal.getName());
        Page<HelpDeskNotification> helpDeskNotifications =
                helpDeskNotificationRepository.findAllByAssignedToPublicId(loggedInUser.getPublicId(), pageable);
        Page<HelpDeskNotificationResponseDTO.DeskDto> deskDtoPage = helpDeskNotifications.map(notificationConverter::convert);
        long count = deskDtoPage.getContent().stream().filter(item -> !item.read()).count();

        return HelpDeskNotificationResponseDTO.builder()
                .count(count)
                .deskDtoList(deskDtoPage.getContent())
                .build();
    }

    @Override
    public void actionTicketAlert(UUID ticketNotificationPublicId, Principal principal) {
        HelpDeskNotification helpDeskNotification = helpDeskNotificationRepository.findByPublicId(ticketNotificationPublicId).orElse(null);
        if (helpDeskNotification != null) {
            helpDeskNotification.setRead(Boolean.TRUE);
            helpDeskNotification.setUpdateDate(LocalDateTime.now());
            helpDeskNotificationRepository.save(helpDeskNotification);
        }
    }

    @Override
    public Page<HelpDeskCustomerDTOResponse> getHelpDeskCustomer(String searchParam, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<HelpDeskCustomer> cq = cb.createQuery(HelpDeskCustomer.class);
        Root<HelpDeskCustomer> root = cq.from(HelpDeskCustomer.class);
        Predicate[] predicates = buildPredicatesV2(cb, root, startDate, endDate, searchParam);

        cq.where(predicates).orderBy(cb.desc(root.get("id")));

        TypedQuery<HelpDeskCustomer> query = entityManager.createQuery(cq)
                .setMaxResults(pageable.getPageSize())
                .setFirstResult((int) pageable.getOffset());

        List<HelpDeskCustomer> tickets = query.getResultList();

        List<HelpDeskCustomerDTOResponse> dtoList = tickets.stream()
                .map(helpDeskCustomerDTOResponseConverter::convert)
                .toList();

        return new PageImpl<>(dtoList, pageable, tickets.size());
    }

    @Override
    public void runTumaSms(TumaSmsDto tumaSmsDto) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, InterruptedException, JobRestartException {

        String requestMessage = tumaSmsDto.messageBody();
        if (tumaSmsDto.isCustomer() == Boolean.TRUE) {
            if (tumaSmsDto.activeCustomers() == Boolean.TRUE) {
                ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");
                LocalDateTime nairobiDate = LocalDateTime.now(Clock.system(nairobiZone));
                LocalDateTime startDate = nairobiDate.minusMonths(6);
                LocalDateTime endDate = nairobiDate;
                jobService.runActiveCustomerJob(requestMessage, tumaSmsDto.customVariable(), null, true, false);
            } else {
                if (tumaSmsDto.multiplePhoneNumber().isEmpty()) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Upload customer number (s)")
                            .userMessage("Sorry, please upload customer number (s)");
                }
                for (String telNumber : tumaSmsDto.multiplePhoneNumber()) {
                    this.sendSmsViaAfricaStalking(telNumber, requestMessage, tumaSmsDto.customVariable());
                }
            }
        }

        if (tumaSmsDto.isDeliveryAgent() == Boolean.TRUE) {
            if (tumaSmsDto.multiplePhoneNumber().isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("add delivery agent number (s)")
                        .userMessage("Sorry, please add delivery agent number (s)");
            }

            for (String telNumber : tumaSmsDto.multiplePhoneNumber()) {
                this.sendSmsToDeliveryAgent(telNumber, requestMessage, tumaSmsDto.customVariable());
            }
        }

        if (tumaSmsDto.isSalesAgent() == Boolean.TRUE) {
            if (tumaSmsDto.multiplePhoneNumber().isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("add sales agent number (s)")
                        .userMessage("Sorry, please add sales agent number (s)");
            }

            for (String telNumber : tumaSmsDto.multiplePhoneNumber()) {
                this.sendSmsToSalesAgent(telNumber, requestMessage, tumaSmsDto.customVariable());
            }
        }

        if (tumaSmsDto.isGroup() == Boolean.TRUE) {

            if (tumaSmsDto.groupPublicId() == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Missing group parameter")
                        .userMessage("Sorry, missing group parameter");
            }

            SmsGroup smsGroup = smsGroupRepository.findByPublicIdAndDeletedIsFalse(tumaSmsDto.groupPublicId())
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Sms group not found")
                            .userMessage("Sorry, sms group not found"));

            boolean sms = tumaSmsDto.sms();
            boolean whatsapp = tumaSmsDto.whatsapp();

            if ("ALL_CUSTOMERS".equalsIgnoreCase(smsGroup.getDefaultName())) {
                jobService.runActiveCustomerJob(requestMessage, tumaSmsDto.customVariable(), tumaSmsDto.campaignName(), sms, whatsapp);
            } else if ("BNPL".equalsIgnoreCase(smsGroup.getDefaultName())) {
                jobService.runBnplEligibleCustomerJob(requestMessage, tumaSmsDto.customVariable(), tumaSmsDto.campaignName(), sms, whatsapp);
            } else {
                sendToGroupMembers(smsGroup, requestMessage, tumaSmsDto.customVariable(), tumaSmsDto.campaignName(), sms, whatsapp);
            }
        }
    }

    private void sendToGroupMembers(SmsGroup smsGroup, String message, Set<String> customVariable, String campaignName, boolean sms, boolean whatsapp) {
        // copied to plain values so the background thread never touches lazy JPA state
        List<GroupSmsRecipient> recipients = smsGroupEntityRepository.findBySmsGroup(smsGroup).stream()
                .map(member -> new GroupSmsRecipient(member.getName(), member.getPhone()))
                .toList();

        if (recipients.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sms group has no contacts")
                    .userMessage("Sorry, this group has no contacts. Please upload contacts first");
        }

        String groupName = smsGroup.getName();
        Set<String> variables = new HashSet<>(customVariable);
        Matcher placeholder = MESSAGE_PLACEHOLDER.matcher(message);
        while (placeholder.find()) {
            variables.add(placeholder.group(1));
        }
        log.info("Queueing messages (sms={}, whatsapp={}) to {} members of group '{}'", sms, whatsapp, recipients.size(), groupName);

        smsGroupExecutor.execute(() -> {
            for (GroupSmsRecipient recipient : recipients) {
                sendToGroupMember(recipient, message, variables, campaignName, sms, whatsapp);
            }
            log.info("Finished sending messages to {} members of group '{}'", recipients.size(), groupName);
        });
    }

    private void sendToGroupMember(GroupSmsRecipient member, String message, Set<String> customVariable, String campaignName, boolean sms, boolean whatsapp) {
        String name = Objects.requireNonNullElse(member.name(), "").trim();
        String phone = toInternationalFormat(member.phone());
        String personalisedMessage = new StringSubstitutor(groupMemberVariables(name, phone, customVariable), "{", "}").replace(message);

        if (sms) {
            sendSmsToGroupMember(name, phone, personalisedMessage, campaignName);
        }
        if (whatsapp) {
            sendWhatsAppToGroupMember(name, phone, personalisedMessage);
        }
    }

    private void sendWhatsAppToGroupMember(String name, String phone, String personalisedMessage) {
        try {
            tumaMessage.sendWhatsAppBroadcast(phone, name, personalisedMessage);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted sending group WhatsApp to {}", phone, e);
        } catch (Exception e) {
            log.error("Error sending group WhatsApp to {}", phone, e);
        }
    }

    private void sendSmsToGroupMember(String name, String phone, String personalisedMessage, String campaignName) {
        try {
            List<Recipient> response = sendSms(new String[]{phone}, personalisedMessage);

            for (Recipient recipient : response) {
                saveSmsGroup(name, phone, personalisedMessage, recipient.status, campaignName);
            }
        } catch (Exception e) {
            log.error("Error sending group SMS to {}", phone, e);
            saveSmsGroup(name, phone, personalisedMessage, "Failed", campaignName);
        }
    }

    // Variables come from the matching Customers record (same as customer SMS); uploaded name/phone fill the gaps
    private Map<String, String> groupMemberVariables(String name, String phone, Set<String> customVariable) {
        Map<String, String> values = new HashMap<>();

        Customers customer = customVariable.isEmpty() ? null : findCustomerByPhone(phone);
        if (customer != null) {
            values.putAll(extractFields(customer, customVariable.stream().toList()));

            if (customVariable.contains("creditLimit") && !customer.isBnplAccountCreated()) {
                values.put("creditLimit", getEligibleCreditLimit(customer));
            }
        }

        for (String variable : customVariable) {
            values.putIfAbsent(variable, "");
        }

        String[] nameParts = name.split("\\s+");
        putIfBlank(values, "Name", name);
        putIfBlank(values, "name", name);
        putIfBlank(values, "fullName", name);
        putIfBlank(values, "firstName", nameParts[0]);
        putIfBlank(values, "lastName", nameParts.length > 1 ? nameParts[nameParts.length - 1] : "");
        putIfBlank(values, "phoneNumber", phone);
        return values;
    }

    private Customers findCustomerByPhone(String phone) {
        try {
            return customersRepository.findByPhoneNumberLastNine(getLastNineDigits(phone)).stream()
                    .filter(customer -> !Boolean.TRUE.equals(customer.getDeleted()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Could not look up customer for group SMS recipient {}: {}", phone, e.getMessage());
            return null;
        }
    }

    private static void putIfBlank(Map<String, String> values, String key, String value) {
        String existing = values.get(key);
        if (existing == null || existing.isBlank()) {
            values.put(key, value);
        }
    }

    @PreDestroy
    void shutdownSmsGroupExecutor() {
        smsGroupExecutor.shutdown();
    }

    @Override
    public Page<TumaSms> getTumaSms(String searchParam, LocalDate startDate, LocalDate endDate, SMSStatus smsStatus, Boolean isCampaign, Pageable pageable) {

        Specification<TumaSms> specification = Specification
                .where(TumaSmsSpecification.searchParamMatches(searchParam))
                .and(TumaSmsSpecification.hasStatus(smsStatus))
                .and(TumaSmsSpecification.hasDateRange(startDate, endDate))
                .and(TumaSmsSpecification.hasCampaign(isCampaign))
                .and(TumaSmsSpecification.isNotDeleted());

        return tumaSmsRepository.findAll(specification, pageable);
    }

    @Override
    public Map<String, String> getTumaSmsFields(String searchParam) {
        Map<String, String> fieldMap = new HashMap<>();

        Field[] fields = Customers.class.getDeclaredFields();

        for (Field field : fields) {
            String fieldName = field.getName();
            fieldMap.put(fieldName, fieldName);
        }

        return fieldMap;
    }

    @Override
    public void getSmsGroupTemplate(HttpServletResponse response, String name) {
        Person loggedInUser = getPerson(name);

        String prefix = "sms_group_template";

        try {
            workbook = createWorkbook();
            setResponseHeader(response, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx", prefix);
            writeHeaderLineSmsGroupTemplate("SMS Group Template");
            writeDataLineSmsGroupTemplate(loggedInUser);
            workbook.write(response.getOutputStream());
            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    @Transactional
    public SmsGroupUploadResponse uploadSmsGroup(MultipartFile file, String groupName, String name) {
        List<SmsGroupExcelDto> smsGroupExcelDtoList = readSmsGroupRows(file);

        if (smsGroupExcelDtoList.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Uploaded file has no data rows")
                    .userMessage("The file has no names or phone numbers to upload");
        }

        return createSmsGroup(smsGroupExcelDtoList, groupName.trim(), name);
    }

    private List<SmsGroupExcelDto> readSmsGroupRows(MultipartFile file) {
        int COL_NAME = 0;
        int COL_PHONE = 1;

        List<SmsGroupExcelDto> smsGroupExcelDtoList = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row r = sheet.getRow(rowIndex);

                if (isRowEmpty(r)) {
                    continue;
                }

                // Excel shows rows starting from 1, POI counts from 0
                int excelRow = rowIndex + 1;
                SmsGroupExcelDto request = new SmsGroupExcelDto();
                request.setRowIndex(excelRow);

                List<String> rowErrors = new ArrayList<>();

                String personName = readCellAsString(r.getCell(COL_NAME));
                if (personName.isEmpty()) {
                    rowErrors.add("name is required");
                } else {
                    request.setName(personName);
                }

                String personPhone = readCellAsString(r.getCell(COL_PHONE));
                if (personPhone.isEmpty()) {
                    rowErrors.add("phone is required");
                } else {
                    request.setPhone(personPhone);
                }

                if (!rowErrors.isEmpty()) {
                    request.getErrorList().put(String.format("Row %d", excelRow), String.join("; ", rowErrors));
                }

                smsGroupExcelDtoList.add(request);
            }
        } catch (IOException | EncryptedDocumentException | IllegalArgumentException e) {
            log.warn("Could not read sms-group upload {}: {}", file.getOriginalFilename(), e.getMessage());
            throw new BadRequestRestApiException()
                    .developerMessage("Could not read Excel file: " + e.getMessage())
                    .userMessage("Could not read the file. Please upload a valid Excel file");
        }

        return smsGroupExcelDtoList;
    }

    private String readCellAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        CellType cellType = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();

        return switch (cellType) {
            case STRING -> cell.getStringCellValue().trim();
            // avoids scientific notation (2.55713E+11) for phone numbers stored as numbers
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    @Override
    public Page<SmsGroup> getSmsGroups(String searchParam, LocalDate startDate, LocalDate endDate, Boolean defaultGroup, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<SmsGroup> cq = cb.createQuery(SmsGroup.class);
        Root<SmsGroup> root = cq.from(SmsGroup.class);
        Predicate[] predicates = buildPredicatesForSmsGroup(cb, root, searchParam, startDate, endDate, defaultGroup);

        cq.where(predicates).orderBy(cb.desc(root.get("id")));

        TypedQuery<SmsGroup> query = entityManager.createQuery(cq)
                .setMaxResults(pageable.getPageSize())
                .setFirstResult((int) pageable.getOffset());

        List<SmsGroup> tickets = query.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<SmsGroup> countRoot = countQuery.from(SmsGroup.class);
        Predicate[] countPredicates = buildPredicatesForSmsGroup(cb, root, searchParam, startDate, endDate, defaultGroup);

        countQuery.select(cb.countDistinct(countRoot)).where(countPredicates);

        return new PageImpl<>(tickets, pageable, tickets.size());
    }

    private Predicate[] buildPredicatesForSmsGroup(CriteriaBuilder cb, Root<SmsGroup> root, String searchParam, LocalDate startDate, LocalDate endDate, Boolean defaultGroup) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("deleted"), Boolean.FALSE));

        if (defaultGroup != null) {
            predicates.add(cb.equal(root.get("defaultGroup"), defaultGroup));
        }

        if (startDate != null && endDate != null) {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
            predicates.add(cb.between(root.get("creationTime"), startDateTime, endDateTime));
        }

        if (searchParam != null && searchParam.trim().length() >= 3) {
            String search = "%" + searchParam.toUpperCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.upper(root.get("name")), search)
//                    cb.like(cb.upper(root.get("helpDeskCustomer").get("name")), search)
            ));
        }
        return predicates.toArray(new Predicate[0]);
    }

    @Override
    public CustomerFAQToResponse createFaqRequest(FaqRequest faqRequest, Principal principal) {
        Person loggedInBy = getPerson(principal.getName());
        Department department = departmentRepository.findByPublicIdAndDeletedIsFalse(faqRequest.departmentPublicId()).orElse(null);
        if (department == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Department Haipo")
                    .userMessage("Samahani, department Haipo.");
        }

        CustomerFAQ customerFAQ = customerFAQRepository.findByDepartmentAndDeletedIsFalse(department).orElse(null);

        if (customerFAQ == null) {
            customerFAQ = new CustomerFAQ();
            customerFAQ.setDepartment(department);
            customerFAQ.setCreatedBy(loggedInBy);
        } else {
            customerFAQ.setUpdatedBy(loggedInBy);
            customerFAQ.preUpdate();
        }
        CustomerFAQ saveCustomerFAQ = customerFAQRepository.save(customerFAQ);

        for (FaqRequest.FaqInner faqInner : faqRequest.faqInnerList()) {
            IssueType issueType = issueTypeRepository.findByPublicIdAndDeletedIsFalse(faqInner.issueTypePublicId()).orElse(null);
            if (issueType == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Issue Type Haipo")
                        .userMessage("Samahani, Issue Type Haipo.");
            }

            CustomerFAQNote customerFAQNote = customerFAQNoteRepository.findByCustomerFAQAndIssueTypeAndDeletedIsFalse(customerFAQ, issueType).orElse(null);

            if (customerFAQNote == null) {
                customerFAQNote = new CustomerFAQNote();
                customerFAQNote.setCustomerFAQ(saveCustomerFAQ);
                customerFAQNote.setQuestion(faqInner.question());
                customerFAQNote.setAnswer(faqInner.answer());
                customerFAQNote.setIssueType(issueType);
                customerFAQNote.setCreatedBy(loggedInBy);
            } else {
                customerFAQNote.setCreatedBy(loggedInBy);
                customerFAQNote.setQuestion(faqInner.question());
                customerFAQNote.setAnswer(faqInner.answer());
            }
            customerFAQNoteRepository.save(customerFAQNote);
        }

        return customerFAQDtoToResponse.convert(saveCustomerFAQ);
    }

    @Override
    public Page<CustomerFAQToResponse> getCustomerFAQ(String searchParam, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Specification<CustomerFAQ> specification = Specification
                .where(CustomerFAQSpecification.searchParamMatches(searchParam))
                .and(CustomerFAQSpecification.hasDateRange(startDate, endDate))
                .and(CustomerFAQSpecification.isNotDeleted());

        List<CustomerFAQ> content = customerFAQRepository.findAll(specification, pageable).getContent();

        List<CustomerFAQToResponse> dtoList = content.stream()
                .map(customerFAQDtoToResponse::convert)
                .toList();

        return new PageImpl<>(dtoList, pageable, dtoList.size());
    }

    @Override
    public SmsGroup getSmsGroup(UUID publicId) {
        SmsGroup smsGroup = smsGroupRepository.findByPublicIdAndDeletedIsFalse(publicId).orElse(null);
        if (smsGroup == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Group not found")
                    .userMessage("Sorry, group not found");
        }
        return smsGroup;
    }

    @Override
    public CustomerFAQToResponse getFAQ(UUID publicId) {
        CustomerFAQ customerFAQ = customerFAQRepository.findByPublicIdAndDeletedIsFalse(publicId).orElse(null);
        if (customerFAQ == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Faq not found")
                    .userMessage("Sorry, Faq not found");
        }
        return customerFAQDtoToResponse.convert(customerFAQ);
    }

    @Override
    public SmsSummaryDto getSmsSummaryDto() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SmsSummaryDto> cq = cb.createQuery(SmsSummaryDto.class);
        Root<TumaSms> root = cq.from(TumaSms.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("deleted"), Boolean.FALSE));

        cq.where(predicates.toArray(new Predicate[0]));

        Expression<Long> totalSms = cb.count(root);

        Expression<Long> totalDelivered = cb.sum(
                cb.<Long>selectCase()
                        .when(cb.equal(root.get("isSuccess"), Boolean.TRUE), 1L)
                        .otherwise(0L));

        Expression<Long> totalUnDelivered = cb.sum(
                cb.<Long>selectCase()
                        .when(cb.equal(root.get("isSuccess"), Boolean.FALSE), 1L)
                        .otherwise(0L));

        cq.select(cb.construct(
                SmsSummaryDto.class,
                totalSms,
                totalDelivered,
                totalUnDelivered
        ));

        return entityManager.createQuery(cq).getSingleResult();
    }

    @Override
    public void updateFaqRequest(UUID customerFaqNotePublicId, FaqUpdateRequest faqUpdateRequest, Principal principal) {
        Person updatedBy = getPerson(principal.getName());
        CustomerFAQNote customerFAQNote = fetchCustomerFAQNote(customerFaqNotePublicId, principal);

        IssueType issueType = fetchIssueType(faqUpdateRequest.issueTypePublicId());
        customerFAQNote.setAnswer(faqUpdateRequest.answer());
        customerFAQNote.setQuestion(faqUpdateRequest.question());
        customerFAQNote.setIssueType(issueType);
        customerFAQNote.setUpdatedBy(updatedBy);
        customerFAQNoteRepository.save(customerFAQNote);
    }

    @Override
    public CustomerFAQNote fetchCustomerFAQNote(UUID publicId, Principal principal) {
        CustomerFAQNote customerFAQNote = customerFAQNoteRepository.findByPublicIdAndDeletedIsFalse(publicId).orElse(null);
        if (customerFAQNote == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("FAQ Note not found")
                    .userMessage("Sorry, FAQ Note not found");
        }
        return customerFAQNote;
    }

    @Override
    public Page<SmsDefaultGroupResponse> getSmsDefaultGroups(String searchParam, String defaultGroupName, Pageable pageable) {
        Set<String> defaultNames = Set.of("COOPERATE", "REGULAR_CREDIT", "SUPPLIER", "DELIVERY_AGENT", "SALES_AGENT", "ALL_CUSTOMERS");
        if (!defaultNames.contains(defaultGroupName)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Group not found")
                    .userMessage("Sorry, Group not found");
        }

        if (defaultGroupName.equals("COOPERATE")) {

        }

        if (defaultGroupName.equals("REGULAR_CREDIT")) {

        }

        if (defaultGroupName.equals("SUPPLIER")) {
            return tighAndReturnSupplierData(searchParam, pageable);
        }

        if (defaultGroupName.equals("DELIVERY_AGENT")) {

        }

        if (defaultGroupName.equals("SALES_AGENT")) {

        }

        if (defaultGroupName.equals("ALL_CUSTOMERS")) {

        }
        return null;
    }

    @Override
    public Page<SmsGroupEntity> getSmsGroupEntity(UUID publicId, String searchParam, Pageable pageable) {
        SmsGroup smsGroup = getSmsGroup(publicId);
        List<SmsGroupEntity> smsGroupEntityList = smsGroup.getSmsGroupEntityList();

        if (smsGroupEntityList.isEmpty()) {
            return new PageImpl<>(Collections.EMPTY_LIST, pageable, 0);
        }
        return new PageImpl<>(smsGroupEntityList, pageable, smsGroupEntityList.size());
    }

    @Override
    public HelpDeskCustomerDTOResponse fetchHelpDeskCustomer(UUID helpDeskCustomerPublicId, Principal principal) {
        HelpDeskCustomer helpDeskCustomer = helpDeskCustomerRepository.findTopByExistingCustomerPublicIdAndDeletedIsFalse(helpDeskCustomerPublicId).orElse(null);
        if (helpDeskCustomer != null) {
            return helpDeskCustomerDTOResponseConverter.convert(helpDeskCustomer);
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Help desk customer not found")
                    .userMessage("Sorry, Help desk customer not found");
        }
    }

    @Override
    public void deleteFAQ(UUID publicId, Principal principal) {
        Person updatedBy = getPerson(principal.getName());
        CustomerFAQ customerFAQ = customerFAQRepository.findByPublicIdAndDeletedIsFalse(publicId).orElse(null);
        if (customerFAQ == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Faq not found")
                    .userMessage("Sorry, Faq not found");
        }
        customerFAQ.setUpdatedBy(updatedBy);
        customerFAQ.setDeleted(true);
        customerFAQRepository.save(customerFAQ);
    }

    @Override
    public void tumaSmsKwaMteja(UUID customerPublicId, TumaSmsKwaMtejaDto tumaSmsKwaMtejaDto) {
        Customers customers = customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId).orElse(null);
        if (customers == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer not found")
                    .userMessage("Sorry, customer not found");
        }

        String requestMessage = tumaSmsKwaMtejaDto.messageBody();

        if (customers.getPhoneNumber() == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer does not have phone number")
                    .userMessage("Sorry, Customer does not have phone number");
        }

        this.tumaUjumbeKwaMteja(customers.getPhoneNumber(), requestMessage, customers.getFullName());
    }

    @Transactional
    @Override
    public void createFollowUpDate(UUID customerPublicId, LocalDate date, String time, Principal principal) {
        Customers customers = customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId).orElse(null);
        if (customers == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer not found.")
                    .userMessage("Sorry, Customer not found.");
        }
//        customers.setCallBackDate(date);
        customers.setFollowUpDate(date);
        customers.setFollowUpTime(time);

        Person updatedBy = getPerson(principal.getName());
        FollowUp followUp = new FollowUp();
        followUp.setFollowUpDate(date);
        followUp.setFollowUpTime(time);
        followUp.setCreatedBy(updatedBy);
        followUp.setCustomerPublicId(customers.getPublicId());
        followUpRepository.save(followUp);
        customersRepository.save(customers);
    }

    @Override
    public Page<FollowUpDto> listFollowUpDate(UUID customerPublicId, Principal principal, Pageable pageable) {
        Page<FollowUp> followUpList = followUpRepository.findByCustomerPublicIdAndDeletedIsFalse(customerPublicId, pageable);

        if (followUpList.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<FollowUpDto> dtoList = followUpList.stream()
                .map(item -> FollowUpDto.builder()
                        .date(DateFormatter.format2DateToString(item.getFollowUpDate()))
                        .time(item.getFollowUpTime())
                        .createdBy(item.getCreatedBy().getFullName())
                        .publicId(item.getPublicId())
                        .build()
                )
                .toList();

        return new PageImpl<>(dtoList, pageable, followUpList.getTotalElements());
    }


    @Override
    public Map<String, String> deleteFollowUpDate(UUID publicId, Principal principal) {
        Person updatedBy = getPerson(principal.getName());
        FollowUp followUp = followUpRepository.findByPublicIdAndDeletedIsFalse(publicId).orElse(null);
        followUp.setDeleted(true);
        followUp.setUpdatedBy(updatedBy);
        FollowUp saveFollowUp = followUpRepository.save(followUp);
        return Map.of("Message", String.format("Follow up date %s, deleted successfully", saveFollowUp.getFollowUpDate()));
    }

    @Override
    public void saveLastContactDate(UUID customerPublicId, Principal principal) {
        customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId)
                .ifPresent(customer -> {
                    customer.setLastContactDate(LocalDateTime.now());
                    customersRepository.save(customer);
                });
    }

    @Override
    public SlaDetailResponse getSlaDetails(UUID ticketPublicId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy hh:mm a");
        ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");
        LocalDateTime nairobiDate = LocalDateTime.now(Clock.system(nairobiZone));

        HelpDesk helpDesk = getHelpDesk(ticketPublicId);

        if (helpDesk == null || helpDesk.getCreationTime() == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("HelpDesk or creation time is missing")
                    .userMessage("Sorry, ticket details are incomplete");
        }

        CustomerExperience customerExperience = customerExperienceRepository
                .findByPublicIdAndDeletedIsFalse(helpDesk.getTemplatePublicId())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Template not found")
                        .userMessage("Sorry, template not found"));

        LocalDateTime resolutionDate = helpDesk.getCreationTime();
        int firstResponse = 0;
        String timeToResolve = "--";

        IssueType issueType = customerExperience.getIssueType();
        if (issueType != null && issueType.getPrioritySetting() != null) {
            PrioritySetting prioritySetting = issueType.getPrioritySetting();

            int duration = safeParseInt(prioritySetting.getSlaDuration());
            if (prioritySetting.getSlaType() == SlaType.Day) {
                resolutionDate = helpDesk.getCreationTime().plusDays(duration);
                timeToResolve = String.format("%d hrs", duration * 24);
            } else if (prioritySetting.getSlaType() == SlaType.Hour) {
                resolutionDate = helpDesk.getCreationTime().plusHours(duration);
                timeToResolve = String.format("%d hrs", duration);
            }

            firstResponse = prioritySetting.getFirstTimeResponse() != null
                    ? prioritySetting.getFirstTimeResponse()
                    : 0;
        }

        String formattedCreationDate = formatter.format(helpDesk.getCreationTime());
        String formattedResolutionDate = formatter.format(resolutionDate);

        Duration remaining = Duration.between(nairobiDate, resolutionDate);

        long days = remaining.toDays();
        long hours = remaining.toHours() % 24;
        long minutes = remaining.toMinutes() % 60;

        String timeRemaining = String.format("%d days %d hrs %d mins", days, hours, minutes);


        Workflow wf = workflowRepository
                .findTopByTemplateAndDeletedIsFalseOrderByCreationTimeDesc(customerExperience)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Workflow not found")
                        .userMessage("Samahi, workflow haipo"));

        Transition selectedTransition = wf.getTransitions().stream()
                .filter(t -> t.getStatusTo().getDepartment().getName()
                        .equalsIgnoreCase(helpDesk.getWorkflowStatus()))
                .sorted(Comparator.comparing(Transition::getTransitionIndex))
                .reduce((first, second) -> second)
                .orElse(null);

        LocalDateTime ticketArrivalTime = null;
        String departmentDuration = "--";
        String departmentTimeRemaining = "--";

        if (selectedTransition != null) {
            ticketArrivalTime = selectedTransition.getTicketArrivalTime();
            int deptDuration = Integer.parseInt(selectedTransition.getDurationNumber());

            Duration deptDurationObj = Duration.ofHours(deptDuration);
            long ddays = deptDurationObj.toDays();
            long dhours = deptDurationObj.toHours() % 24;
            long dmins = deptDurationObj.toMinutes() % 60;
            departmentDuration = String.format("%d days %d hrs %d mins", ddays, dhours, dmins);

            LocalDateTime deadline = ticketArrivalTime.plusHours(deptDuration);
            Duration deptRemaining = Duration.between(nairobiDate, deadline);

            long rdays = deptRemaining.toDays();
            long rhours = deptRemaining.toHours() % 24;
            long rmins = deptRemaining.toMinutes() % 60;

            if (deptRemaining.isNegative()) {
                departmentTimeRemaining = "over due";
            } else {
                departmentTimeRemaining = String.format("%d days %d hrs %d mins", rdays, rhours, rmins);
            }
        }

        return SlaDetailResponse.builder()
                .creationDate(formattedCreationDate)
                .resolveDate(formattedResolutionDate)
                .firstResponse(firstResponse > 0 ? firstResponse + " hrs" : "--")
                .timeToResolution(timeToResolve)
                .timeRemaining(timeRemaining)
                .departmentArrivalTime(ticketArrivalTime != null ? formatter.format(ticketArrivalTime) : "--")
                .departmentDuration(departmentDuration)
                .departmentTimeRemaining(departmentTimeRemaining)
                .build();
    }

    @Override
    public Set<String> listHelpDeskStatus(String status, Principal principal) {
        Set<String> result;
        if (status == null) {
            result = helpDeskRepository.findAllNames();
        } else {
            result = helpDeskRepository.findByAllNames(status);
        }

        return result.stream()
                .filter(Objects::nonNull)
                .filter(s -> !s.equalsIgnoreCase("resolve")
                        && !s.equalsIgnoreCase("resolved"))
                .collect(Collectors.toSet());
    }

    @Override
    public List<TicketHistoryResponse> getTicketHistoryResponse(UUID ticketPublicId) {
        HelpDesk helpDesk = getHelpDesk(ticketPublicId);
        List<TicketHistory> ticketHistoryList = ticketHistoryRepository.findByHelpDeskOrderByActionedTimeDesc(helpDesk);
        if (ticketHistoryList.isEmpty()) {
            return Collections.emptyList();
        }
        return ticketHistoryList.stream().map(ticketHistoryToResponse::convert).toList();
    }

    @Override
    public List<TicketViewerResponse> getTicketViewerResponse(UUID ticketPublicId) {
        HelpDesk helpDesk = getHelpDesk(ticketPublicId);
        List<TicketViewer> ticketViewerList = ticketViewerRepository.findByHelpDeskOrderByViewedAtDesc(helpDesk);
        if (ticketViewerList.isEmpty()) {
            return List.of();
        } else {
            return ticketViewerList.stream().map(ticketViewerToResponse::convert).toList();
        }
    }

    @Override
    public void actionRecordTicketFirstView(UUID ticketPublicId, Principal principal) {
        Person updatedBy = getPerson(principal.getName());

        HelpDesk helpDesk = getHelpDesk(ticketPublicId);
        if (helpDesk.getRead() == Boolean.FALSE) {
            helpDesk.setAssignedTo(updatedBy);
            helpDesk.setRead(Boolean.TRUE);
            HelpDesk saveHelpDesk = helpDeskRepository.save(helpDesk);
            recordTicketHistory(saveHelpDesk, "Viewed Ticket", helpDesk.getWorkflowStatus(), helpDesk.getWorkflowNewStatus(), updatedBy.getFullName());
        }
    }

    @Override
    public void shareAppLink(String customerPhone, UUID smsTemplatePublicId, Principal principal) {
        String lastNineDigits = getLastNineDigits(customerPhone);
        String phoneNumber = phoneNumberHelper.getPhoneNumber("0" + lastNineDigits);
        List<Customers> customer = customersRepository.findByPhoneNumberLastNine(lastNineDigits);

        String customerName = !customer.isEmpty() ? customer.get(0).getName() : customer.get(0).getStoreName();

        SmsTemplate smsTemplate = smsTemplateRepository.findByPublicIdAndDeletedIsFalse(smsTemplatePublicId).orElseThrow(() -> new BadRequestRestApiException().
                developerMessage("Template not found").userMessage("Sorry, template not found"));

        tumaMessage.tumaUjumbeKwaMteja(phoneNumber, smsTemplate.getMessage(), customerName);
    }

    @Override
    public void runNotifyFollowUpGuy() {
        ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");
        LocalDate nairobiDate = LocalDate.now(Clock.system(nairobiZone));

        List<LocalDate> dates = Arrays.asList(nairobiDate, nairobiDate.plusDays(1));
        for (LocalDate date : dates) {
            List<FollowUp> followUpList = followUpRepository.findAllByOneDayTo(date);
            if (!followUpList.isEmpty()) {
                for (FollowUp followUp : followUpList) {
                    String officerName = followUp.getCreatedBy().getFullName();
                    String officerEmail = followUp.getCreatedBy().getEmail();

                    String dateString = date.equals(nairobiDate)
                            ? String.format("today - %s", date)
                            : String.format("tomorrow - %s", date);

                    Customers customer = customersRepository
                            .findByPublicIdAndDeletedIsFalse(followUp.getCustomerPublicId())
                            .orElse(null);

                    String customerName = customer != null ? customer.getName() : "--";
                    String customerTel = customer != null ? customer.getPhoneNumber() : "--";

                    sendEmail(officerName, officerEmail, dateString, customerName, customerTel);
                }
            }
        }
    }


    @Override
    public List<PersonMiniData> getPersonMiniData(UUID ticketPublicId, Principal principal) {
        HelpDesk helpDesk = getHelpDesk(ticketPublicId);
        Department department = departmentRepository.findByNameIgnoreCaseAndDeletedIsFalse(helpDesk.getWorkflowStatus()).orElse(null);

        if (department != null) {
            List<Person> departmentOfficers = personRepository.findByDepartmentAndDeletedIsFalse(department);
            if (!departmentOfficers.isEmpty()) {
                return departmentOfficers.stream().map(this::convertToPersonMiniData).toList();
            } else {
                return Collections.emptyList();
            }
        }
        return List.of();
    }

    private PersonMiniData convertToPersonMiniData(Person person) {
        return PersonMiniData.builder()
                .publicId(person.getPublicId())
                .name(person.getFullName())
                .build();
    }

    private void sendEmail(String officerName, String officerEmail, String dateString, String customerName, String customerTel) {
        String emailBody = emailBodyCrafter.craftFollowUpEmail(officerName, dateString, customerName, customerTel);
        String body = String.format("Hi %s We hope this finds you well. Kindly adhere to this reminder: %s, you have a scheduled follow-up task with customer %s, reachable at telephone number %s. Best regards.", officerName, dateString, customerName, customerTel);

        emailService.sendMessage(officerEmail, "Follow-up Task", null, emailBody, Collections.emptyList());
        dmGChart(body);
    }

    private void dmGChart(String emailBody) {

        String payload = String.format("{\"text\": \"%s\"}", emailBody);

        webhookClient.dmGchartOnFollowUp(payload);
    }

    private void recordTicketHistory(HelpDesk saveHelpDesk, String reason, String currentDepartment, String currentStatus, String fullName) {
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
    }

    private int safeParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }


    private Page<SmsDefaultGroupResponse> tighAndReturnSupplierData(String searchParam, Pageable pageable) {
        Page<Suppliers> suppliers = supplierRepository.findByDeletedIsFalse(pageable);
        List<SmsDefaultGroupResponse> smsDefaultGroupResponseList = suppliers.map(item -> SmsDefaultGroupResponse.builder().publicId(item.getPublicId())
                .name(item.getSupplierName())
                .phoneNumber(item.getPhoneNumber())
                .build()).toList();
        return new PageImpl<>(smsDefaultGroupResponseList, pageable, smsDefaultGroupResponseList.size());
    }

    private SmsGroupUploadResponse createSmsGroup(List<SmsGroupExcelDto> smsGroupExcelDtoList, String groupName, String name) {
        Person loggedInUser = getPerson(name);
        SmsGroup smsGroup = smsGroupRepository.findByNameIgnoreCaseAndDeletedIsFalse(groupName).orElse(null);
        if (smsGroup == null) {
            smsGroup = new SmsGroup();
            smsGroup.setName(groupName);
            smsGroup.setIsActive(Boolean.TRUE);
            smsGroup.setDefaultGroup(Boolean.FALSE);
            smsGroup.setCreatedBy(loggedInUser);
        } else {
            smsGroup.setUpdatedBy(loggedInUser);
            smsGroup.preUpdate();
        }
        SmsGroup saveSmsGroup = smsGroupRepository.save(smsGroup);

        Set<String> knownPhones = new HashSet<>();
        for (SmsGroupEntity existingMember : saveSmsGroup.getSmsGroupEntityList()) {
            knownPhones.add(toInternationalFormat(existingMember.getPhone()));
        }

        Map<Integer, String> errorsByRow = new TreeMap<>();
        List<SmsGroupEntity> smsGroupEntityList = new ArrayList<>();
        int duplicateCount = 0;

        for (SmsGroupExcelDto smsGroupExcelDto : smsGroupExcelDtoList) {
            if (!smsGroupExcelDto.getErrorList().isEmpty()) {
                errorsByRow.put(smsGroupExcelDto.getRowIndex(), String.join("; ", smsGroupExcelDto.getErrorList().values()));
                continue;
            }

            Map<Boolean, String> phoneCheck = checkPhoneNumber(smsGroupExcelDto.getPhone());
            if (phoneCheck.containsKey(Boolean.FALSE)) {
                errorsByRow.put(smsGroupExcelDto.getRowIndex(), phoneCheck.get(Boolean.FALSE));
                continue;
            }

            String internationalPhone = toInternationalFormat(smsGroupExcelDto.getPhone());
            if (!knownPhones.add(internationalPhone)) {
                duplicateCount++;
                continue;
            }

            SmsGroupEntity smsGroupEntity = new SmsGroupEntity();
            smsGroupEntity.setName(smsGroupExcelDto.getName());
            // stored without "+" (e.g. 255713347613), matching the upload template
            smsGroupEntity.setPhone(internationalPhone.substring(1));
            smsGroupEntity.setSmsGroup(saveSmsGroup);
            smsGroupEntityList.add(smsGroupEntity);
        }

        if (!smsGroupEntityList.isEmpty()) {
            smsGroupEntityRepository.saveAll(smsGroupEntityList);
        }

        saveSmsGroup.setErrorList(createErrorLogs(errorsByRow));
        smsGroupRepository.save(saveSmsGroup);

        List<SmsGroupUploadResponse.RowError> errors = errorsByRow.entrySet().stream()
                .map(entry -> new SmsGroupUploadResponse.RowError(entry.getKey(), entry.getValue()))
                .toList();

        return SmsGroupUploadResponse.builder()
                .groupPublicId(saveSmsGroup.getPublicId())
                .groupName(saveSmsGroup.getName())
                .totalRows(smsGroupExcelDtoList.size())
                .addedCount(smsGroupEntityList.size())
                .duplicateCount(duplicateCount)
                .failedCount(errors.size())
                .errors(errors)
                .build();
    }

    private ObjectNode createErrorLogs(Map<Integer, String> errorsByRow) {
        if (errorsByRow.isEmpty()) {
            return null;
        }

        ObjectNode node = mapper.createObjectNode();
        for (Map.Entry<Integer, String> entry : errorsByRow.entrySet()) {
            node.put(String.format("Row %d", entry.getKey()), entry.getValue());
        }
        return node;
    }

    private Predicate[] buildPredicates(
            CriteriaBuilder cb,
            Root<HelpDesk> root,
            String helpDeskStatus,
            TicketPriority ticketPriority,
            LocalDate startDate,
            LocalDate endDate,
            String searchParam,
            UUID customerPublicId,
            UUID existingCustomerPublicId,
            UUID departmentPublicId,
            TicketDto.CustomerType customerType) {

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("deleted"), Boolean.FALSE));


        if (helpDeskStatus != null && !helpDeskStatus.isEmpty() && helpDeskStatus.trim().length() >= 3) {
            String search = "%" + helpDeskStatus.toUpperCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.upper(root.get("workflowNewStatus")), search)
            ));
        }

        if (ticketPriority != null) {
            predicates.add(cb.equal(root.get("ticketPriority"), ticketPriority));
        }

        if (departmentPublicId != null) {
            Join<HelpDesk, Department> deptJoin = root.join("template").join("departmentInCopy", JoinType.LEFT);

            predicates.add(cb.or(
                    cb.equal(root.get("section").get("publicId"), departmentPublicId),
                    cb.equal(deptJoin.get("publicId"), departmentPublicId)
            ));
        }

        if (existingCustomerPublicId != null) {
            HelpDeskCustomer helpDeskCustomer = helpDeskCustomerRepository.findTopByExistingCustomerPublicIdAndDeletedIsFalse(existingCustomerPublicId).orElse(null);
            if (helpDeskCustomer != null) {
                predicates.add(cb.equal(root.get("helpDeskCustomer").get("publicId"), helpDeskCustomer.getPublicId()));
            }
        }

        if (customerPublicId != null) {
            predicates.add(cb.equal(root.get("helpDeskCustomer").get("publicId"), customerPublicId));
        }

        if (customerType != null) {
            predicates.add(cb.equal(root.get("helpDeskCustomer").get("customerType"), customerType));
        }

        if (startDate != null && endDate != null) {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
            predicates.add(cb.between(root.get("creationTime"), startDateTime, endDateTime));
        }

        if (searchParam != null && searchParam.trim().length() >= 3) {
            String search = "%" + searchParam.toUpperCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.upper(root.get("ticketNumber")), search),
                    cb.like(cb.upper(root.get("workflowStatus")), search),
                    cb.like(cb.upper(root.get("helpDeskCustomer").get("name")), search),
                    cb.like(cb.upper(root.get("helpDeskCustomer").get("phoneNumber")), search)
            ));
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

    public HelpDesk getHelpDesk(UUID publicId) {
        HelpDesk helpDesk = helpDeskRepository.findByPublicId(publicId).orElse(null);
        if (helpDesk == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Ticket not found")
                    .userMessage("Sorry, Ticket not found");
        }
        return helpDesk;
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

    public IssueType fetchIssueType(UUID publicId) {
        IssueType issueType = issueTypeRepository.findByPublicId(publicId).orElse(null);
        if (issueType == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Issue Type not found")
                    .userMessage("Sorry, Issue type not found");
        }
        return issueType;
    }

    public void craftAndSaveComment(Person actionedBy, HelpDesk helpDesk, String comment) {

        HelpDeskComment helpDeskComment = new HelpDeskComment();
        helpDeskComment.setCommentBody(comment);
        helpDeskComment.setHelpDesk(helpDesk);
        helpDeskComment.setCreatedBy(actionedBy);
        HelpDeskComment savedHelpDeskComment = helpDeskCommentRepository.save(helpDeskComment);
        commentConverter.convert(savedHelpDeskComment);
    }

    private void createHelpDeskNotification(UUID publicId, String notificationHeader) {
        HelpDeskNotification newHelpNotification = new HelpDeskNotification();
        newHelpNotification.setRead(Boolean.FALSE);
        newHelpNotification.setMessageAction(notificationHeader);
        newHelpNotification.setAssignedToPublicId(publicId);
        newHelpNotification.setCreationTime(LocalDateTime.now());
        helpDeskNotificationRepository.save(newHelpNotification);
    }

    private Person getAssignedTo(@NotNull UUID publicId) {

        Person person = personRepository.findByPublicIdAndDeletedIsFalse(publicId).orElse(null);
        if (person == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("person not found")
                    .userMessage("Sorry, person not found");
        }
        return person;
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
                emailService.sendMessage(allUsersInRole.stream().findFirst().get().getEmail(), "Ticket Allocation", null, emailBody, Collections.emptyList());
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

    private Predicate[] buildPredicatesV2(
            CriteriaBuilder cb,
            Root<HelpDeskCustomer> root,
            LocalDate startDate,
            LocalDate endDate,
            String searchParam) {

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("deleted"), Boolean.FALSE));

        if (startDate != null && endDate != null) {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
            predicates.add(cb.between(root.get("creationTime"), startDateTime, endDateTime));
        }

        if (searchParam != null && searchParam.trim().length() >= 3) {
            String search = "%" + searchParam.toUpperCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.upper(root.get("name")), search),
                    cb.like(cb.upper(root.get("phoneNumber")), search)
            ));
        }

        return predicates.toArray(new Predicate[0]);
    }

    public void sendSmsViaAfricaStalking(String phoneNumber, String message, Set<String> customVariable) {
        List<Customers> customers = customersRepository.findByDeletedIsFalseAndPhoneNumberIgnoreCase(phoneNumber);
        String message1 = "Greetings";
        Customers customer = null;

        if (!customers.isEmpty()) {
            customer = customers.get(0);
            Map<String, String> stringStringMap = extractFields(customer, customVariable.stream().toList());

            StringSubstitutor sub = new StringSubstitutor(stringStringMap, "{", "}");
            message1 = sub.replace(message);
        }

        try {
            String[] recipients = {phoneNumber};

            List<Recipient> response = sendSms(recipients, message1);

            log.info("SMS sent successfully to {}", response);
            log.info("-----message1---- {}", message1);

            for (Recipient recipient : response) {
                log.info("SMS sent to {} with status: {}, messageId {}", recipient.number, recipient.status, recipient.messageId);
                if (customer != null) {
                    saveCustomer(phoneNumber, customer.getFullName(), message1, recipient.status, null);
                }
            }
        } catch (Exception e) {
            log.error("Error sending SMS via AfricaStalking", e);
        }
    }

    @Override
    public void sendSmsToActiveCustomers(Customers customer, String message, Set<String> customVariable, String campaignName) {
        String message1 = customer != null ? personaliseCustomerMessage(customer, message, customVariable) : "Greetings";

        String lastNineDigits = getLastNineDigits(customer.getPhoneNumber());
        String phoneNumber = phoneNumberHelper.getPhoneNumber("0" + lastNineDigits);

        log.info("---campaignName {}, ", campaignName);

        try {
            String[] recipients = {phoneNumber};

            List<Recipient> response = sendSms(recipients, message1);

            for (Recipient recipient : response) {
                log.info("SMS sent to {} with status: {}, messageId {}", recipient.number, recipient.status, recipient.messageId);
                if (customer != null) {
                    saveCustomer(customer.getPhoneNumber(), customer.getFullName(), message1, recipient.status, campaignName);
                }
            }
        } catch (Exception e) {
            log.error("Error sending SMS via AfricaStalking", e);
        }
    }

    @Override
    public void sendWhatsAppToActiveCustomers(Customers customer, String message, Set<String> customVariable) {
        String personalisedMessage = personaliseCustomerMessage(customer, message, customVariable);
        String phoneNumber = toInternationalFormat(customer.getPhoneNumber());

        try {
            tumaMessage.sendWhatsAppBroadcast(phoneNumber, customer.getFullName(), personalisedMessage);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted sending WhatsApp to {}", phoneNumber, e);
        } catch (Exception e) {
            log.error("Error sending WhatsApp to {}", phoneNumber, e);
        }
    }

    private String personaliseCustomerMessage(Customers customer, String message, Set<String> customVariable) {
        Map<String, String> values = extractFields(customer, customVariable.stream().toList());

        if (customVariable.contains("creditLimit") && !customer.isBnplAccountCreated()) {
            values.put("creditLimit", getEligibleCreditLimit(customer));
        }

        return new StringSubstitutor(values, "{", "}").replace(message);
    }

    @Override
    public ByteArrayOutputStream runExportTickets(String searchParam, String helpDeskStatus, TicketPriority ticketPriority, LocalDate startDate, LocalDate endDate, UUID customerPublicId, UUID existingCustomerPublicId, UUID departmentPublicId, TicketDto.CustomerType customerType, StatementExportType exportType, Pageable pageable, Principal principal) throws DocumentException, IOException {
        Page<HelpDeskDTOResponse> fetchedList = getHelpDeskList(
                searchParam,
                helpDeskStatus,
                ticketPriority,
                startDate,
                endDate,
                customerPublicId,
                existingCustomerPublicId,
                departmentPublicId,
                customerType,
                pageable);

        if (exportType == StatementExportType.EXCEL) {
            return ticketReportHelper.createExcelExport(fetchedList.getContent(), principal.getName());
        } else {
            return ticketReportHelper.createPdfExport(fetchedList, principal.getName());
        }
    }

    @Override
    public void inBoundTumaUjumbe(String customerPhone, TicketDto.CustomerType customerType, UUID smsTemplatePublicId, Principal principal) {
        if (customerType == TicketDto.CustomerType.Existing_Customer) {
            shareAppLink(customerPhone, smsTemplatePublicId, principal);
        } else {
            String lastNineDigits = getLastNineDigits(customerPhone);
            String phoneNumber = phoneNumberHelper.getPhoneNumber("0" + lastNineDigits);

            SmsTemplate smsTemplate = smsTemplateRepository.findByPublicIdAndDeletedIsFalse(smsTemplatePublicId).orElseThrow(() -> new BadRequestRestApiException().
                    developerMessage("Template not found").userMessage("Sorry, template not found"));

            tumaMessage.tumaUjumbeKwaMteja(phoneNumber, smsTemplate.getMessage(), "Mteja");
        }
    }

    private void sendSmsToDeliveryAgent(String phoneNumber, String message, Set<String> customVariable) {
        List<DeliveryAgents> deliveryAgents = deliveryAgentsRepository.findByDeletedIsFalseAndPhoneNumber(phoneNumber);
        String message1 = "Greetings";
        DeliveryAgents deliveryAgents1 = null;
        if (!deliveryAgents.isEmpty()) {
            deliveryAgents1 = deliveryAgents.get(0);
            Map<String, String> stringStringMap = extractFields(deliveryAgents1, customVariable.stream().toList());

            StringSubstitutor sub = new StringSubstitutor(stringStringMap, "{", "}");
            message1 = sub.replace(message);
        }

        try {
            String[] recipients = {phoneNumber};

            List<Recipient> response = sendSms(recipients, message1);

            for (Recipient recipient : response) {
                if (deliveryAgents1 != null) {
                    saveDeliveryAgent(deliveryAgents1, message1, recipient.status);
                }
            }
        } catch (Exception e) {
            log.error("Error sending SMS via AfricaStalking", e);
        }
    }

    private void sendSmsToSalesAgent(String phoneNumber, String message, Set<String> customVariable) {
        List<SalesAgents> salesAgents = salesAgentsRepository.findByDeletedIsFalseAndPhoneNumber(phoneNumber);
        String message1 = "Greetings";
        SalesAgents salesAgents1 = null;
        if (!salesAgents.isEmpty()) {
            salesAgents1 = salesAgents.get(0);
            Map<String, String> stringStringMap = extractFields(salesAgents1, customVariable.stream().toList());

            StringSubstitutor sub = new StringSubstitutor(stringStringMap, "{", "}");
            message1 = sub.replace(message);
        }

        try {
            String[] recipients = {phoneNumber};

            List<Recipient> response = sendSms(recipients, message1);

            for (Recipient recipient : response) {
                if (salesAgents1 != null) {
                    saveSAlesAgent(salesAgents1, message1, recipient.status);
                }
            }
        } catch (Exception e) {
            log.error("Error sending SMS via AfricaStalking", e);
        }
    }

//    private void sendSmsToSalesAgent(String phoneNumber, String message, Set<String> customVariable) {
//        List<SalesAgents> salesAgents = salesAgentsRepository.findByDeletedIsFalseAndPhoneNumber(phoneNumber);
//        String message1 = "Greetings";
//        SalesAgents salesAgents1 = null;
//        if (!salesAgents.isEmpty()) {
//            salesAgents1 = salesAgents.get(0);
//            Map<String, String> stringStringMap = extractFields(salesAgents1, customVariable.stream().toList());
//
//            StringSubstitutor sub = new StringSubstitutor(stringStringMap, "{", "}");
//            message1 = sub.replace(message);
//        }
//
//        try {
//            String[] recipients = {phoneNumber};
//
//            List<Recipient> response = sendSms(recipients, message1);
//
//            for (Recipient recipient : response) {
//                if (recipient.status.equals("Success")) {
//                    if (salesAgents1 != null) {
//                        saveSAlesAgent(salesAgents1, message1);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            log.error("Error sending SMS via AfricaStalking", e);
//        }
//    }


    private TumaSms saveCustomerMessage() {
        TumaSms tumaSms = new TumaSms();
        tumaSms.setIsCustomer(Boolean.TRUE);
        return tumaSmsRepository.save(tumaSms);
    }

    private void saveCustomer(String phone, String name, String message, String status, String campaignName) {
        TumaSms tumaSms = new TumaSms();
        tumaSms.setTextMessage(message);
        tumaSms.setPhoneNumber(phone);
        tumaSms.setRecipientName(name);
        tumaSms.setIsSuccess(status.equals("Success") ? Boolean.TRUE : Boolean.FALSE);
        tumaSms.setIsCustomer(Boolean.TRUE);

        if (campaignName != null && !campaignName.isEmpty() && campaignName.length() > 2) {
            tumaSms.setIsCampaign(Boolean.TRUE);
            tumaSms.setCampaignName(campaignName);
        }
        tumaSmsRepository.save(tumaSms);
    }

    private void saveDeliveryAgent(DeliveryAgents deliveryAgents, String message, String status) {
        TumaSms tumaSms = new TumaSms();
        tumaSms.setTextMessage(message);
        tumaSms.setPhoneNumber(deliveryAgents.getPhoneNumber());
        tumaSms.setRecipientName(deliveryAgents.getFullName());
        tumaSms.setIsSuccess(status.equals("Success") ? Boolean.TRUE : Boolean.FALSE);
        tumaSms.setIsDeliveryAgent(Boolean.TRUE);
        tumaSmsRepository.save(tumaSms);
    }

    private void saveSAlesAgent(SalesAgents salesAgents, String message, String status) {
        TumaSms tumaSms = new TumaSms();
        tumaSms.setTextMessage(message);
        tumaSms.setPhoneNumber(salesAgents.getPhoneNumber());
        tumaSms.setRecipientName(salesAgents.getName());
        tumaSms.setIsSuccess(status.equals("Success") ? Boolean.TRUE : Boolean.FALSE);
        tumaSms.setIsSalesAgent(Boolean.TRUE);
        tumaSmsRepository.save(tumaSms);
    }

    private void saveSmsGroup(String name, String phone, String message, String status, String campaignName) {
        TumaSms tumaSms = new TumaSms();
        tumaSms.setTextMessage(message);
        tumaSms.setPhoneNumber(phone);
        tumaSms.setRecipientName(name);
        tumaSms.setIsSuccess("Success".equals(status));
        tumaSms.setIsGroup(Boolean.TRUE);

        if (campaignName != null && campaignName.length() > 2) {
            tumaSms.setIsCampaign(Boolean.TRUE);
            tumaSms.setCampaignName(campaignName);
        }
        tumaSmsRepository.save(tumaSms);
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

    private String getEligibleCreditLimit(Customers customer) {
        BigDecimal aov = Objects.requireNonNullElse(customer.getPreliminaryValue(), BigDecimal.ZERO);
        BigDecimal creditLimit = bnplHelper.getCreditLimitForCustomer(customer.getCustomerType(), aov);
        return String.format("%,.0f", creditLimit);
    }

    public Map<String, String> extractFields(Object entity, List<String> fieldNames) {
        Map<String, String> values = new HashMap<>();
        Class<?> clazz = entity.getClass();

        for (String fieldName : fieldNames) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(entity);
                values.put(fieldName, value != null ? value.toString() : "");
            } catch (NoSuchFieldException | IllegalAccessException e) {
                values.put(fieldName, "");
            }
        }
        return values;
    }

    public static XSSFWorkbook createWorkbook() {

        return new XSSFWorkbook();
    }

    public void setResponseHeader(HttpServletResponse response, String contentType, String extension, String prefix) {

        String timeStamp = String.valueOf(System.currentTimeMillis()), fileName = prefix + timeStamp + extension;
        response.setContentType(contentType);
        String headerKey = "Content-Disposition", headerValue = "attachment; filename=" + fileName;
        response.setHeader(headerKey, headerValue);
    }

    public void writeHeaderLineSmsGroupTemplate(String sheetName) {

        sheet = workbook.createSheet(sheetName);
        Row row = sheet.createRow(0);

        CellStyle cellStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Inter");
        font.setBold(true);
        font.setColor(IndexedColors.SEA_GREEN.getIndex());
        cellStyle.setFont(font);

        int COL_NAME = 0;
        int COL_PHONE = 1;

        createCell(row, COL_NAME, "name*", cellStyle);
        createCell(row, COL_PHONE, "phone*", cellStyle);
    }

    public void createCell(Row row, int columnIndex, Object value, CellStyle style) {

        Cell cell = row.createCell(columnIndex);
        cell.setCellValue((String) value);
        cell.setCellStyle(style);
    }

    private void writeDataLineSmsGroupTemplate(Person loggedInUser) {

        Row row = sheet.createRow(1);

        CellStyle cellStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Arial");
        font.setBold(true);
        cellStyle.setFont(font);

        int COL_NAME = 0;
        int COL_PHONE = 1;

        createCell(row, COL_NAME, "Mark Cleo", cellStyle);
        createCell(row, COL_PHONE, "255713347613", cellStyle);
    }

    private boolean isRowEmpty(Row row) {

        if (row == null) {
            return true;
        }
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            if (!readCellAsString(row.getCell(i)).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void tumaUjumbeKwaMteja(String phoneNumber, String message, String customerName) {

        try {
            String[] recipients = {phoneNumber};

            List<Recipient> response = sendSms(recipients, message);

            for (Recipient recipient : response) {
                log.info("SMS sent to {} with status: {}, messageId {}", recipient.number, recipient.status, recipient.messageId);
                saveCustomer(phoneNumber, customerName, message, recipient.status, null);
            }
        } catch (Exception e) {
            log.error("Error sending SMS via AfricaStalking", e);
        }
    }
}


