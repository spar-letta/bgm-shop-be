package com.app.itibari.utils.helpers;

import com.africastalking.AfricasTalking;
import com.africastalking.SmsService;
import com.africastalking.sms.Recipient;
import com.app.itibari.dataTypes.LoadingStatusEnum;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.TumaSms;
import com.app.itibari.models.request.WhatsAppOutboundMedia;
import com.app.itibari.models.request.WhatsAppRequest;
import com.app.itibari.repository.TumaSmsRepository;
import com.app.itibari.services.WhatsAppResponseService;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.exceptions.rest.RestApiException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@Slf4j
public class TumaMessage {

    @Value("${africastalking.senderId}")
    private String senderId;

    @Value("${whatsAppDetails.phoneNumberId}")
    private String phoneNumberId;

    @Value("${whatsAppDetails.accessToken}")
    private String accessToken;

    // Meta template used for first contact on /messages; body has {{1}}
    @Value("${whatsAppDetails.firstContactTemplate:new_message}")
    private String firstContactTemplate;

    @Value("${whatsAppDetails.firstContactTemplateLanguage:en}")
    private String firstContactTemplateLanguage;

    @Value("${whatsAppDetails.firstContactTemplateParameterName:1}")
    private String firstContactTemplateParameterName;

    // Meta-approved template used for group broadcasts
    @Value("${whatsAppDetails.broadcastTemplate:group_text}")
    private String broadcastTemplate;

    @Value("${whatsAppDetails.broadcastTemplateLanguage:en}")
    private String broadcastTemplateLanguage;

    // Named body variable on group_text (Meta error 100 if this is omitted)
    @Value("${whatsAppDetails.broadcastTemplateParameterName:1}")
    private String broadcastTemplateParameterName;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final String WHATSAPP_URL = "https://graph.facebook.com/v22.0/";

    private final String ORDER_PLANNING = "order_planning";
    private final String ORDER_TRANSIT = "order_transit";
    private final String ORDER_DELIVERED = "order_delivered";
    private final String TEMP_NAME = "customer_support";

    private final PhoneNumberHelper phoneNumberHelper;
    private final WhatsAppResponseService whatsAppResponseService;

    @Autowired
    private TumaSmsRepository tumaSmsRepository;

    public TumaMessage(PhoneNumberHelper phoneNumberHelper, @Lazy WhatsAppResponseService whatsAppResponseService) {
        this.phoneNumberHelper = phoneNumberHelper;
        this.whatsAppResponseService = whatsAppResponseService;
    }

