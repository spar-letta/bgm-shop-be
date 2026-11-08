package com.app.itibari.web;

import com.app.itibari.dataTypes.CustomerType;
import com.app.itibari.dataTypes.SMSStatus;
import com.app.itibari.dataTypes.SmsChannel;
import com.app.itibari.dataTypes.TicketPriority;
import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.*;
import com.app.itibari.models.response.*;
import com.app.itibari.services.CustomerExperienceService;
import com.app.itibari.services.HelpDeskService;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Tag(name = "HelpTickets", description = "HelpTickets API")
@RestController
@RequestMapping("/helpTickets")
public class HelpDeskRestController {

    private final HelpDeskService helpDeskService;
    private final CustomerExperienceService customerExperienceService;

    public HelpDeskRestController(HelpDeskService helpDeskService, CustomerExperienceService customerExperienceService) {
        this.helpDeskService = helpDeskService;
        this.customerExperienceService = customerExperienceService;
    }

    @PostMapping
//    @PreAuthorize("hasAuthority('CREATE_TICKET')")
    @Operation(summary = "create ticket", description = "create ticket", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "Action CREATE_TICKET_EXISTING_CUSTOMER", value = Examples.CREATE_TICKET_request),
            @ExampleObject(name = "Action CREATE_TICKET_NEW_CUSTOMER", value = Examples.CREATE_TICKET_NEW_CUSTOMER_request),})))
    public HelpDeskDTOResponse createTicket(@Valid @RequestBody TicketDto ticketDto,
                                            @Parameter(hidden = true) Principal principal) {

        return helpDeskService.createTicket(ticketDto, principal);
    }

    @GetMapping
    @Operation(summary = "Get all Tickets", description = "Get all Tickets",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<HelpDeskDTOResponse> getHelpDesks(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                  @RequestParam(name = "status", required = false) String helpDeskStatus,
                                                  @RequestParam(name = "priority", required = false) TicketPriority ticketPriority,
                                                  @RequestParam(name = "customerType", required = false) TicketDto.CustomerType customerType,
                                                  @RequestParam(name = "customerPublicId", required = false) UUID customerPublicId,
                                                  @RequestParam(name = "departmentPublicId", required = false) UUID departmentPublicId,
                                                  @RequestParam(name = "existingCustomerPublicId", required = false) UUID existingCustomerPublicId,
                                                  @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                  @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                  @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return helpDeskService.getHelpDeskList(searchParam, helpDeskStatus, ticketPriority,
                startDate, endDate, customerPublicId, existingCustomerPublicId, departmentPublicId, customerType, pageable);
    }

    @GetMapping("/{publicId}")
    @Operation(summary = "Get Ticket", description = "Get Ticket",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public HelpDeskDTOResponse getHelpDesk(@PathVariable(name = "publicId") UUID publicId, @Parameter(hidden = true) Principal principal) {

        return helpDeskService.getHelpDesk(publicId, principal);
    }

    @PutMapping("/{publicId}")
    @Operation(summary = "update ticket", description = "update ticket", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "Action UPDATE_TICKET", value = Examples.UPDATE_TICKET_request),})))
    public HelpDeskDTOResponse updateTicket(@PathVariable(name = "publicId") UUID publicId,
                                            @Valid @RequestBody TicketUpdateDto ticketUpdateDto,
                                            @Parameter(hidden = true) Principal principal) {

        return helpDeskService.updateTicket(publicId, ticketUpdateDto, principal);
    }

    @PostMapping("/{publicId}/comment")
    @Operation(summary = "create ticket comment", description = "create ticket comment", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "Action CREATE_COMMENT", value = Examples.CREATE_TICKET_COMMENT_request),})))
    public HelpDeskCommentResponseDTO.CommentDto createComment(@PathVariable(name = "publicId") UUID publicId,
                                                               @Valid @RequestBody TicketCommentDto ticketCommentDto,
                                                               @Parameter(hidden = true) Principal principal) {

        return helpDeskService.createComment(publicId, ticketCommentDto, principal);
    }

    @GetMapping("/{publicId}/comments")
    @Operation(summary = "Get all Tickets comments", description = "Get all Tickets comments",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public HelpDeskCommentResponseDTO getHelpDeskCommentList(@PathVariable(name = "publicId") UUID publicId,
                                                             @Parameter(hidden = true) Principal principal) {

        return helpDeskService.getHelpDeskCommentList(publicId, principal);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get Ticket Summary", description = "Get Ticket Summary",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public HelpDeskTicketSummary getTicketSummary(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "status", required = false) String helpDeskStatus,
            @RequestParam(name = "customerType", required = false) TicketDto.CustomerType customerType,
            @RequestParam(name = "departmentPublicId", required = false) UUID departmentPublicId,
            @RequestParam(name = "customerPublicId", required = false) UUID customerPublicId,
            @Parameter(hidden = true) Principal principal) {

        return helpDeskService.getTicketSummary(customerPublicId, searchParam, helpDeskStatus, customerType, departmentPublicId, principal);
    }

    @Deprecated
    @PutMapping("/{ticketPublicId}/actions")
    @Operation(summary = "action on ticket", description = "action on ticket", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "Action ACTION_ASSIGN", value = Examples.ACTION_ASSIGN_request),
            @ExampleObject(name = "Action ACTION_RESOLVE", value = Examples.ACTION_RESOLVE_request),})))
    public HelpDeskDTOResponse actionTicket(@PathVariable UUID ticketPublicId,
                                            @Valid @RequestBody TicketActionDto ticketActionDto,
                                            @Parameter(hidden = true) Principal principal) {

        return helpDeskService.actionTicket(ticketPublicId, ticketActionDto, principal);
    }

    @PutMapping("/{ticketPublicId}/actionRecordTicketViewer")
    @Operation(summary = "action on ticket", description = "action on ticket", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "Action Record Ticket First Viewer", value = Examples.ACTION_RESOLVE_request),})))
    public void actionRecordTicketFirstView(@PathVariable UUID ticketPublicId,
                                            @Parameter(hidden = true) Principal principal) {

        helpDeskService.actionRecordTicketFirstView(ticketPublicId, principal);
    }

    @GetMapping("/ticketNotifications")
    @Operation(summary = "Get my notifications", description = "Get my notifications",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public HelpDeskNotificationResponseDTO getTicketNotifications(@Parameter(hidden = true) Principal principal,
                                                                  @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return helpDeskService.getTicketNotifications(principal, pageable);
    }

    @PutMapping("/ticketNotifications/{ticketNotificationPublicId}/actions")
    @Operation(summary = "action on ticket alert", description = "action on ticket alert", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "Action ACTION_ON_TICKET", value = Examples.CREATE_TICKET_request),})))
    public void actionTicketAlert(@PathVariable(name = "ticketNotificationPublicId") UUID ticketNotificationPublicId,
                                  @Parameter(hidden = true) Principal principal) {

        helpDeskService.actionTicketAlert(ticketNotificationPublicId, principal);
    }

    @GetMapping("/customerWithSummarisedTickets")
    @Operation(summary = "Get all Tickets", description = "Get all Tickets",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<HelpDeskCustomerDTOResponse> listHelpDeskCustomer(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                                  @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                  @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                                  @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return helpDeskService.getHelpDeskCustomer(searchParam, startDate, endDate, pageable);
    }

    @GetMapping("/customerWithSummarisedTickets/{helpDeskCustomerPublicId}")
    @Operation(summary = "Get one help Desk Customer", description = "Get one help Desk Customer",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public HelpDeskCustomerDTOResponse getHelpDeskCustomer(@PathVariable UUID helpDeskCustomerPublicId,
                                                           @Parameter(hidden = true) Principal principal) {

        return helpDeskService.fetchHelpDeskCustomer(helpDeskCustomerPublicId, principal);
    }

    @PostMapping("/customerExperience")
//    @PreAuthorize("hasAuthority('CREATE_TICKET')")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.CREATE_CUSTOMER_EXPERIENCE_response)))
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "CREATE_TICKET_EXISTING_CUSTOMER", required = true, content = @Content(examples = {
            @ExampleObject(value = Examples.CREATE_CUSTOMER_EXPERIENCE_request),
    })))
    public CustomerExperience createCustomerExperience(@Valid @RequestBody CustomerExperienceRequest customerExperienceRequest,
                                                       @Parameter(hidden = true) Principal principal) {

        return customerExperienceService.createCustomerExperience(customerExperienceRequest, principal);
    }

    @PatchMapping("/customerExperience/{publicId}/updateTemplate")
