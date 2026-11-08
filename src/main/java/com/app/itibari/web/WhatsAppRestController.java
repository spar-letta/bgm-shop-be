package com.app.itibari.web;

import com.app.itibari.dataTypes.WhatsAppStatus;
import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.WhatsAppResponse;
import com.app.itibari.models.request.WhatsAppRequest;
import com.app.itibari.models.response.WhatsAppResponseWrapper;
import com.app.itibari.services.WhatsAppResponseService;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Tag(name = "whatsapp", description = "whatsapp API")
@RestController
@RequestMapping("/itibari-whatsapp")
@RequiredArgsConstructor
public class WhatsAppRestController {

    private final WhatsAppResponseService whatsAppResponseService;

    @GetMapping
    public String verifyWebhook(@RequestParam Map<String, String> params) {
        return whatsAppResponseService.verifyChallenge(params);
    }

    @PostMapping
    public void receiveWebhook(@RequestBody Map<String, Object> body) {
        log.info("-----------Incoming webhook: {}", body);
        whatsAppResponseService.runPersistData(body);
    }

    @PostMapping(value = "/reply", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Reply to a WhatsApp conversation",
            responses = {@ApiResponse(responseCode = "200", description = "OK")
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "Reply", value = Examples.WHATSAPP_REPLY_REQUEST)})))
    public void reply(@RequestBody @Valid WhatsAppRequest request,
                      @Parameter(hidden = true) Principal principal) throws IOException, InterruptedException {
        whatsAppResponseService.updateCustomerCallStatus(request, null, principal.getName());
    }

    @PostMapping(value = "/reply")
    @Operation(summary = "Reply to a WhatsApp conversation with an optional file",
            description = "Send a text reply and/or attach a file. customerPhone and messageBody can be query params, form fields, or multipart. messageBody is used as the caption for image, video, and document attachments.",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public void replyWithFile(@RequestParam("customerPhone") String customerPhone,
                              @RequestParam(value = "messageBody", required = false) String messageBody,
                              @Parameter(description = "Optional file. Multipart form field must be named file.")
                              @RequestParam(value = "file", required = false) MultipartFile file,
                              HttpServletRequest httpRequest,
                              @Parameter(hidden = true) Principal principal) throws IOException, InterruptedException {
        rejectMisnamedFilePart(httpRequest, file);
        WhatsAppRequest request = WhatsAppRequest.builder()
                .customerPhone(customerPhone)
                .messageBody(messageBody != null ? messageBody : "")
                .build();
        whatsAppResponseService.updateCustomerCallStatus(request, file, principal.getName());
    }

    @PostMapping("/messages")
    @Operation(summary = "Send a WhatsApp message",
            description = "Create and send a WhatsApp message. New numbers (no inbound in 24 hours) are sent with the new_message template, filling {{1}} from messageBody. Files can only be attached after the customer replies. Put customerPhone, optional messageBody, and optional file in multipart form-data; the file part must be named file.",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public void send(@RequestParam("customerPhone") String customerPhone,
                     @RequestParam(value = "messageBody", required = false) String messageBody,
                     @Parameter(description = "Optional file. Multipart form field must be named file.")
                     @RequestParam(value = "file", required = false) MultipartFile file,
                     HttpServletRequest httpRequest,
                     @Parameter(hidden = true) Principal principal) throws IOException, InterruptedException {
        rejectMisnamedFilePart(httpRequest, file);
        WhatsAppRequest request = WhatsAppRequest.builder()
                .customerPhone(customerPhone)
                .messageBody(messageBody != null ? messageBody : "")
                .build();
        whatsAppResponseService.sendMessage(request, file, principal.getName());
    }

    @JsonView(BaseView.WhatsAppView.class)
    @GetMapping("/get-all-messages")
    public Page<WhatsAppResponseWrapper> fetchAllMessages(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "status", required = false) WhatsAppStatus status,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "unread", required = false) Boolean unread,
            @PageableDefault(size = 20) Pageable pageable) {
        LocalDate effectiveStart = from != null ? from : startDate;
        LocalDate effectiveEnd = to != null ? to : endDate;
        return whatsAppResponseService.fetchAll(searchParam,
                status,
                effectiveStart,
                effectiveEnd,
                unread,
                pageable);
    }

    @JsonView(BaseView.WhatsAppView.class)
    @GetMapping("/get-all-message-items")
    public Page<WhatsAppResponse> fetchAllMessages(
            @RequestParam(name = "phoneNumber") String phoneNumber,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return whatsAppResponseService.fetchAllByCustomerTel(phoneNumber, pageable);
    }

    @PostMapping("/cx-read-message")
    @Operation(summary = "cx mark read message",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public void readMessage(@RequestParam(name = "phoneNumber") String phoneNumber,
                      @Parameter(hidden = true) Principal principal) throws IOException, InterruptedException {
        whatsAppResponseService.readMessage(phoneNumber, principal.getName());
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get total unread WhatsApp messages",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Map<String, Long> getUnreadCount() {
        return whatsAppResponseService.getUnreadCount();
    }

    private void rejectMisnamedFilePart(HttpServletRequest request, MultipartFile file) {
        if (!(request instanceof MultipartHttpServletRequest multipartRequest)) {
            return;
        }
        boolean hasAnyFile = multipartRequest.getFileMap().values().stream()
                .anyMatch(part -> part != null && !part.isEmpty());
        if (hasAnyFile && (file == null || file.isEmpty())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Multipart included a file but not under part name 'file': "
                            + multipartRequest.getFileMap().keySet())
                    .userMessage("Attach the file as a form field named file");
        }
    }
}