    public List<Recipient> sendSms(String[] recipients, String message) {

        try {
            SmsService sms = AfricasTalking.getService(AfricasTalking.SERVICE_SMS);

            log.info("Attempting to send SMS to: {}, Message: {}, SenderID: {}",
                    Arrays.toString(recipients), message, senderId);

            return sms.send(message, senderId, recipients, true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send SMS: " + e.getMessage(), e);
        }
    }

    public void tumaUjumbeKwaMteja(String phoneNumber, String message, String customerName) {

        try {
            String[] recipients = {phoneNumber};

            List<Recipient> response = sendSms(recipients, message);

            log.info("SMS sent successfully to {}", response);

            for (Recipient recipient : response) {
                log.info("SMS sent to {} with status: {}, messageId {}", recipient.number, recipient.status, recipient.messageId);
                saveCustomer(phoneNumber, customerName, message, recipient.status);
            }
        } catch (Exception e) {
            log.error("Error sending SMS via AfricaStalking", e);
        }
    }

    private void saveCustomer(String phone, String name, String message, String status) {
        TumaSms tumaSms = new TumaSms();
        tumaSms.setTextMessage(message);
        tumaSms.setPhoneNumber(phone);
        tumaSms.setRecipientName(name);
        tumaSms.setIsSuccess(status.equals("Success") ? Boolean.TRUE : Boolean.FALSE);
        tumaSms.setIsCustomer(Boolean.TRUE);
        tumaSmsRepository.save(tumaSms);
    }

    public void whatsAppCustomer(String process, String phoneNumber, String customerName, String salesOrderNumber) throws IOException, InterruptedException {
        String formatted_url = String.format("%s%s/messages", WHATSAPP_URL, phoneNumberId);

        String processType = "";
        if (process.equalsIgnoreCase("planning")){
            processType = ORDER_PLANNING;
        }

        if (process.equalsIgnoreCase("in_transit")){
            processType = ORDER_TRANSIT;
        }

        if (process.equalsIgnoreCase("delivered")){
            processType = ORDER_DELIVERED;
        }

        String payload = getPayload(processType, phoneNumber, customerName, salesOrderNumber);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(formatted_url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        whatsAppResponseService.saveMessage(response.body(), processType, phoneNumber, customerName, salesOrderNumber);
    }

    public void replyMessage(Optional<Person> foundUser, WhatsAppRequest whatsAppRequest, String messageId) throws IOException, InterruptedException {
        replyMessage(foundUser, whatsAppRequest, messageId, null);
    }

    public void replyMessage(Optional<Person> foundUser, WhatsAppRequest whatsAppRequest, String messageId, WhatsAppOutboundMedia media)
            throws IOException, InterruptedException {
        dispatchWhatsAppMessage(foundUser, whatsAppRequest, messageId, media);
    }

    public void sendMessage(Optional<Person> foundUser, WhatsAppRequest whatsAppRequest, WhatsAppOutboundMedia media)
            throws IOException, InterruptedException {
        dispatchWhatsAppMessage(foundUser, whatsAppRequest, null, media);
    }

    public void sendTemplateMessage(Optional<Person> foundUser, WhatsAppRequest whatsAppRequest)
            throws IOException, InterruptedException {
        String formatted_url = String.format("%s%s/messages", WHATSAPP_URL, phoneNumberId);
        String templateText = (whatsAppRequest.messageBody() == null ? "" : whatsAppRequest.messageBody())
                .replaceAll("\\s+", " ")
                .trim();
        if (templateText.isBlank()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Template body is empty")
                    .userMessage("Please enter a message");
        }

        JSONObject payload = buildFirstContactTemplatePayload(whatsAppRequest.customerPhone(), templateText);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(formatted_url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            log.error("WhatsApp template send failed: {} {}", response.statusCode(), response.body());
            throw whatsAppSendFailure(response.statusCode(), response.body());
        }

        WhatsAppRequest persisted = WhatsAppRequest.builder()
                .customerPhone(whatsAppRequest.customerPhone())
                .messageBody(templateText)
                .build();
        whatsAppResponseService.saveReplyMessage(response.body(), persisted, foundUser, null);
    }

    private void dispatchWhatsAppMessage(Optional<Person> foundUser, WhatsAppRequest whatsAppRequest, String messageId,
                                         WhatsAppOutboundMedia media) throws IOException, InterruptedException {
        String formatted_url = String.format("%s%s/messages", WHATSAPP_URL, phoneNumberId);
        String payload;
        if (media == null || media.messageType() == null || "text".equalsIgnoreCase(media.messageType())) {
            payload = getReplyPayload(whatsAppRequest.customerPhone(), messageId, whatsAppRequest.messageBody());
        } else {
            String uploadedMediaId = uploadMedia(media);
            payload = getMediaReplyPayload(whatsAppRequest.customerPhone(), messageId, media, uploadedMediaId, whatsAppRequest.messageBody());
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(formatted_url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            log.error("WhatsApp message failed: {} {}", response.statusCode(), response.body());
            throw whatsAppSendFailure(response.statusCode(), response.body());
        }

        whatsAppResponseService.saveReplyMessage(response.body(), whatsAppRequest, foundUser, media);
    }

    public void sendWhatsAppBroadcast(String phoneNumber, String recipientName, String message) throws IOException, InterruptedException {
        String formatted_url = String.format("%s%s/messages", WHATSAPP_URL, phoneNumberId);

        // template parameters may not contain new lines, tabs or more than four consecutive spaces
        String templateText = message.replaceAll("\\s+", " ").trim();

        JSONObject payload = buildBroadcastTemplatePayload(phoneNumber, templateText);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(formatted_url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() / 100 != 2) {
            throw new IOException("WhatsApp API returned " + response.statusCode() + ": " + response.body());
        }

        whatsAppResponseService.saveBroadcastMessage(response.body(), phoneNumber, recipientName, templateText);
    }

    private String getPayload(String processType, String phoneNumber, String customerName, String salesOrderNumber) {
        String phoneNumber1 = phoneNumberHelper.getPhoneNumber(phoneNumber);
        return String.format("{\n" +
                "  \"messaging_product\": \"whatsapp\",\n" +
                "  \"to\": \"%s\",\n" +
                "  \"type\": \"template\",\n" +
                "  \"template\": {\n" +
                "    \"name\": \"%s\",\n" +
                "    \"language\": { \"code\": \"en\" },\n" +
                "    \"components\": [\n" +
                "      {\n" +
                "        \"type\": \"body\",\n" +
                "        \"parameters\": [\n" +
                "          { \"type\": \"text\", \"text\": \"%s\" },\n" +
                "          { \"type\": \"text\", \"text\": \"%s\" }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +   // Hello {shop}, {body_message}
                "}", phoneNumber1, processType, customerName, salesOrderNumber);
    }

    private String getReplyPayload(String phoneNumber, String messageId, String messageText) {
        return baseReplyPayload(phoneNumber, messageId)
                .put("type", "text")
                .put("text", new JSONObject()
                        .put("preview_url", false)
                        .put("body", messageText != null ? messageText : ""))
                .toString();
    }

    private String getMediaReplyPayload(String phoneNumber, String messageId, WhatsAppOutboundMedia media,
                                        String uploadedMediaId, String caption) {
        String type = media.messageType();
        JSONObject mediaObject = new JSONObject().put("id", uploadedMediaId);
        if (caption != null && !caption.isBlank() && !Set.of("audio", "sticker").contains(type)) {
            mediaObject.put("caption", caption);
        }
        if ("document".equals(type) && media.fileName() != null && !media.fileName().isBlank()) {
            mediaObject.put("filename", sanitizeFileName(media.fileName()));
        }
        return baseReplyPayload(phoneNumber, messageId)
                .put("type", type)
                .put(type, mediaObject)
                .toString();
    }

    private JSONObject buildFirstContactTemplatePayload(String phoneNumber, String templateText) {
        JSONObject parameter = new JSONObject()
                .put("type", "text")
                .put("text", templateText);
        if (firstContactTemplateParameterName != null && !firstContactTemplateParameterName.isBlank()) {
            parameter.put("parameter_name", firstContactTemplateParameterName.trim());
        }

        return new JSONObject()
                .put("messaging_product", "whatsapp")
                .put("to", phoneNumberHelper.getPhoneNumber(phoneNumber))
                .put("type", "template")
                .put("template", new JSONObject()
                        .put("name", firstContactTemplateName())
                        .put("language", new JSONObject().put("code", firstContactTemplateLanguage()))
                        .put("components", new JSONArray().put(new JSONObject()
                                .put("type", "body")
                                .put("parameters", new JSONArray().put(parameter)))));
    }

    private String firstContactTemplateName() {
        return (firstContactTemplate == null || firstContactTemplate.isBlank())
                ? "new_message" : firstContactTemplate.trim();
    }

    private String firstContactTemplateLanguage() {
        if (firstContactTemplateLanguage != null && !firstContactTemplateLanguage.isBlank()) {
            return firstContactTemplateLanguage.trim();
        }
        return (broadcastTemplateLanguage == null || broadcastTemplateLanguage.isBlank())
                ? "en" : broadcastTemplateLanguage.trim();
    }

    private JSONObject buildBroadcastTemplatePayload(String phoneNumber, String templateText) {
        String templateName = (broadcastTemplate == null || broadcastTemplate.isBlank())
                ? "group_text" : broadcastTemplate.trim();
        String language = (broadcastTemplateLanguage == null || broadcastTemplateLanguage.isBlank())
                ? "en" : broadcastTemplateLanguage.trim();

        JSONObject parameter = new JSONObject()
                .put("type", "text")
                .put("text", templateText);
        if (broadcastTemplateParameterName != null && !broadcastTemplateParameterName.isBlank()) {
            parameter.put("parameter_name", broadcastTemplateParameterName.trim());
        }

        return new JSONObject()
                .put("messaging_product", "whatsapp")
                .put("to", phoneNumberHelper.getPhoneNumber(phoneNumber))
                .put("type", "template")
                .put("template", new JSONObject()
                        .put("name", templateName)
                        .put("language", new JSONObject().put("code", language))
                        .put("components", new JSONArray().put(new JSONObject()
                                .put("type", "body")
                                .put("parameters", new JSONArray().put(parameter)))));
    }

    private JSONObject baseReplyPayload(String phoneNumber, String messageId) {
        JSONObject payload = new JSONObject()
                .put("messaging_product", "whatsapp")
                .put("recipient_type", "individual")
                .put("to", phoneNumberHelper.getPhoneNumber(phoneNumber));
        if (messageId != null && !messageId.isBlank()) {
            payload.put("context", new JSONObject().put("message_id", messageId));
        }
        return payload;
    }
    private RestApiException whatsAppSendFailure(int statusCode, String body) {
        String userMessage = "Sorry, we could not send the WhatsApp message. Please try again.";
        try {
            JSONObject error = new JSONObject(body).optJSONObject("error");
            if (error != null && error.optInt("code") == 131047) {
                userMessage = "This customer has not messaged us in the last 24 hours, so WhatsApp blocked the message.";
            } else if (error != null) {
                JSONObject errorData = error.optJSONObject("error_data");
                String details = errorData != null ? errorData.optString("details") : "";
                if (details != null && !details.isBlank()) {
                    userMessage = "WhatsApp rejected the message: " + details;
                }
            }
        } catch (Exception ignored) {
            // keep the generic user message
        }
        return new BadRequestRestApiException()
                .developerMessage("WhatsApp API returned " + statusCode + ": " + body)
                .userMessage(userMessage);
    }

    private String uploadMedia(WhatsAppOutboundMedia media) throws IOException, InterruptedException {
        String formattedUrl = String.format("%s%s/media", WHATSAPP_URL, phoneNumberId);
        String boundary = "----ItibariWhatsApp" + UUID.randomUUID();
        String mimeType = media.mimeType() != null ? media.mimeType() : "application/octet-stream";
        String fileName = sanitizeFileName(media.fileName());
        byte[] multipartBody = buildMediaUploadBody(boundary, media.content(), mimeType, fileName);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(formattedUrl))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            log.error("WhatsApp media upload failed: {} {}", response.statusCode(), response.body());
            throw new BadRequestRestApiException()
                    .developerMessage("WhatsApp media upload failed: " + response.statusCode() + " " + response.body())
                    .userMessage("Sorry, we could not attach the file. Please try again.");
        }

        JSONObject body = new JSONObject(response.body());
        if (!body.has("id")) {
            throw new BadRequestRestApiException()
                    .developerMessage("WhatsApp media upload response missing id: " + response.body())
                    .userMessage("Sorry, we could not attach the file. Please try again.");
        }
        return body.getString("id");
    }

    private byte[] buildMediaUploadBody(String boundary, byte[] fileBytes, String mimeType, String fileName) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            writeFormField(out, boundary, "messaging_product", "whatsapp");
            writeFormField(out, boundary, "type", mimeType);
            writeFilePart(out, boundary, fileBytes, mimeType, fileName);
            out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build WhatsApp media upload body", e);
        }
    }

    private void writeFormField(ByteArrayOutputStream out, String boundary, String name, String value) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(value.getBytes(StandardCharsets.UTF_8));
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private void writeFilePart(ByteArrayOutputStream out, String boundary, byte[] fileBytes, String mimeType, String fileName)
            throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(fileBytes);
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "attachment";
        }
        String name = fileName.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[\\r\\n\"]", "_");
        return name.isBlank() ? "attachment" : name;
    }

    /**
     * Downloads inbound WhatsApp media. Meta media URLs expire quickly (~5 minutes),
     * so callers should persist the bytes immediately.
     */
    public WhatsAppMedia downloadMedia(String mediaId) throws IOException, InterruptedException {
        String metaUrl = WHATSAPP_URL + mediaId;

        HttpRequest metaRequest = HttpRequest.newBuilder()
                .uri(URI.create(metaUrl))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> metaResponse = httpClient.send(metaRequest, HttpResponse.BodyHandlers.ofString());
        if (metaResponse.statusCode() / 100 != 2) {
            throw new IOException("Failed to resolve WhatsApp media " + mediaId
                    + ": " + metaResponse.statusCode() + " " + metaResponse.body());
        }

        JSONObject metaJson = new JSONObject(metaResponse.body());
        String downloadUrl = metaJson.getString("url");
        String mimeType = metaJson.optString("mime_type", "application/octet-stream");

        HttpRequest downloadRequest = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<byte[]> downloadResponse = httpClient.send(downloadRequest, HttpResponse.BodyHandlers.ofByteArray());
        if (downloadResponse.statusCode() / 100 != 2) {
            throw new IOException("Failed to download WhatsApp media " + mediaId
                    + ": HTTP " + downloadResponse.statusCode());
        }

        String extension = mimeType.contains("/") ? mimeType.substring(mimeType.indexOf('/') + 1) : "bin";
        String fileName = mediaId + "." + extension.replace("+", "_");

        return new WhatsAppMedia(downloadResponse.body(), mimeType, fileName);
    }

    public record WhatsAppMedia(byte[] content, String mimeType, String fileName) {
    }
}