//    @PreAuthorize("hasAuthority('CREATE_TICKET')")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.CREATE_CUSTOMER_EXPERIENCE_response)))
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "CREATE_TICKET_EXISTING_CUSTOMER", required = true, content = @Content(examples = {
            @ExampleObject(value = Examples.CREATE_CUSTOMER_EXPERIENCE_request),})))
    public CustomerExperience updateCustomerExperience(@PathVariable UUID publicId,
                                                       @Valid @RequestBody CustomerExperienceRequest customerExperienceRequest,
                                                       @Parameter(hidden = true) Principal principal) {

        return customerExperienceService.updateCustomerExperience(publicId, customerExperienceRequest, principal);
    }

    @GetMapping("/customerExperience")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "Get all customer experiences", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.GET_ALL_CUSTOMER_EXPERIENCE_response)))})
    public Page<CustomerExperience> getCustomerExperience(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                          @RequestParam(name = "issueTypePublicId", required = false) UUID issueTypePublicId,
                                                          @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                          @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                          @RequestParam(name = "isActive", required = false) Boolean isActive,
                                                          @RequestParam(name = "customerType", required = false) TicketDto.CustomerType customerType,
                                                          @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return customerExperienceService.getCustomerExperience(searchParam, startDate, endDate, issueTypePublicId, isActive, customerType, pageable);
    }

    @GetMapping("/customerExperience/{publicId}")
