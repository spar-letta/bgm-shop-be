package com.app.itibari.services;

import com.app.itibari.configuration.YeastarConfiguration;
import com.app.itibari.dataTypes.NoteType;
import com.app.itibari.models.entities.ChurnedCallHistory;
import com.app.itibari.models.entities.CustomerNotes;
import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.request.YeastarDialRequest;
import com.app.itibari.models.request.YeastarOutboundCallRequest;
import com.app.itibari.models.request.YeastarWebhookEvent;
import com.app.itibari.models.response.YeastarApiResponse;
import com.app.itibari.models.response.YeastarExtension;
import com.app.itibari.models.response.YeastarExtensionListResponse;
import com.app.itibari.repository.ChurnedCallHistoryRepository;
import com.app.itibari.repository.CustomerNotesRepository;
import com.app.itibari.repository.CustomersRepository;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.utils.feign.YeastarApiClient;
import com.app.itibari.utils.formatter.DateFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class YeastarCallService {

    private final YeastarApiClient yeastarApiClient;
    private final YeastarTokenService yeastarTokenService;
    private final YeastarConfiguration yeastarConfiguration;
    private final PersonRepository personRepository;
    private final CustomersRepository customersRepository;
    private final CustomerNotesRepository customerNotesRepository;
    private final ChurnedCallHistoryRepository churnedCallHistoryRepository;

    public YeastarApiResponse clickToCall(YeastarDialRequest request, String email) {
        String destinationNumber = request.getCallee();

        log.info("Initiating click-to-call: -> {}", request.getCallee());

        String extension = yeastarConfiguration.getDefaultExtension();
        Person person = personRepository.findByEmailAndDeletedIsFalse(email).orElse(null);
        if (person != null && person.getCallerId() != null && !person.getCallerId().isBlank()) {
            extension = person.getCallerId();
        }

       // destinationNumber = getFormattedDestinationNumber(destinationNumber);
        YeastarApiResponse response = yeastarApiClient.dial(
                yeastarTokenService.getValidToken(),
                //"CfusQJhRDbyMnt0MTPQyCA5g27q2o2yR",
                new YeastarOutboundCallRequest(extension,destinationNumber,"yes")
        );
        if (!response.getMessage().equalsIgnoreCase("SUCCESS")) {
            log.warn("Click-to-call failed: {}", response.getMessage());
        }

        log.info("person details: -> {}", person.getPublicId());
        updateCustomerAttendedBy(request, person);
        return response;
    }

    private void updateCustomerAttendedBy(YeastarDialRequest request, Person person) {
        Customers customer = customersRepository.findByPhoneNumberIgnoreCaseAndDeletedIsFalse(request.getCallee()).orElse(null);
        if (customer != null){
            customer.setCallMadeBy(person.getFullName());
            Customers saveCustomer = customersRepository.save(customer);
            if (request.getIsChurned() != null && request.getIsChurned() == Boolean.TRUE){
                createChurnedCustomerCalledRecord(saveCustomer, person, request.getIsChurned(), Boolean.FALSE, Boolean.FALSE);
            }
            if (request.getLocationChange() != null && request.getLocationChange() == Boolean.TRUE){
                createChurnedCustomerCalledRecord(saveCustomer, person, Boolean.FALSE, request.getLocationChange(), Boolean.FALSE);
            }

            if (request.getOrderAmendment() != null && request.getOrderAmendment() == Boolean.TRUE){
                createChurnedCustomerCalledRecord(saveCustomer, person, Boolean.FALSE, request.getLocationChange(), request.getOrderAmendment());
            }
        }
    }

    private void createChurnedCustomerCalledRecord(Customers customer, Person person, Boolean isChurned, Boolean locationChange, Boolean orderAmendment) {
        ChurnedCallHistory churnedCallHistory = new ChurnedCallHistory();
        churnedCallHistory.setCustomerPublicId(customer.getPublicId().toString());
        churnedCallHistory.setCreationTime(LocalDateTime.now());
        churnedCallHistory.setCxAgentPublicId(person.getPublicId());
        churnedCallHistory.setCxAgentName(person.getFullName());
        churnedCallHistory.setCustomerName(customer.getName());
        churnedCallHistory.setCustomerName(customer.getStoreName());
        churnedCallHistory.setDistrict(customer.getOrganizationRegions() != null ? customer.getOrganizationRegions().getName(): null);
        churnedCallHistory.setWard(customer.getOrganizationLocation() != null ? customer.getOrganizationLocation().getName() : null);
        churnedCallHistory.setPhoneNumber(customer.getPhoneNumber());
        churnedCallHistory.setLastOrderDate(customer.getLastOrderDate() != null ? DateFormatter.formatDate(customer.getLastOrderDate()) : null);

        if (isChurned == Boolean.TRUE) {
            churnedCallHistory.setChurned(true);
            List<String> content = new ArrayList<>();
            List<String> contentReasonCode = new ArrayList<>();
            StringBuilder stringBuilder = new StringBuilder();
            for (CustomerNotes customerNotes : customerNotesRepository.findByCustomerAndDeletedIsFalse(customer)) {

                if (customerNotes.getNoteType() == NoteType.CS_NOTES) {
                    String contentNote = String.format("%s-%s (department - %s) -customer note", customerNotes.getReasonCode().getCode(), customerNotes.getReasonCode().getDescription(), customerNotes.getReasonCode().getDepartment().getName());


                    String departmentName = customerNotes.getReasonCode().getDepartment().getName();

                    stringBuilder.append(departmentName).append(",");
                    contentReasonCode.add(contentNote);
                }

                if (customerNotes.getNoteType() == NoteType.CUSTOMER_NOTES) {
                    if (customerNotes.getContent() != null && !customerNotes.getContent().isEmpty()) {
                        String contentNote = String.format("%s-customer note", customerNotes.getContent());
                        content.add(contentNote);
                    }
                }
            }

            String joinedContent = String.join(", ", content);
            String joinedContentReasonCode = String.join(", ", contentReasonCode);

            churnedCallHistory.setReasonCode(joinedContentReasonCode);
            churnedCallHistory.setXcNote(joinedContent);
            churnedCallHistory.setResponsibleDepartment(stringBuilder.toString());
        }

        if (locationChange == Boolean.TRUE){
            churnedCallHistory.setLocationChange(true);
        }

        if (orderAmendment == Boolean.TRUE){
            churnedCallHistory.setOrderAmendment(true);
        }

        churnedCallHistory.setTelephone(true);
        churnedCallHistoryRepository.save(churnedCallHistory);
    }

    public YeastarApiResponse hangupCall(String channelId) {
        log.info("Hanging up call channel: {}", channelId);
        return yeastarApiClient.hangup(
                yeastarTokenService.getValidToken(),
                Map.of("channel_id", channelId)
        );
    }

    public YeastarApiResponse transferCall(String callId, String targetExtension) {
        log.info("Transferring call {} to ext {}", callId, targetExtension);
        return yeastarApiClient.transfer(
                yeastarTokenService.getValidToken(),
                Map.of("call_id", callId, "transfer_to", targetExtension)
        );
    }

    public List<YeastarExtension> listExtensions() {
        YeastarExtensionListResponse response = yeastarApiClient.listExtensions(
                yeastarTokenService.getValidToken()
        );
        if (!response.isSuccess()) {
            log.warn("Failed to list Yeastar extensions");
            return List.of();
        }
        return response.getExtensionList();
    }

    public void handleCallEvent(YeastarWebhookEvent event) {
        String status = event.getStatus();

        if (isFailureStatus(status)) {
            log.warn("Yeastar call FAILED: event={}, callId={}, caller={}, callee={}, status={}, reason={}",
                    event.getEvent(), event.getCallId(), event.getCaller(), event.getCallee(),
                    status, event.getReason());
        } else {
            log.info("Yeastar call event: event={}, callId={}, caller={}, callee={}, status={}",
                    event.getEvent(), event.getCallId(), event.getCaller(), event.getCallee(), status);
        }
    }

    private boolean isFailureStatus(String status) {
        if (status == null) return false;
        return switch (status.toUpperCase()) {
            case "FAILED", "NOANSWER", "NO_ANSWER", "BUSY", "CONGESTION", "CANCEL" -> true;
            default -> false;
        };
    }
}
