package com.app.itibari.services;

import com.africastalking.AfricasTalking;
import com.africastalking.SmsService;
import com.africastalking.sms.Recipient;
import com.app.itibari.models.request.BeemRequest;
import com.app.itibari.models.request.Receipient;
import com.app.itibari.models.request.SmsRequest;
import com.app.itibari.models.response.BeemResponse;
import com.app.itibari.models.response.SmsResponse;
import com.app.itibari.utils.feign.AfricanEastSmsService;
import com.app.itibari.utils.feign.AuthEncoder;
import com.app.itibari.utils.feign.BeemSmsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class ItibariSmsServiceImpl implements ItibariSmsService {

    @Value("${africastalking.senderId}")
    private String senderId;

    @Value("${beem.api.key}")
    private String beemApiKey;

    @Value("${beem.api.secret}")
    private String beemApiSecret;

    @Value("${beem.sender.id}")
    private String beemSenderId;

    @Value("${beem.choice}")
    private boolean beemChoice;

    private final AfricanEastSmsService africanEastSmsService;

    private final ObjectMapper objectMapper;

    private final BeemSmsService beemSmsService;

    public ItibariSmsServiceImpl(AfricanEastSmsService africanEastSmsService, ObjectMapper objectMapper, BeemSmsService beemSmsService) {

        this.africanEastSmsService = africanEastSmsService;
        this.objectMapper = objectMapper;
        this.beemSmsService = beemSmsService;
    }

    @Override
    public void send(String phoneNumber, String message) {

        SmsRequest smsRequest = new SmsRequest();
        smsRequest.setFrom("Itibari");
        smsRequest.setTo("+255762385818");
        smsRequest.setText(message);
        smsRequest.setReference("ITB");

        // this.sendTextMessage(smsRequest);

        log.info("===========Sending SMS to {} with message============= \n {}", phoneNumber, message);

        if (beemChoice) {
            this.sendSmsViaBeem(phoneNumber, message);
        } else {
            this.sendSmsViaAfricastalking(phoneNumber, message);
        }


    }

    /**
     * Sends an SMS message using the African East SMS service.
     *
     * @param smsRequest The SMS request object containing the message details.
     */
    private void sendTextMessage(SmsRequest smsRequest) {

        String authorization = AuthEncoder.encode("chizeful", "chabichize");

        try {

            SmsResponse smsResponse = africanEastSmsService.sendSMS(authorization, smsRequest);
            log.info("SMS sent successfully to {}", smsResponse.getMessages().get(0).getTo());
            log.info("SMS response: {}", objectMapper.writeValueAsString(smsResponse));
        } catch (Exception e) {
            log.error("Error sending sms", e);
        }
    }

    /**
     * Sends an SMS message using the Africastalking API.
     *
     * @param recipients The phone numbers of the recipients.
     * @param message    The message to be sent.
     * @return A list of recipients with their status.
     */
    private List<Recipient> sendSms(String[] recipients, String message) {

        try {
            SmsService sms = AfricasTalking.getService(AfricasTalking.SERVICE_SMS);

            // Log the actual request being sent
            log.info("Attempting to send SMS to: {}, Message: {}, SenderID: {}",
                    Arrays.toString(recipients), message, senderId);

            // The 'true' parameter enables verbose logging from the SDK
            List<Recipient> response = sms.send(message, senderId, recipients, true);

            if (response.isEmpty()) {
                log.error("Empty response from Africa's Talking API");
            } else {
                response.forEach(r -> log.info("Message sent to {}: status {}",
                        r.number, r.status));
            }
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to send SMS: " + e.getMessage(), e);
        }
    }

    private void sendSmsViaAfricastalking(String phoneNumber, String message) {

        try {
            String[] recipients = {phoneNumber};

            List<Recipient> response = sendSms(recipients, message);

            log.info("SMS sent successfully to {}", response);

            for (Recipient recipient : response) {
                log.info("SMS sent to {} with status: {}", recipient.number, recipient.status);
            }
        } catch (Exception e) {
            log.error("Error sending SMS via Africastalking", e);
        }
    }

    private void sendSmsViaBeem(String phoneNumber, String message) {

        try {
            String auth = AuthEncoder.encode(beemApiKey, beemApiSecret);

            // dest_addr must not include a leading + per Beem API spec

            BeemRequest beemRequest = new BeemRequest();
            beemRequest.setSourceAddr(beemSenderId);
            beemRequest.setEncoding(0);
            beemRequest.setMessage(message);
            beemRequest.setRecipients(Collections.singletonList(new Receipient(1, phoneNumber)));

            BeemResponse response = beemSmsService.sendBeemText(auth, beemRequest);
            log.info("SMS sent via Beem to {}: code={}, message={}", phoneNumber, response.getCode(), response.getMessage());
        } catch (Exception e) {
            log.error("Error sending SMS via Beem", e);
        }
    }
}