//    @PreAuthorize("hasAuthority('CREATE_ISSUE_TYPE')")
    @Operation(summary = "get customer experience form", description = "get customer experience form", responses = {
            @ApiResponse(responseCode = "200", description = "OK")})
    public CustomerExperience getCustomerExperience(@PathVariable UUID publicId,
                                                    @Parameter(hidden = true) Principal principal) {

        return customerExperienceService.fetchCustomerExperience(publicId, principal);
    }

    @PatchMapping("/customerExperience/{publicId}")
//    @PreAuthorize("hasAuthority('CREATE_ISSUE_TYPE')")
    @Operation(summary = "action on customer experience", description = "action on customer experience", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "Action ON customer experience", value = ""),})))
    public CustomerExperience actionCustomerExperience(@PathVariable UUID publicId,
                                                       @RequestParam(name = "isActive") Boolean isActive,
                                                       @Parameter(hidden = true) Principal principal) {

        return customerExperienceService.actionOnCustomerExperience(publicId, isActive, principal);
    }

    @DeleteMapping("/customerExperience/{publicId}")
//    @PreAuthorize("hasAuthority('CREATE_ISSUE_TYPE')")
    @Operation(summary = "action delete customer experience", description = "action delete customer experience", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "Action delete customer experience", value = ""),})))
    public void actionDeleteTemplate(@PathVariable UUID publicId,
                                     @Parameter(hidden = true) Principal principal) {

        customerExperienceService.actionDeleteTemplate(publicId, principal);
    }

    @PostMapping("/priorities")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.CREATE_CUSTOMER_EXPERIENCE_response)))
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "CREATE_TICKET_EXISTING_CUSTOMER", required = true, content = @Content(examples = {
            @ExampleObject(value = Examples.CREATE_PRIORITY_SETTING_request),})))
    public PrioritySettingResponse createPrioritySetting(@Valid @RequestBody PrioritySettingRequest prioritySettingRequest,
                                                         @Parameter(hidden = true) Principal principal) {

        return customerExperienceService.createPrioritySetting(prioritySettingRequest, principal);
    }

    @GetMapping("/priorities")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "Get all customer experiences", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.GET_ALL_CUSTOMER_EXPERIENCE_response)))})
    public Page<PrioritySettingResponse> getPrioritySettings(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                             @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                             @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                             @RequestParam(name = "isActive", required = false) Boolean isActive,
                                                             @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return customerExperienceService.getPrioritySettings(searchParam, startDate, endDate, isActive, pageable);
    }

    @GetMapping("/priorities/{publicId}")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "Get priority")})
    public PrioritySettingResponse getPrioritySetting(@PathVariable UUID publicId) {

        return customerExperienceService.fetchPrioritySetting(publicId);
    }

    @PutMapping("/priorities/{publicId}")
    @Operation(requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "UPDATE_EXISTING_PRIORITY", required = true, content = @Content(examples = {
            @ExampleObject(value = Examples.UPDATE_PRIORITY_SETTING_request),})))
    public PrioritySettingResponse updatePrioritySetting(@PathVariable UUID publicId,
                                                         @Valid @RequestBody PrioritySettingRequest prioritySettingRequest,
                                                         @Parameter(hidden = true) Principal principal) {

        return customerExperienceService.updatePrioritySetting(publicId, prioritySettingRequest, principal);
    }

    @PatchMapping("/priorities/{publicId}")
