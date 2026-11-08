package com.app.itibari.services.helper;

import com.app.itibari.models.request.InvoiceListRequest;
import com.app.itibari.models.request.TRARequest;
import com.app.itibari.models.response.TRAResponse;
import com.app.itibari.utils.feign.TraIntegrationService;
import com.app.itibari.utils.helpers.WebhookClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TraSubmissionHelper {

    private final TraIntegrationService traIntegrationService;
    private final ObjectMapper objectMapper;

    private final WebhookClient webhookClient;

    public Optional<TRAResponse> submitReceipt(TRARequest request, String salesOrderNumber) {
        if (request.getInvoiceDetails() == null || request.getInvoiceDetails().isEmpty()) {
            log.warn("Skipping TRA request for sales order {} because no invoice items were eligible", salesOrderNumber);
            return Optional.empty();
        }

        InvoiceListRequest invoice = new InvoiceListRequest();
        invoice.setInvoice(Collections.singletonList(request));

        try {
            log.info("TRA Request for : {}", objectMapper.writeValueAsString(invoice));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize TRA request for sales order {}: {}", salesOrderNumber, e.getMessage());
        }

        TRAResponse response;
        try {
            response = traIntegrationService.sendReceiptToTraServer(invoice);
        } catch (Exception e) {
            log.error("Error sending receipt to TRA server for sales order {}: {}", salesOrderNumber, e.getMessage());
            // send this error to a gchat channel
            String message = String.format("Error sending receipt to TRA server for sales order %s: %s", salesOrderNumber, e.getMessage());
            try {
                String payload = objectMapper.writeValueAsString(Collections.singletonMap("text", message));
                webhookClient.acceptAndSend("TRA Errors", payload);
            } catch (JsonProcessingException jsonProcessingException) {
                log.warn("Failed to serialize TRA error webhook payload for sales order {}: {}", salesOrderNumber, jsonProcessingException.getMessage());
            }
            return Optional.empty();
        }

        try {
            log.info("TRA Response: {}", objectMapper.writeValueAsString(response));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize TRA response for sales order {}: {}", salesOrderNumber, e.getMessage());
        }

        if (response == null) {
            log.error("Failed to send receipt to TRA server for sales order {}: empty response", salesOrderNumber);
            return Optional.empty();
        }

        String responseStatus = response.getStatus();
        if (responseStatus == null || !responseStatus.equalsIgnoreCase("success")) {
            log.error("Failed to send receipt to TRA server for sales order {}: status={}, message={}",
                    salesOrderNumber,
                    responseStatus,
                    response.getMessage());
            return Optional.empty();
        }

        return Optional.of(response);
    }
}
