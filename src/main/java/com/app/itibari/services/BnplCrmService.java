package com.app.itibari.services;

import com.app.itibari.dataTypes.BnplFunnelStage;
import com.app.itibari.dataTypes.BnplLeadStatus;
import com.app.itibari.models.request.*;
import com.app.itibari.models.response.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BnplCrmService {

    BnplLeadPageable getLeads(String search, BnplFunnelStage funnelStage, BnplLeadStatus leadStatus,UUID callStatus, UUID locationId, UUID wardId, Boolean vipOverOneMillion,Pageable pageable);

    BnplLeadPageable getQualifiedLeads(String search, BnplFunnelStage funnelStage, BnplLeadStatus leadStatus, UUID callStatus, UUID locationId, UUID wardId, Boolean vipOverOneMillion, Pageable pageable);

    void createLeads();

    void cleanupStaleLeads();

    BnplLeadResponse getLead(UUID leadPublicId);

    void updateLeadStatus(UUID leadPublicId, BnplLeadStatusRequest request);

    void sendAppLink(UUID leadPublicId);

    void flagCallback(UUID leadPublicId, BnplCallbackFlagRequest request);

    BnplCallAttemptResponse initiateCall(UUID leadPublicId, String agentEmail);

    void tagCallOutcome(UUID callPublicId, BnplCallOutcomeRequest request);

    Page<BnplCallAttemptResponse> getCallHistory(UUID leadPublicId, Pageable pageable);

    BnplFieldVisitResponse logFieldVisit(UUID leadPublicId, BnplFieldVisitRequest request, String agentEmail);

    Page<BnplFieldVisitResponse> getVisitHistory(UUID leadPublicId, Pageable pageable);

    List<BnplReasonCodeResponse> getReasonCodes();

    BnplReasonCodeResponse createReasonCode(BnplReasonCodeRequest request);

    void updateReasonCode(UUID codePublicId, BnplReasonCodeRequest request);

    BnplLeadNotesResponse addLeadCustomerNotes(UUID leadPublicId, @Valid CustomerNotesRequest customerNotesRequest, Principal principal);

    Page<BnplLeadNotesResponse> listLeadNotes(UUID leadPublicId, Pageable pageable);

    BnplLeadNotesResponse updateLeadNotes(UUID notePublicId, @Valid CustomerNotesRequest customerNotesRequest, Principal principal);

    void doAssignSalesAgent(UUID leadPublicId, UUID assignedSalesAgentPublicId, Principal principal);

    Page<AssignedBnplCustomerResponse> listAssignedCustomers(Principal principal, Pageable pageable);
}
