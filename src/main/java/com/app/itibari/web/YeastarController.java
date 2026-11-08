package com.app.itibari.web;

import com.app.itibari.models.request.YeastarDialRequest;
import com.app.itibari.models.request.YeastarWebhookEvent;
import com.app.itibari.models.response.YeastarApiResponse;
import com.app.itibari.models.response.YeastarCallRecord;
import com.app.itibari.models.response.YeastarExtension;
import com.app.itibari.services.YeastarCallService;
import com.app.itibari.services.YeastarCdrService;
import com.app.itibari.docs.Examples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "Yeastar PBX", description = "Yeastar Linkus telephony integration")
@RestController
@RequestMapping("/yeastar")
@RequiredArgsConstructor
public class YeastarController {

    private final YeastarCallService yeastarCallService;
    private final YeastarCdrService yeastarCdrService;

    @PostMapping("/call/dial")
    @Operation(summary = "Click-to-call", description = "Initiate an outbound call from a PBX extension",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Call initiated", content = @Content(examples = {@ExampleObject(name = "DIAL_RESPONSE", value = Examples.YEASTAR_DIAL_RESPONSE)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "DIAL_REQUEST", value = Examples.YEASTAR_DIAL_REQUEST)
    })))
    public ResponseEntity<YeastarApiResponse> dial(@RequestBody YeastarDialRequest request,
                                                   @Parameter(hidden = true) Principal principal) {
        return ResponseEntity.ok(yeastarCallService.clickToCall(request, principal.getName()));
    }

    @PostMapping("/call/hangup")
    @Operation(summary = "Hangup call", description = "Terminate an active call by channel ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Call terminated", content = @Content(examples = {@ExampleObject(name = "HANGUP_RESPONSE", value = Examples.YEASTAR_HANGUP_RESPONSE)})),
                    @ApiResponse(responseCode = "404", description = "Call not found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "HANGUP_REQUEST", value = Examples.YEASTAR_HANGUP_REQUEST)
    })))
    public ResponseEntity<YeastarApiResponse> hangup(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(yeastarCallService.hangupCall(body.get("channel_id")));
    }

    @PostMapping("/call/{callId}/transfer")
    @Operation(summary = "Transfer call", description = "Transfer an active call to another extension",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Call transferred", content = @Content(examples = {@ExampleObject(name = "TRANSFER_RESPONSE", value = Examples.YEASTAR_TRANSFER_RESPONSE)})),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public ResponseEntity<YeastarApiResponse> transfer(
            @PathVariable("callId") String callId,
            @RequestParam("targetExtension") String targetExtension) {
        return ResponseEntity.ok(yeastarCallService.transferCall(callId, targetExtension));
    }

    @GetMapping("/extensions")
    @Operation(summary = "List extensions", description = "Get all PBX extensions and their current status",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "EXTENSIONS_RESPONSE", value = Examples.YEASTAR_EXTENSIONS_RESPONSE)})),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public ResponseEntity<List<YeastarExtension>> listExtensions() {
        return ResponseEntity.ok(yeastarCallService.listExtensions());
    }

    @GetMapping("/cdr")
    @Operation(summary = "Call history by phone", description = "Get CDR records for a customer phone number",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "CDR_BY_PHONE_RESPONSE", value = Examples.YEASTAR_CDR_BY_PHONE_RESPONSE)})),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public ResponseEntity<List<YeastarCallRecord>> getCdrByPhone(
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(yeastarCdrService.getCallHistoryByPhone(phoneNumber, from, to));
    }

    @GetMapping("/cdr/extension")
    @Operation(summary = "Call history by extension", description = "Get CDR records for a PBX extension",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "CDR_BY_EXTENSION_RESPONSE", value = Examples.YEASTAR_CDR_BY_EXTENSION_RESPONSE)})),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public ResponseEntity<List<YeastarCallRecord>> getCdrByExtension(
            @RequestParam("extension") String extension,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(yeastarCdrService.getCallHistoryByExtension(extension, from, to));
    }

    @PostMapping("/webhook")
   // @SecurityRequirements ?
    @Operation(summary = "PBX webhook", description = "Receives real-time call events from Yeastar PBX",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Event received"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "WEBHOOK_EVENT", value = Examples.YEASTAR_WEBHOOK_EVENT)
    })))
    public ResponseEntity<Void> handleWebhookEvent(@RequestBody YeastarWebhookEvent event) {
        yeastarCallService.handleCallEvent(event);
        return ResponseEntity.ok().build();
    }
}