//    @PreAuthorize("hasAuthority('CREATE_ISSUE_TYPE')")
    @Operation(summary = "action on priority", description = "action on priority", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "Action ON Priority", value = ""),})))
    public PrioritySetting actionPrioritySetting(@PathVariable UUID publicId,
                                                 @RequestParam(name = "isActive") Boolean isActive,
                                                 @Parameter(hidden = true) Principal principal) {

        return customerExperienceService.actionOnPrioritySetting(publicId, isActive, principal);
    }

    @DeleteMapping("/priorities/{publicId}")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.CREATE_CUSTOMER_EXPERIENCE_response)))})
    public void deletePrioritySetting(@PathVariable UUID publicId,
                                      @Parameter(hidden = true) Principal principal) {

        customerExperienceService.deletePrioritySetting(publicId, principal);
    }

    @JsonView(BaseView.TumaSmsView.class)
    @PostMapping("/tuma-sms")
    @Operation(summary = "action on ticket", description = "tuma sms", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE FOR NON GROUP", value = Examples.CREATE_TUMA_SMS_TO_CUSTOMERS_request),
            @ExampleObject(name = "CREATE FOR GROUP", value = Examples.CREATE_TUMA_SMS_FOR_GROUP_request),})))
    public void createTumaSms(@Valid @RequestBody TumaSmsDto tumaSmsDto) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, InterruptedException, JobRestartException {

        helpDeskService.runTumaSms(tumaSmsDto);
    }

    @GetMapping("/tuma-sms")
    @JsonView(BaseView.TumaSmsView.class)
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "Get all sms", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.GET_ALL_CUSTOMER_EXPERIENCE_response)))})
    public Page<TumaSms> getTumaSms(@RequestParam(name = "searchParam", required = false) String searchParam,
                                    @RequestParam(name = "smsStatus", required = false) SMSStatus smsStatus,
                                    @RequestParam(name = "isCampaign", required = false) Boolean isCampaign,
                                    @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                    @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                    @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return helpDeskService.getTumaSms(searchParam, startDate, endDate, smsStatus, isCampaign, pageable);
    }

    @GetMapping("/tuma-sms/fields")
    @JsonView(BaseView.TumaSmsView.class)
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "Get fields", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.GET_ALL_CUSTOMER_EXPERIENCE_response)))})
    public Map<String, String> getTumaSmsFields(@RequestParam(name = "searchParam", required = false) String searchParam) {

        return helpDeskService.getTumaSmsFields(searchParam);
    }

    @GetMapping("/tuma-sms/summary")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "Get sms summary", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.GET_ALL_CUSTOMER_EXPERIENCE_response)))})
    public SmsSummaryDto getSmsSummaryDto() {

        return helpDeskService.getSmsSummaryDto();
    }

    @GetMapping("/smsGroupTemplate")
    public void getPriceManagementTemplate(HttpServletResponse response,
                                           @Parameter(hidden = true) Principal principal) {

        helpDeskService.getSmsGroupTemplate(response, principal.getName());
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload Sms Group User Details", description = "Upload Sms Group User Details",
            responses = {@ApiResponse(responseCode = "200", description = "OK"),})
    public SmsGroupUploadResponse uploadSmsGroup(@RequestParam("file") MultipartFile file,
                                                 @RequestParam(name = "group_name") String groupName,
                                                 @Parameter(hidden = true) Principal principal) {

        if (file.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("File is empty")
                    .userMessage("Please upload a file");
        }

        if (StringUtils.isBlank(groupName)) {
            throw new BadRequestRestApiException()
                    .developerMessage("group_name is blank")
                    .userMessage("Please provide a group name");
        }

        if (StringUtils.isEmpty(file.getOriginalFilename())) {
            throw new BadRequestRestApiException()
                    .developerMessage("File name is empty")
                    .userMessage("Please upload a file");
        }

        String fileName = file.getOriginalFilename().toLowerCase();
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
            throw new BadRequestRestApiException()
                    .developerMessage("File is not an Excel file")
                    .userMessage("Please upload an Excel file");
        }

        return helpDeskService.uploadSmsGroup(file, groupName, principal.getName());
    }

    @GetMapping("/tuma-sms/groups")
    @JsonView(BaseView.TumaSmsView.class)
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "Get all sms groups", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.GET_ALL_CUSTOMER_EXPERIENCE_response)))})
    public Page<SmsGroup> getSmsGroups(@RequestParam(name = "searchParam", required = false) String searchParam,
                                       @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                       @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                       @RequestParam(name = "defaultGroup", required = false) Boolean defaultGroup,
                                       @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return helpDeskService.getSmsGroups(searchParam, startDate, endDate, defaultGroup, pageable);
    }

    @GetMapping("/tuma-sms/groups/{publicId}")
    @JsonView(BaseView.TumaSmsView.class)
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "Get sms groups", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.GET_ALL_CUSTOMER_EXPERIENCE_response)))})
    public SmsGroup getSmsGroup(@PathVariable UUID publicId) {

        return helpDeskService.getSmsGroup(publicId);
    }

    @PostMapping("/faqs")
    @Operation(summary = "create faq", description = "create faq", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE FAQ", value = Examples.CREATE_FAQS_request),})))
    public CustomerFAQToResponse createFaq(@Valid @RequestBody FaqRequest faqRequest, @Parameter(hidden = true) Principal principal) {

        return helpDeskService.createFaqRequest(faqRequest, principal);
    }

    @PutMapping("/faqs/{customerFaqNotePublicId}")
    @Operation(summary = "update faq", description = "update faq", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE FAQ", value = Examples.UPDATE_FAQ_NOTE),})))
    public void updateFaq(@PathVariable UUID customerFaqNotePublicId,
                          @Valid @RequestBody FaqUpdateRequest faqUpdateRequest,
                          @Parameter(hidden = true) Principal principal) {

        helpDeskService.updateFaqRequest(customerFaqNotePublicId, faqUpdateRequest, principal);
    }

    @GetMapping("/faqs/notesItem/{customerFaqNotePublicId}")
//    @PreAuthorize("hasAuthority('CREATE_ISSUE_TYPE')")
    @Operation(summary = "get customer faq note", description = "get customer faq note", responses = {
            @ApiResponse(responseCode = "200", description = "OK")})
    public CustomerFAQNote getCustomerFAQNote(@PathVariable UUID customerFaqNotePublicId,
                                              @Parameter(hidden = true) Principal principal) {

        return helpDeskService.fetchCustomerFAQNote(customerFaqNotePublicId, principal);
    }

    @GetMapping("/faqs")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "Get faqs", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.GET_ALL_CUSTOMER_EXPERIENCE_response)))})
    public Page<CustomerFAQToResponse> getCustomerFAQs(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                       @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                       @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                       @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return helpDeskService.getCustomerFAQ(searchParam, startDate, endDate, pageable);
    }

    @GetMapping("/faqs/{publicId}")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "Get faq", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.GET_ALL_CUSTOMER_EXPERIENCE_response)))})
    public CustomerFAQToResponse getCustomerFAQ(@PathVariable UUID publicId) {

        return helpDeskService.getFAQ(publicId);
    }

    @DeleteMapping("/faqs/{publicId}")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "Delete faq", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.GET_ALL_CUSTOMER_EXPERIENCE_response)))})
    public void deleteCustomerFAQ(@PathVariable UUID publicId, @Parameter(hidden = true) Principal principal) {

        helpDeskService.deleteFAQ(publicId, principal);
    }

    @GetMapping("/tuma-sms/defaultGroups")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "Get all sms groups", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.GET_ALL_CUSTOMER_EXPERIENCE_response)))})
    public Page<SmsDefaultGroupResponse> getSmsDefaultGroups(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                             @RequestParam(name = "defaultGroupName") String defaultGroupName,
                                                             @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return helpDeskService.getSmsDefaultGroups(searchParam, defaultGroupName, pageable);
    }

    @GetMapping("/tuma-sms/groups/{publicId}/noneDefaultGroup")
    @JsonView(BaseView.TumaSmsView.class)
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "Get group lists", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.GET_ALL_CUSTOMER_EXPERIENCE_response)))})
    public Page<SmsGroupEntity> getSmsGroupEntity(@PathVariable UUID publicId,
                                                  @RequestParam(name = "searchParam", required = false) String searchParam,
                                                  @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return helpDeskService.getSmsGroupEntity(publicId, searchParam, pageable);
    }

    @PostMapping("/customerNotes/{customerPublicId}")
