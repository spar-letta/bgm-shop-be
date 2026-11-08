package com.app.itibari.services;

import com.app.itibari.converter.BnplLeadNotesToDTO;
import com.app.itibari.converter.CustomerNotesToResponse;
import com.app.itibari.dataTypes.*;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.*;
import com.app.itibari.models.response.*;
import com.app.itibari.repository.*;
import com.app.itibari.services.helper.BnplHelper;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.formatter.DateFormatter;
import com.app.itibari.utils.helpers.PhoneNumberHelper;
import com.app.itibari.utils.specification.CustomerSpecification;
import com.app.itibari.utils.specification.LeadSpecification;
import com.app.itibari.utils.specification.PurchaseOrderSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BnplCrmServiceImpl implements BnplCrmService {

    private final BnplLeadRepository bnplLeadRepository;
    private final BnplCallAttemptRepository bnplCallAttemptRepository;
    private final BnplFieldVisitRepository bnplFieldVisitRepository;
    private final BnplReasonCodeRepository bnplReasonCodeRepository;
    private final CustomersRepository customersRepository;
    private final PersonRepository personRepository;
    private final BnplHelper bnplHelper;
    private final RejectionReasonRepository rejectionReasonRepository;
    private final RejectionCodeRepository rejectionCodeRepository;
    private final CustomerNotesRepository customerNotesRepository;
    private final CustomerNotesToResponse customerNotesToResponse;
    private final BnplLeadNotesToDTO bnplLeadNotesToDTO;
    private final SalesAgentsRepository salesAgentsRepository;
    private final SalesOrdersRepository salesOrdersRepository;
    private final ItibariSmsService itibariSmsService;
    private final PhoneNumberHelper phoneNumberHelper;
    private final YeastarCallService yeastarCallService;
    private final CallStatusRepository callStatusRepository;

    @Value("${bnpl.app-link:}")
    private String bnplAppLink;

    @Override
    @Transactional(readOnly = true)
    public BnplLeadPageable getLeads(String search, BnplFunnelStage funnelStage, BnplLeadStatus leadStatus, UUID callStatus, UUID locationId, UUID wardId, Boolean vipOverOneMillion, Pageable pageable) {
        boolean vip = vipOverOneMillion != null ? vipOverOneMillion : false;

        Specification<BnplLead> specification = Specification
                .where(LeadSpecification.searchParamMatches(search))
                .and(LeadSpecification.isNotDeleted())
                .and(LeadSpecification.hasHundredThousandAndAbovePotentialCredit())
                .and(LeadSpecification.hasFunnelStage(funnelStage))
                .and(LeadSpecification.hasLeadStatus(leadStatus))
                .and(LeadSpecification.hasCallStatus(callStatus))
                .and(LeadSpecification.hasLocation(locationId))
                .and(LeadSpecification.hasWard(wardId))
                .and(LeadSpecification.isOverOneMillion(vip));

        Page<BnplLead> page = bnplLeadRepository.findAll(specification, pageable);
        BnplLeadPageable response = new BnplLeadPageable();
        response.setTotalElements(page.getTotalElements());
        response.setPageSize(page.getSize());
        response.setTotalPages(page.getTotalPages());
        response.setLast(page.isLast());
        response.setFirst(page.isFirst());
        response.setPageNumber(page.getNumber());
        response.setContent(page.getContent().stream().map(this::mapLeadToResponse).collect(Collectors.toList()));
        return response;
    }

    @Override
    @Transactional
    public BnplLeadPageable getQualifiedLeads(String search, BnplFunnelStage funnelStage, BnplLeadStatus leadStatus, UUID callStatus, UUID locationId, UUID wardId, Boolean vipOverOneMillion, Pageable pageable) {
        boolean vip = vipOverOneMillion != null ? vipOverOneMillion : false;
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Africa/Nairobi"));
        Specification<Customers> customerSpecification = Specification
                .where(CustomerSpecification.isNotDeleted())
                .and(CustomerSpecification.hasHundredThousandAndAbovePotentialCredit())
                .and(CustomerSpecification.hasNotOnboardedCustomerWithThreeOrdersInEachRecentThirtyDayPeriod(now));

        List<Customers> qualifiedCustomers = customersRepository.findAll(customerSpecification);
        qualifiedCustomers.forEach(customer -> createLeadIfMissing(customer, null));

        if (qualifiedCustomers.isEmpty()) {
            BnplLeadPageable response = new BnplLeadPageable();
            response.setTotalElements(0);
            response.setPageSize(pageable.getPageSize());
            response.setTotalPages(0);
            response.setLast(true);
            response.setFirst(true);
            response.setPageNumber(pageable.getPageNumber());
            response.setContent(Collections.emptyList());
            return response;
        }

        Specification<BnplLead> specification = Specification
                .where(LeadSpecification.searchParamMatches(search))
                .and(LeadSpecification.isNotDeleted())
                .and(LeadSpecification.hasFunnelStage(funnelStage))
                .and(LeadSpecification.hasLeadStatus(leadStatus))
                .and(LeadSpecification.hasCallStatus(callStatus))
                .and(LeadSpecification.hasLocation(locationId))
                .and(LeadSpecification.hasWard(wardId))
                .and(LeadSpecification.isOverOneMillion(vip))
                .and((root, query, cb) -> root.get("customer").in(qualifiedCustomers));

        Page<BnplLead> page = bnplLeadRepository.findAll(specification, pageable);
        BnplLeadPageable response = new BnplLeadPageable();
        response.setTotalElements(page.getTotalElements());
        response.setPageSize(page.getSize());
        response.setTotalPages(page.getTotalPages());
        response.setLast(page.isLast());
        response.setFirst(page.isFirst());
        response.setPageNumber(page.getNumber());
        response.setContent(page.getContent().stream().map(this::mapLeadToResponse).collect(Collectors.toList()));
        return response;
    }

    @Override
    @Transactional
    public void cleanupStaleLeads() {
        List<BnplLead> staleLeads = bnplLeadRepository.findStaleLeads(
                LocalDateTime.now(ZoneId.of("Africa/Nairobi")).minusDays(60));
        if (staleLeads.isEmpty()) {
            return;
        }
        staleLeads.forEach(lead -> lead.setDeleted(true));
        bnplLeadRepository.saveAll(staleLeads);
        log.info("Soft-deleted {} BNPL CRM leads due to ineligibility or 60 days without an order", staleLeads.size());
    }

    @Override
    @Transactional
    public void createLeads() {
        cleanupStaleLeads();

        Page<BNPLEligibleCustomerDTO> customers = bnplHelper.getEligibleCustomersForBNPLAccountCreation(null, null, null, null, PageRequest.of(0, 1000));

        if (customers.isEmpty()) {
            return;
        }

        LocalDateTime inactiveBefore = LocalDateTime.now(ZoneId.of("Africa/Nairobi")).minusDays(60);
        customers.forEach(customer -> {
            Optional<BnplLead> optionalBnplLead = bnplLeadRepository.findByCustomerPublicIdAndDeletedIsFalse(customer.getPublicId().toString());
            if (optionalBnplLead.isEmpty()) {

                Customers customerLead = customersRepository.findByPublicIdAndDeletedIsFalse(customer.getPublicId()).orElse(null);

                if (customerLead != null) {
                    // Keep inactive customers out of the lead list until they place another order.
                    if (bnplLeadRepository.hasInactiveDeletedLead(customerLead, inactiveBefore)) {
                        return;
                    }
                    log.info("Creating Lead with id {}", customer.getPublicId().toString());

                    createLeadIfMissing(customerLead, customer.getGmv());
                }
            }
        });

    }

    private void createLeadIfMissing(Customers customer, BigDecimal loanEligibilityAmount) {
        if (bnplLeadRepository.findByCustomerPublicIdAndDeletedIsFalse(customer.getPublicId().toString()).isPresent()) {
            return;
        }

        BigDecimal creditLimit = bnplHelper.getCreditLimitForCustomer(customer.getCustomerType(), customer.getPreliminaryValue());
        BnplLead lead = BnplLead.builder()
                .customer(customer)
                .phoneNumber(customer.getPhoneNumber())
                .loanEligibilityAmount(loanEligibilityAmount)
                .funnelStage(BnplFunnelStage.PROSPECT)
                .leadStatus(null)
                .assignedAgent(null)
                .totalCallAttempts(0)
                .callbackFlagged(Boolean.FALSE)
                .customerPublicId(customer.getPublicId().toString())
                .potentialCreditLimit(creditLimit)
                .build();

        bnplLeadRepository.save(lead);
    }

    @Override
    @Transactional(readOnly = true)
    public BnplLeadResponse getLead(UUID leadPublicId) {
        BnplLead lead = validateLead(leadPublicId);
        return mapLeadToResponse(lead);
    }

    @Override
    @Transactional
    public void updateLeadStatus(UUID leadPublicId, BnplLeadStatusRequest request) {
        BnplLead lead = validateLead(leadPublicId);

        BnplLeadStatus status = request.getLeadStatus();
        lead.setLeadStatus(status);

        switch (status) {
            case GREEN -> lead.setFunnelStage(BnplFunnelStage.PROSPECT);
            case YELLOW -> lead.setFunnelStage(BnplFunnelStage.PENDING);
            case RED -> lead.setFunnelStage(BnplFunnelStage.REJECTED);
            default -> lead.setFunnelStage(null);
        }

        bnplLeadRepository.save(lead);
    }

    @Override
    public void sendAppLink(UUID leadPublicId) {
        BnplLead lead = validateLead(leadPublicId);

        if (!StringUtils.hasText(bnplAppLink)) {
            throw new BadRequestRestApiException()
                    .developerMessage("BNPL app link is not configured")
                    .userMessage("Sorry, the app link is not configured");
        }

        String phoneNumber = StringUtils.hasText(lead.getPhoneNumber())
                ? lead.getPhoneNumber()
                : lead.getCustomer() != null ? lead.getCustomer().getPhoneNumber() : null;

        if (!StringUtils.hasText(phoneNumber)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Lead customer phone number not found")
                    .userMessage("Sorry, lead customer phone number not found");
        }

        String customerName = lead.getCustomer() != null && StringUtils.hasText(lead.getCustomer().getName())
                ? lead.getCustomer().getName()
                : "Customer";
        String message = String.format("Dear %s, download the Itibari app here: %s", customerName, bnplAppLink);

        itibariSmsService.send(phoneNumberHelper.getPhoneNumber(phoneNumber), message);
    }


    @Override
    @Transactional
    public void flagCallback(UUID leadPublicId, BnplCallbackFlagRequest request) {
        BnplLead lead = validateLead(leadPublicId);
        if (lead.getLeadStatus() != BnplLeadStatus.YELLOW) {
            throw new BadRequestRestApiException()
                    .developerMessage("Callback flagging is only allowed for YELLOW status leads")
                    .userMessage("Only undecided (Yellow) leads can be flagged for callback");
        }
        lead.setCallbackFlagged(Boolean.TRUE);
        lead.setCallbackDate(request.getCallbackDate());
        bnplLeadRepository.save(lead);
    }

    @Override
    @Transactional
    public BnplCallAttemptResponse initiateCall(UUID leadPublicId, String agentEmail) {
        BnplLead lead = validateLead(leadPublicId);

        Person agent = validateLoggedInUser(agentEmail);

        int attemptNumber = (lead.getTotalCallAttempts() != null ? lead.getTotalCallAttempts() : 0) + 1;
        LocalDateTime now = LocalDateTime.now();

        BnplCallAttempt attempt = BnplCallAttempt.builder()
                .lead(lead)
                .agent(agent)
                .attemptNumber(attemptNumber)
                .startTime(now)
                .reasonCodes(new ArrayList<>())
                .build();

        BnplCallAttempt saved = bnplCallAttemptRepository.save(attempt);

        lead.setTotalCallAttempts(attemptNumber);
        lead.setLastCalledAt(now);
        bnplLeadRepository.save(lead);

        // then make actual call here
        YeastarDialRequest request = new YeastarDialRequest();
        request.setCallee(phoneNumberHelper.getPhoneNumber(lead.getPhoneNumber()));
        yeastarCallService.clickToCall(request, agentEmail);

        return mapCallAttemptToResponse(saved);
    }

    @Override
    @Transactional
    public void tagCallOutcome(UUID callPublicId, BnplCallOutcomeRequest request) {
        BnplCallAttempt attempt = bnplCallAttemptRepository.findByPublicIdAndDeletedIsFalse(callPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Call attempt not found")
                        .userMessage("Call attempt not found"));

        attempt.setCallStatus(request.getCallStatus());
        attempt.setNotes(request.getNotes());

        if (request.getReasonCodePublicIds() != null && !request.getReasonCodePublicIds().isEmpty()) {
            List<BnplReasonCode> reasonCodes = bnplReasonCodeRepository.findByPublicIdInAndDeletedIsFalse(request.getReasonCodePublicIds());
            attempt.setReasonCodes(reasonCodes);
            if (!reasonCodes.isEmpty()) {
                String lastReason = reasonCodes.stream().map(BnplReasonCode::getDescription).collect(Collectors.joining(", "));
                attempt.getLead().setLastReason(lastReason);
            }
        }

        if (request.getLeadStatus() != null) {
            BnplLead lead = attempt.getLead();
            lead.setLeadStatus(request.getLeadStatus());
            lead.setScheduledDate(request.getScheduledDate());
            lead.setCallStatusPublicId(request.getCallStatusPublicId() != null ? request.getCallStatusPublicId().toString() : null);
            bnplLeadRepository.save(lead);
        }

        if (request.getCallStatusPublicId() != null) {
            CallStatus callStatus = callStatusRepository.findByPublicIdAndDeletedIsFalse(request.getCallStatusPublicId())
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Call status not found")
                            .userMessage("Call status not found"));
            attempt.setOutComeStatus(callStatus);
        }

        bnplCallAttemptRepository.save(attempt);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BnplCallAttemptResponse> getCallHistory(UUID leadPublicId, Pageable pageable) {
        BnplLead lead = validateLead(leadPublicId);
        return bnplCallAttemptRepository.findByLeadAndDeletedIsFalseOrderByStartTimeDesc(lead, pageable)
                .map(this::mapCallAttemptToResponse);
    }

    @Override
    @Transactional
    public BnplFieldVisitResponse logFieldVisit(UUID leadPublicId, BnplFieldVisitRequest request, String agentEmail) {
        BnplLead lead = validateLead(leadPublicId);

        Person officer = null;
        if (request.getOfficerPublicId() != null) {
            officer = personRepository.findByEmailAndDeletedIsFalse(agentEmail).orElse(null);
        }

        BnplFieldVisit visit = BnplFieldVisit.builder()
                .lead(lead)
                .officer(officer)
                .visitDate(request.getVisitDate())
                .nextVisitDate(request.getNextVisitDate())
                .outcome(request.getOutcome())
                .notes(request.getNotes())
                .customerPublicId(lead.getCustomer() != null ? lead.getCustomer().getPublicId().toString() : null)
                .build();

        BnplFieldVisit saved = bnplFieldVisitRepository.save(visit);

        if (request.getVisitDate() != null) {
            lead.setLastFieldVisitDate(request.getVisitDate());
            bnplLeadRepository.save(lead);
        }

        return mapFieldVisitToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BnplFieldVisitResponse> getVisitHistory(UUID leadPublicId, Pageable pageable) {
        BnplLead lead = validateLead(leadPublicId);
        return bnplFieldVisitRepository.findByLeadAndDeletedIsFalseOrderByVisitDateDesc(lead, pageable)
                .map(this::mapFieldVisitToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BnplReasonCodeResponse> getReasonCodes() {
        return bnplReasonCodeRepository.findByIsActiveTrueAndDeletedIsFalse()
                .stream().map(this::mapReasonCodeToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BnplReasonCodeResponse createReasonCode(BnplReasonCodeRequest request) {
        BnplReasonCode code = BnplReasonCode.builder()
                .code(request.getCode())
                .description(request.getDescription())
                .isActive(Boolean.TRUE)
                .build();
        return mapReasonCodeToResponse(bnplReasonCodeRepository.save(code));
    }

    @Override
    @Transactional
    public void updateReasonCode(UUID codePublicId, BnplReasonCodeRequest request) {
        BnplReasonCode code = bnplReasonCodeRepository.findByPublicIdAndDeletedIsFalse(codePublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Reason code not found")
                        .userMessage("Reason code not found"));
        if (request.getCode() != null) code.setCode(request.getCode());
        if (request.getDescription() != null) code.setDescription(request.getDescription());
        if (request.getIsActive() != null) code.setIsActive(request.getIsActive());
        bnplReasonCodeRepository.save(code);
    }

    @Override
    public BnplLeadNotesResponse addLeadCustomerNotes(UUID leadPublicId, CustomerNotesRequest customerNotesRequest, Principal principal) {
        BnplLead lead = validateLead(leadPublicId);

        Person actionedBy = validateLoggedInUser(principal.getName());

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

        customerNotes.setCreatedBy(actionedBy);
        customerNotes.setNoteType(customerNotesRequest.noteType());
        customerNotes.setContent(customerNotesRequest.content());
        customerNotes.setBnplLead(lead);
        return bnplLeadNotesToDTO.convert(customerNotesRepository.save(customerNotes));
    }

    @Override
    public Page<BnplLeadNotesResponse> listLeadNotes(UUID leadPublicId, Pageable pageable) {
        BnplLead lead = validateLead(leadPublicId);
        return customerNotesRepository.findAllByBnplLeadAndDeletedIsFalse(lead, pageable).map(bnplLeadNotesToDTO::convert);
    }

    @Override
    public BnplLeadNotesResponse updateLeadNotes(UUID notePublicId, CustomerNotesRequest customerNotesRequest, Principal principal) {
        Person actionedBy = validateLoggedInUser(principal.getName());

        CustomerNotes customerNotes = validateCustomerNote(notePublicId);

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
        customerNotes.setUpdatedBy(actionedBy);
        customerNotes.preUpdate();
        return bnplLeadNotesToDTO.convert(customerNotesRepository.save(customerNotes));
    }

    @Override
    public void doAssignSalesAgent(UUID leadPublicId, UUID assignedSalesAgentPublicId, Principal principal) {
        Person loggedInUser = validateLoggedInUser(principal.getName());
        Person fs = personRepository.findByPublicIdAndDeletedIsFalse(assignedSalesAgentPublicId).orElseThrow(() -> new BadRequestRestApiException()
                .developerMessage("Assigned finance office not found")
                .userMessage("Assigned finance office not found"));

        BnplLead lead = validateLead(leadPublicId);
        lead.setAssignedAgent(fs);
        lead.setUpdatedBy(loggedInUser);
        bnplLeadRepository.save(lead);
    }

    @Override
    public Page<AssignedBnplCustomerResponse> listAssignedCustomers(Principal principal, Pageable pageable) {
        Person financeServiceOfficer = validateLoggedInUser(principal.getName());

        if (financeServiceOfficer == null) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
        return bnplLeadRepository.findByAssignedAgentAndDeletedIsFalse(financeServiceOfficer, pageable).map(this::convertLeadToResponse);
    }

    private AssignedBnplCustomerResponse convertLeadToResponse(BnplLead bnplLead) {
        return AssignedBnplCustomerResponse.builder()
                .publicId(bnplLead.getPublicId())
                .name(bnplLead.getCustomer().getFullName())
                .telephone(bnplLead.getPhoneNumber())
                .scheduledDate(DateFormatter.format2DateToString(bnplLead.getScheduledDate()))
                .location(bnplLead.getCustomer().getOrganizationRegions().getName())
                .location(bnplLead.getCustomer().getOrganizationLocation().getName())
                .build();
    }

    private BnplLeadResponse mapLeadToResponse(BnplLead lead) {

        String district = Optional.ofNullable(lead.getCustomer())
                .map(Customers::getOrganizationRegions)
                .map(OrganizationRegions::getName)
                .orElse("--");

        String ward = Optional.ofNullable(lead.getCustomer())
                .map(Customers::getOrganizationLocation)
                .map(OrganizationLocations::getName)
                .orElse("--");

        long deliveredOrdersCount = getDeliveredOrdersCount(lead.getCustomer());
        BigDecimal preliminaryValue = lead.getCustomer() != null ? lead.getCustomer().getPreliminaryValue() : null;

        BigDecimal gmv = (preliminaryValue != null)
                ? BigDecimal.valueOf(deliveredOrdersCount).multiply(preliminaryValue)
                : BigDecimal.ZERO;

        String outcome = lead.getBnplFieldVisits() == null ? "--" :
                lead.getBnplFieldVisits().stream()
                        .reduce((first, second) -> second)
                        .map(BnplFieldVisit::getOutcome)
                        .orElse("--");
// get latest call attempt

        List<BnplCallAttempt> bnplCallAttempts = bnplCallAttemptRepository.findByLeadAndDeletedIsFalse(lead);

        BnplCallAttempt bnplCallAttempt = bnplCallAttempts.stream()
                .max(Comparator.comparing(BnplCallAttempt::getStartTime))
                .orElse(null);

        String latestCustomerResponse = bnplCallAttempt != null && bnplCallAttempt.getReasonCodes() != null
                ? bnplCallAttempt.getReasonCodes().stream()
                .filter(Objects::nonNull)
                .map(BnplReasonCode::getDescription)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "))
                : null;

        BnplLeadResponse r = new BnplLeadResponse();
        r.setPublicId(lead.getPublicId());
        r.setCustomerPublicId(lead.getCustomer() != null ? lead.getCustomer().getPublicId() : null);
        r.setCustomerName(lead.getCustomer() != null ? lead.getCustomer().getName() : null);
        r.setPhoneNumber(lead.getPhoneNumber());
        r.setLoanEligibilityAmount(lead.getLoanEligibilityAmount());
        r.setFunnelStage(lead.getFunnelStage());
        r.setLeadStatus(lead.getLeadStatus());
        r.setAssignedAgentName(lead.getAssignedAgent() != null ? lead.getAssignedAgent().getFullName() : null);
        r.setTotalCallAttempts(lead.getTotalCallAttempts());
        r.setLastCalledAt(lead.getLastCalledAt());
        r.setLastFieldVisitDate(lead.getLastFieldVisitDate());
        r.setCallbackDate(lead.getCallbackDate());
        r.setCallbackFlagged(lead.getCallbackFlagged());
        r.setCreationTime(lead.getCreationTime());
        r.setScheduledDate(lead.getScheduledDate() != null ? DateFormatter.format2DateToString(lead.getScheduledDate()) : null);
        r.setAssignedAgentPublicId(lead.getAssignedAgent() != null ? lead.getAssignedAgent().getPublicId() : null);
        r.setLatitude(lead.getCustomer() != null ? lead.getCustomer().getLatitude() : null);
        r.setLongitude(lead.getCustomer() != null ? lead.getCustomer().getLongitude() : null);
        r.setAov(preliminaryValue);
        r.setDistrict(district);
        r.setWard(ward);
        r.setGmv(gmv);
        r.setLastReason(latestCustomerResponse != null && !latestCustomerResponse.isEmpty() ? latestCustomerResponse : null);
        r.setOutcome(outcome);
        r.setBnplVerificationStatus(lead.getCustomer() != null ? lead.getCustomer().getBnplVerificationStatus() : null);
        r.setCreditLimit(lead.getPotentialCreditLimit());
        r.setCallStatus(mapCallStatusSummary(lead.getCustomer() != null ? lead.getCustomer().getCallStatus() : null));
        r.setPreviousOrderDate(lead.getCustomer() != null ? lead.getCustomer().getPreviousOrderDate() : null);
        r.setOutComeStatus(bnplCallAttempt != null ? mapCallStatusSummary(bnplCallAttempt.getOutComeStatus()) : null);
        r.setSalesAgent(lead.getCustomer().getServedBy() != null ? new SalesAgentDTO(lead.getCustomer().getServedBy().getPublicId(), lead.getCustomer().getServedBy().getName()) : null);
        return r;
    }

    private CallStatusSummary mapCallStatusSummary(CallStatus callStatus) {
        if (callStatus == null) {
            return null;
        }

        return new CallStatusSummary(
                callStatus.getPublicId(),
                callStatus.getCode(),
                callStatus.getDescription()
        );
    }

    private Pageable prioritizeUnsetLeadStatus(Pageable pageable) {
        Sort secondary = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "creationTime");
        Sort sort = Sort.by(Sort.Order.asc("leadStatus").with(Sort.NullHandling.NULLS_FIRST)).and(secondary);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private long getDeliveredOrdersCount(Customers customer) {
        if (customer == null || customer.getPublicId() == null) {
            return 0;
        }

        return salesOrdersRepository
                .countOrdersByCustomerPublicIdsAndStatus(List.of(customer.getPublicId()), LoadingStatusEnum.DELIVERED)
                .stream()
                .findFirst()
                .map(row -> (Long) row[1])
                .orElse(0L);
    }

    private BnplCallStatus resolveBnplCallStatus(BnplCallAttempt attempt) {
        if (attempt.getCallStatus() != null) {
            return attempt.getCallStatus();
        }
        CallStatus outcomeStatus = attempt.getOutComeStatus();
        if (outcomeStatus == null || outcomeStatus.getCode() == null) {
            return null;
        }
        try {
            return BnplCallStatus.valueOf(outcomeStatus.getCode());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private BnplCallAttemptResponse mapCallAttemptToResponse(BnplCallAttempt attempt) {
        BnplCallAttemptResponse r = new BnplCallAttemptResponse();
        r.setPublicId(attempt.getPublicId());
        r.setLeadPublicId(attempt.getLead() != null ? attempt.getLead().getPublicId() : null);
        r.setAgentName(attempt.getAgent() != null ? attempt.getAgent().getFullName() : null);
        r.setAttemptNumber(attempt.getAttemptNumber());
        r.setStartTime(attempt.getStartTime());
        r.setCallStatus(resolveBnplCallStatus(attempt));
        if (attempt.getOutComeStatus() != null) {
            r.setOutcomeStatusCode(attempt.getOutComeStatus().getCode());
            r.setOutcomeStatusDescription(attempt.getOutComeStatus().getDescription());
        }
        r.setNotes(attempt.getNotes());
        r.setReasonCodes(attempt.getReasonCodes() != null
                ? attempt.getReasonCodes().stream().map(this::mapReasonCodeToResponse).collect(Collectors.toList())
                : new ArrayList<>());
        r.setCreationTime(attempt.getCreationTime());
        return r;
    }

    private BnplFieldVisitResponse mapFieldVisitToResponse(BnplFieldVisit visit) {
        BnplFieldVisitResponse r = new BnplFieldVisitResponse();
        r.setPublicId(visit.getPublicId());
        r.setLeadPublicId(visit.getLead() != null ? visit.getLead().getPublicId() : null);
        r.setOfficerName(visit.getOfficer() != null ? visit.getOfficer().getFullName() : null);
        r.setVisitDate(visit.getVisitDate());
        r.setNextVisitDate(visit.getNextVisitDate());
        r.setOutcome(visit.getOutcome());
        r.setNotes(visit.getNotes());
        r.setCustomerName(visit.getLead().getCustomer().getFullName());
        r.setCreationTime(visit.getCreationTime());
        return r;
    }

    private BnplReasonCodeResponse mapReasonCodeToResponse(BnplReasonCode code) {
        BnplReasonCodeResponse r = new BnplReasonCodeResponse();
        r.setPublicId(code.getPublicId());
        r.setCode(code.getCode());
        r.setDescription(code.getDescription());
        r.setIsActive(code.getIsActive());
        r.setCreationTime(code.getCreationTime());
        return r;
    }

    private CustomerNotes validateCustomerNote(UUID publicId) {
        CustomerNotes customerNotes = customerNotesRepository.findByPublicIdAndDeletedIsFalse(publicId).orElse(null);
        if (customerNotes == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Lead notes not found")
                    .userMessage("Sorry, lead notes not found");
        }
        return customerNotes;
    }

    private BnplLead validateLead(UUID leadPublicId) {
        return bnplLeadRepository.findByPublicIdAndDeletedIsFalse(leadPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Lead not found")
                        .userMessage("Lead not found"));
    }

    public Person validateLoggedInUser(String userName) {
        return personRepository.findByEmailAndDeletedIsFalse(userName)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Agent not found")
                        .userMessage("Agent not found"));
    }

    public SalesAgents validateUserWithId(UUID publicId) {
        return salesAgentsRepository.findByPublicIdAndDeletedIsFalse(publicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Agent not found")
                        .userMessage("Agent not found"));
    }
}
