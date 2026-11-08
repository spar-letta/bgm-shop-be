package com.app.itibari.services;

import com.app.itibari.dataTypes.SMSStatus;
import com.app.itibari.dataTypes.StatementExportType;
import com.app.itibari.dataTypes.TicketPriority;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.*;
import com.app.itibari.models.response.*;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.text.DocumentException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface HelpDeskService {
    HelpDeskDTOResponse createTicket(@Valid TicketDto ticketDto, Principal principal);

    Page<HelpDeskDTOResponse> getHelpDeskList(String searchParam, String helpDeskStatus, TicketPriority ticketPriority, LocalDate startDate, LocalDate endDate, UUID customerPublicId, UUID existingCustomerPublicId, UUID departmentPublicId, TicketDto.CustomerType customerType, Pageable pageable);

    HelpDeskDTOResponse updateTicket(UUID publicId, @Valid TicketUpdateDto ticketUpdateDto, Principal principal);

    HelpDeskDTOResponse getHelpDesk(UUID publicId, Principal principal);

    HelpDeskCommentResponseDTO.CommentDto createComment(UUID publicId, @Valid TicketCommentDto ticketCommentDto, Principal principal);

    HelpDeskCommentResponseDTO getHelpDeskCommentList(UUID publicId, Principal principal);

    HelpDeskTicketSummary getTicketSummary(UUID customerPublicId, String searchParam, String helpDeskStatus, TicketDto.CustomerType customerType, UUID departmentPublicId, Principal principal);

    HelpDeskDTOResponse actionTicket(UUID ticketPublicId, @Valid TicketActionDto ticketActionDto, Principal principal);

    HelpDeskNotificationResponseDTO getTicketNotifications(Principal principal, Pageable pageable);

    void actionTicketAlert(UUID ticketNotificationPublicId, Principal principal);

    Page<HelpDeskCustomerDTOResponse> getHelpDeskCustomer(String searchParam, LocalDate startDate, LocalDate endDate, Pageable pageable);

    void runTumaSms(@Valid TumaSmsDto tumaSmsDto) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, InterruptedException, JobRestartException;

    Page<TumaSms> getTumaSms(String searchParam, LocalDate startDate, LocalDate endDate, SMSStatus smsStatus, Boolean isCampaign, Pageable pageable);

    Map<String, String> getTumaSmsFields(String searchParam);

    void getSmsGroupTemplate(HttpServletResponse response, String name);

    SmsGroupUploadResponse uploadSmsGroup(MultipartFile file, String groupName, String name);

    Page<SmsGroup> getSmsGroups(String searchParam, LocalDate startDate, LocalDate endDate, Boolean defaultGroup, Pageable pageable);

    CustomerFAQToResponse createFaqRequest(@Valid FaqRequest faqRequest, Principal principal);

    Page<CustomerFAQToResponse> getCustomerFAQ(String searchParam, LocalDate startDate, LocalDate endDate, Pageable pageable);

    SmsGroup getSmsGroup(UUID publicId);

    CustomerFAQToResponse getFAQ(UUID publicId);

    SmsSummaryDto getSmsSummaryDto();

    void updateFaqRequest(UUID customerFaqNotePublicId, @Valid FaqUpdateRequest faqUpdateRequest, Principal principal);

    CustomerFAQNote fetchCustomerFAQNote(UUID publicId, Principal principal);

    Page<SmsDefaultGroupResponse> getSmsDefaultGroups(String searchParam, String defaultGroupName, Pageable pageable);

    Page<SmsGroupEntity> getSmsGroupEntity(UUID publicId, String searchParam, Pageable pageable);

    HelpDeskCustomerDTOResponse fetchHelpDeskCustomer(UUID helpDeskCustomerPublicId, Principal principal);

    void deleteFAQ(UUID publicId, Principal principal);

    void tumaSmsKwaMteja(UUID customerPublicId, @Valid TumaSmsKwaMtejaDto tumaSmsKwaMtejaDto);

    void createFollowUpDate(UUID customerPublicId, LocalDate date, String time, Principal principal);

    Page<FollowUpDto> listFollowUpDate(UUID customerPublicId, Principal principal, Pageable pageable);

    Map<String, String> deleteFollowUpDate(UUID publicId, Principal principal);

    void saveLastContactDate(UUID customerPublicId, Principal principal);

    SlaDetailResponse getSlaDetails(UUID ticketPublicId);

    Set<String> listHelpDeskStatus(String status, Principal principal);

    List<TicketHistoryResponse> getTicketHistoryResponse(UUID ticketPublicId);

    List<TicketViewerResponse> getTicketViewerResponse(UUID ticketPublicId);

    void actionRecordTicketFirstView(UUID ticketPublicId, Principal principal);

    void shareAppLink(String customerPhone, UUID smsTemplatePublicId, Principal principal);

    void runNotifyFollowUpGuy();

    List<PersonMiniData> getPersonMiniData(UUID ticketPublicId, Principal principal);

    void sendSmsToActiveCustomers(Customers customers, String message, Set<String> customVariable, String campaignName);

    void sendWhatsAppToActiveCustomers(Customers customer, String message, Set<String> customVariable);

    ByteArrayOutputStream runExportTickets(String searchParam, String helpDeskStatus, TicketPriority ticketPriority, LocalDate startDate, LocalDate endDate, UUID customerPublicId, UUID existingCustomerPublicId, UUID departmentPublicId, TicketDto.CustomerType customerType, StatementExportType exportType, Pageable pageable, Principal principal) throws DocumentException, IOException;

    void inBoundTumaUjumbe(String customerPhone, TicketDto.CustomerType customerType, UUID smsTemplatePublicId, Principal principal);
}