//    @PreAuthorize("hasAuthority('CREATE_CUSTOMER_NOTES')")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.CREATE_CUSTOMER_EXPERIENCE_response)))
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "CREATE_CUSTOMER_NOTES", required = true, content = @Content(examples = {
            @ExampleObject(value = Examples.ADD_UPDATE_CUSTOMER_NOTE_REQUEST),})))
    public CustomerNotesDTO addCustomerNotes(@PathVariable UUID customerPublicId,
                                             @Valid @RequestBody CustomerNotesRequest customerNotesRequest,
                                             @Parameter(hidden = true) Principal principal) {

        return customerExperienceService.addCustomerNotes(customerPublicId, customerNotesRequest, principal);
    }

    @PutMapping("/customerNotes/{publicId}")
//    @PreAuthorize("hasAuthority('UPDATE_CUSTOMER_NOTES')")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.CREATE_CUSTOMER_EXPERIENCE_response)))
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "UPDATE_CUSTOMER_NOTES", required = true, content = @Content(examples = {
            @ExampleObject(value = Examples.ADD_UPDATE_CUSTOMER_NOTE_REQUEST),})))
    public CustomerNotesDTO updateCustomerNotes(@PathVariable UUID publicId,
                                                @Valid @RequestBody CustomerNotesRequest customerNotesRequest,
                                                @Parameter(hidden = true) Principal principal) {

        return customerExperienceService.updateCustomerNotes(publicId, customerNotesRequest, principal);
    }

    @GetMapping("/customerNotes/{publicId}")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "get customerNotes", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.GET_ALL_CUSTOMER_EXPERIENCE_response)))})
    public CustomerNotesDTO getCustomerNotesDTO(@PathVariable UUID publicId,
                                                @Parameter(hidden = true) Principal principal) {

        return customerExperienceService.getCustomerNotesDTO(publicId, principal);
    }

    @GetMapping("/customerNotes/{customerPublicId}/listings")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "Get customer notes", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.GET_ALL_CUSTOMER_EXPERIENCE_response)))})
    public Page<CustomerNotesDTO> listCustomerNotes(@PathVariable UUID customerPublicId,
                                                    @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return customerExperienceService.listCustomerNotes(customerPublicId, pageable);
    }

    @DeleteMapping("/customerNotes/{publicId}")
//    @PreAuthorize("hasAuthority('DELETE_CUSTOMER_NOTES')")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.CREATE_CUSTOMER_EXPERIENCE_response)))})
    public Map<String, String> deleteCustomerNotes(@PathVariable UUID publicId,
                                                   @Parameter(hidden = true) Principal principal) {

        return customerExperienceService.deleteCustomerNotes(publicId, principal);
    }

    @PostMapping("/tuma-sms-kwa-kastoma/{customerPublicId}")
    @Operation(summary = "action tuma sms kwa mteja", description = "action tuma sms kwa mteja", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "TUMA_SMS_KWA_MTEJA", value = Examples.TUMA_SMS_KWA_KASTOMA),})))
    public void createTumaSms(@PathVariable UUID customerPublicId,
                              @Valid @RequestBody TumaSmsKwaMtejaDto tumaSmsKwaMtejaDto) {

        helpDeskService.tumaSmsKwaMteja(customerPublicId, tumaSmsKwaMtejaDto);
    }

    @PostMapping("/followUpDate/{customerPublicId}")
//    @PreAuthorize("hasAuthority('CREATE_TICKET')")
    @Operation(summary = "create follow up date", description = "create follow up date", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "Action CREATE_FOLLOW_UP_DATE", value = ""),})))
    public void createFollowUpDate(@RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                   @RequestParam(name = "time") String time,
                                   @PathVariable UUID customerPublicId,
                                   @Parameter(hidden = true) Principal principal) {

        helpDeskService.createFollowUpDate(customerPublicId, date, time, principal);
    }

    @GetMapping("/followUpDate/{customerPublicId}")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "Get customer notes", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.GET_ALL_CUSTOMER_EXPERIENCE_response)))})
    public Page<FollowUpDto> listFollowUpDate(@PathVariable UUID customerPublicId,
                                              @Parameter(hidden = true) Principal principal,
                                              @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return helpDeskService.listFollowUpDate(customerPublicId, principal, pageable);
    }

    @DeleteMapping("/followUpDate/{publicId}/delete")
//    @PreAuthorize("hasAuthority('DELETE_CUSTOMER_NOTES')")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.CREATE_CUSTOMER_EXPERIENCE_response)))})
    public Map<String, String> deleteFollowUpDate(@PathVariable UUID publicId,
                                                  @Parameter(hidden = true) Principal principal) {

        return helpDeskService.deleteFollowUpDate(publicId, principal);
    }

    @PostMapping("/{customerPublicId}/lastContactDate")
    public void saveLastContactDate(@PathVariable UUID customerPublicId,
                                    @Parameter(hidden = true) Principal principal) {

        helpDeskService.saveLastContactDate(customerPublicId, principal);
    }

    @GetMapping("/slaDetails/{ticketPublicId}")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "Get sla details", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.GET_ALL_CUSTOMER_EXPERIENCE_response)))})
    public SlaDetailResponse getSlaDetails(@PathVariable UUID ticketPublicId) {

        return helpDeskService.getSlaDetails(ticketPublicId);
    }

    @GetMapping("/ticketStatuses")
    @Operation(summary = "Get Ticket Status", description = "Get Ticket Status",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Set<String> listHelpDeskStatus(@RequestParam(name = "status", required = false) String status,
                                          @Parameter(hidden = true) Principal principal) {

        return helpDeskService.listHelpDeskStatus(status, principal);
    }

    @GetMapping("/{ticketPublicId}/history")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "Get ticket history", content = @Content(mediaType = "application/json", schema = @Schema(example = "")))})
    public List<TicketHistoryResponse> getTicketHistoryResponse(@PathVariable UUID ticketPublicId) {

        return helpDeskService.getTicketHistoryResponse(ticketPublicId);
    }

    @GetMapping("/{ticketPublicId}/firstViewers")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "Get ticket first time viewers", content = @Content(mediaType = "application/json", schema = @Schema(example = "")))})
    public List<TicketViewerResponse> getTicketViewerResponse(@PathVariable UUID ticketPublicId) {

        return helpDeskService.getTicketViewerResponse(ticketPublicId);
    }

    @PostMapping("/share-app-link")
//    @PreAuthorize("hasAuthority('SEND_APP_LINK')")
    @Operation(summary = "share app links", description = "share app links", responses = {
            @ApiResponse(responseCode = "200", description = "OK")})
    public void shareAppLink(@RequestParam(name = "customerPhone") String customerPhone,
                             @RequestParam(name = "smsTemplatePublicId") UUID smsTemplatePublicId,
                             @Parameter(hidden = true) Principal principal) {

        helpDeskService.shareAppLink(customerPhone, smsTemplatePublicId, principal);
    }

    @PostMapping("/in-bound-send-sms")
//    @PreAuthorize("hasAuthority('SEND_APP_LINK')")
    @Operation(summary = "tuma sms", description = "share app links", responses = {
            @ApiResponse(responseCode = "200", description = "OK")})
    public void tumaUjumbe(@RequestParam(name = "customerPhone") String customerPhone,
                        @RequestParam(name = "customerType") TicketDto.CustomerType customerType,
                        @RequestParam(name = "smsTemplatePublicId") UUID smsTemplatePublicId,
                        @Parameter(hidden = true) Principal principal) {

        helpDeskService.inBoundTumaUjumbe(customerPhone, customerType, smsTemplatePublicId, principal);
    }

    @PostMapping("/followUpTrial")
    public void shareAppLink() {

        helpDeskService.runNotifyFollowUpGuy();
    }

    @GetMapping("/{ticketPublicId}/department-users")
    @Operation(summary = "Get department users", description = "Get department users",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public List<PersonMiniData> getPersonMiniData(@PathVariable UUID ticketPublicId,
                                                  @Parameter(hidden = true) Principal principal) {

        return helpDeskService.getPersonMiniData(ticketPublicId, principal);
    }

    @PostMapping("/sms-template")
//    @PreAuthorize("hasAuthority('CREATE_SMS_TEMPLATE')")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.CREATE_SMS_TEMPLATE_response)))
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "CREATE_SMS_TEMPLATE", required = true, content = @Content(examples = {
            @ExampleObject(value = Examples.CREATE_SMS_TEMPLATE_request),})))
    public SmsTemplateResponse createSmsTemplateRequest(@Valid @RequestBody SmsTemplateRequest smsTemplateRequest,
                                                        @Parameter(hidden = true) Principal principal) {

        return customerExperienceService.createSmsTemplateRequest(smsTemplateRequest, principal);
    }

    @GetMapping("/sms-template")
    @Operation(summary = "Get sms-templates", description = "Get sms-templates",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<SmsTemplateResponse> getSmsTemplates(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                     @RequestParam(name = "status", required = false) Boolean status,
                                                     @RequestParam(name = "channel", required = false) SmsChannel channel,
                                                     @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable,
                                                     @Parameter(hidden = true) Principal principal) {

        return customerExperienceService.getSmsTemplates(searchParam, status, channel, pageable, principal);
    }

    @PutMapping("/sms-template/{publicId}")
//    @PreAuthorize("hasAuthority('UPDATE_SMS_TEMPLATE')")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.CREATE_SMS_TEMPLATE_response)))
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "UPDATE_SMS_TEMPLATE", required = true, content = @Content(examples = {
            @ExampleObject(value = Examples.CREATE_SMS_TEMPLATE_request),})))
    public SmsTemplateResponse updateSmsTemplateRequest(@PathVariable UUID publicId,
                                                        @Valid @RequestBody SmsTemplateRequest smsTemplateRequest,
                                                        @Parameter(hidden = true) Principal principal) {

        return customerExperienceService.updateSmsTemplateRequest(publicId, smsTemplateRequest, principal);
    }

    @DeleteMapping("/sms-template/{publicId}")
//    @PreAuthorize("hasAuthority('DELETE_SMS_TEMPLATE')")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.CREATE_CUSTOMER_EXPERIENCE_response)))
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "CREATE_TICKET_EXISTING_CUSTOMER", required = true, content = @Content(examples = {
            @ExampleObject(value = Examples.CREATE_CUSTOMER_EXPERIENCE_request),})))
    public void deleteSmsTemplateRequest(@PathVariable UUID publicId,
                                         @Parameter(hidden = true) Principal principal) {

        customerExperienceService.deleteSmsTemplateRequest(publicId, principal);
    }

    @PatchMapping("/sms-template/{publicId}")
//    @PreAuthorize("hasAuthority('ACTION_SMS_TEMPLATE')")
    public void actionSmsTemplateRequest(@PathVariable UUID publicId,
                                         @Parameter(hidden = true) Principal principal) {

        customerExperienceService.actionSmsTemplateRequest(publicId, principal);
    }
}
