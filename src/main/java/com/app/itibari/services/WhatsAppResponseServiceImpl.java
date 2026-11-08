package com.app.itibari.services;

import com.app.itibari.dataTypes.ReferenceType;
import com.app.itibari.dataTypes.WhatsAppStatus;
import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.WhatsAppResponse;
import com.app.itibari.models.request.WhatsAppOutboundMedia;
import com.app.itibari.models.request.WhatsAppRequest;
import com.app.itibari.models.response.FileResponseDTO;
import com.app.itibari.models.response.WhatsAppResponseWrapper;
import com.app.itibari.repository.CustomersRepository;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.WhatsAppResponseRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.TumaMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.app.itibari.utils.helpers.PhoneNumberHelper.getLastNineDigits;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppResponseServiceImpl implements WhatsAppResponseService {

    private static final DateTimeFormatter WHATSAPP_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> MEDIA_TYPES = Set.of("image", "document", "audio", "video", "sticker");
    private static final long IMAGE_MAX_BYTES = 5L * 1024 * 1024;
    private static final long VIDEO_MAX_BYTES = 16L * 1024 * 1024;
    private static final long AUDIO_MAX_BYTES = 16L * 1024 * 1024;
    private static final long DOCUMENT_MAX_BYTES = 50L * 1024 * 1024;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${whatsAppDetails.callBackAccessToken}")
    private String VERIFY_TOKEN;

    private final WhatsAppResponseRepository whatsAppResponseRepository;
    private final CustomersRepository customersRepository;
    private final TumaMessage tumaMessage;
    private final PersonRepository personRepository;
    private final FileService fileService;

    @Override
    public String verifyChallenge(Map<String, String> params) {
        String mode = params.get("hub.mode");
        String token = params.get("hub.verify_token");
        String challenge = params.get("hub.challenge");

        if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(token)) {
            log.info("---------Webhook verified!----");
            return challenge;
        } else {
            return "Error: Verification failed";
        }
    }

    @Override
    public void runPersistData(Map<String, Object> body) {
        List<Map<String, Object>> entry = (List<Map<String, Object>>) body.get("entry");
        if (entry != null) {
            entry.forEach(e -> {
                List<Map<String, Object>> changes = (List<Map<String, Object>>) e.get("changes");
                if (changes != null) {
                    changes.forEach(change -> {
                        Map<String, Object> value = (Map<String, Object>) change.get("value");

                        // Handle incoming messages like from the User/Customer
                        List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");
                        if (messages != null) {
                            messages.forEach(msg -> {
                                String from = (String) msg.get("from");
                                String id = (String) msg.get("id");
                                String messageType = msg.get("type") != null
                                        ? String.valueOf(msg.get("type")).toLowerCase(Locale.ROOT)
                                        : "text";

                                ParsedInboundMessage parsed = parseInboundMessage(msg, messageType);
                                String customerName = getCustomerDetails(from);

                                saveWhatsAppMessage(
                                        from,
                                        parsed.bodyText(),
                                        "Direct",
                                        customerName,
                                        id,
                                        null,
                                        null,
                                        false,
                                        messageType,
                                        parsed.mediaId(),
                                        parsed.mimeType());
                                log.info("Saved standard message: {} type={} -> {}", from, messageType, parsed.bodyText());
                            });
                        }

                        // Handle history messages
                        List<Map<String, Object>> history = (List<Map<String, Object>>) value.get("history");
                        if (history != null) {
                            history.forEach(h -> {
                                List<Map<String, Object>> threads = (List<Map<String, Object>>) h.get("threads");
                                if (threads != null) {
                                    threads.forEach(thread -> {
                                        List<Map<String, Object>> threadMessages = (List<Map<String, Object>>) thread.get("messages");
                                        if (threadMessages != null) {
                                            threadMessages.forEach(msg -> {
                                                String from = (String) msg.get("from");
                                                String messageType = msg.get("type") != null
                                                        ? String.valueOf(msg.get("type")).toLowerCase(Locale.ROOT)
                                                        : "text";
                                                ParsedInboundMessage parsed = parseInboundMessage(msg, messageType);

                                                String customerName = getCustomerDetails(from);

                                                saveWhatsAppMessage(
                                                        from,
                                                        parsed.bodyText(),
                                                        "History",
                                                        customerName,
                                                        null,
                                                        null,
                                                        null,
                                                        false,
                                                        messageType,
                                                        parsed.mediaId(),
                                                        parsed.mimeType());
                                                log.info("Saved history message: {} type={} -> {}", from, messageType, parsed.bodyText());
                                            });
                                        }
                                    });
                                }
                            });
                        }

                        List<Map<String, Object>> statuses = (List<Map<String, Object>>) value.get("statuses");
                        if (statuses != null) {
                            statuses.forEach(statusObj -> {
                                String id = (String) statusObj.get("id");
                                String status = (String) statusObj.get("status");
                                String recipientId = (String) statusObj.get("recipient_id"); //255672903554

                                updateWhatsAppMessage(recipientId, id, status);
                                if ("failed".equalsIgnoreCase(status)) {
                                    log.error("WhatsApp delivery failed id={} recipient={} payload={}",
                                            id, recipientId, statusObj);
                                } else {
                                    log.info("Saved status update: {} -> {}", id, status);
                                }
                            });
                        }
                    });
                }
            });
        }
    }

    private void updateWhatsAppMessage(String recipientId, String id, String status) {
        WhatsAppResponse whatsAppResponse = whatsAppResponseRepository.findByMessageIdIgnoreCase(id).orElse(null);
        if (whatsAppResponse != null){
            whatsAppResponse.setStatus(WhatsAppStatus.valueOf(status));
            whatsAppResponseRepository.save(whatsAppResponse);
        }
    }

    @Override
    public Page<WhatsAppResponseWrapper> fetchAll(
            String searchParam,
            WhatsAppStatus status,
            LocalDate startDate,
            LocalDate endDate,
            Boolean unread,
            Pageable pageable) {

        if (startDate == null && endDate == null) {
            startDate = LocalDate.now();
            endDate = startDate;
        }

        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        String search = null;
        if (searchParam != null && searchParam.trim().length() >= 3) {
            search = "%" + searchParam.trim().toUpperCase() + "%";
        }

        // Keep query ORDER BY (all unread first, then by latest); ignore client sort to avoid conflict
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        // Same 7-day window as /unread-count so badge threads always appear (and pin to top)
        LocalDateTime unreadSince = LocalDate.now().minusDays(7).atStartOfDay();

        return whatsAppResponseRepository.fetchLatestWithUnread(
                status, search, start, end, unread, unreadSince, unsorted
        );
    }

    private WhatsAppResponseWrapper toWrapper(WhatsAppResponse whatsAppResponse) {
        String storeName = customersRepository.findByPhoneNumberLastNine(getLastNineDigits(whatsAppResponse.getPhoneNumber()))
                .stream()
                .findFirst()
                .map(Customers::getStoreName)
                .orElse(null);

        return WhatsAppResponseWrapper.builder()
                .dateTime(whatsAppResponse.getCreationTime() == null
                        ? null
                        : whatsAppResponse.getCreationTime().format(WHATSAPP_DATE_TIME_FORMATTER))
                .customerName(whatsAppResponse.getCustomerName())
                .phone(whatsAppResponse.getPhoneNumber())
                .storeName(storeName)
                .build();
    }

    @Override
    public void saveMessage(String body, String processType, String phoneNumber, String customerName, String salesOrderNumber) {
        JSONObject json = new JSONObject(body);

        JSONObject messageObj = json.getJSONArray("messages").getJSONObject(0);

        String id = messageObj.getString("id");
        String messageStatus = messageObj.getString("message_status");

        String message = getOriginalMessage(processType, customerName, salesOrderNumber);

        saveWhatsAppMessage(phoneNumber, message, "System", customerName, id, messageStatus, null, false, "text", null, null);
    }

    @Override
    public void saveBroadcastMessage(String body, String phoneNumber, String recipientName, String message) {
        JSONObject messageObj = new JSONObject(body).getJSONArray("messages").getJSONObject(0);

        String id = messageObj.getString("id");
        String messageStatus = messageObj.optString("message_status", null);

        saveWhatsAppMessage(phoneNumber, message, "System", recipientName, id, messageStatus, null, false, "text", null, null);
    }

    @Override
    public Page<WhatsAppResponse> fetchAllByCustomerTel(String phoneNumber, Pageable pageable) {
        String lastNineDigits = getLastNineDigits(phoneNumber);
        Page<WhatsAppResponse> page = whatsAppResponseRepository.findByPhoneNumberContainingIgnoreCase(lastNineDigits, pageable);
        page.forEach(this::attachMediaUrl);
        return page;
    }

    @Override
    public void updateCustomerCallStatus(WhatsAppRequest request, MultipartFile file, String userName) throws IOException, InterruptedException {
        Optional<Person> foundUser = requirePerson(userName);
        WhatsAppOutboundMedia media = requireMessageOrFile(request, file);

        String phoneNumber1 = canonicalWhatsAppPhone(request.customerPhone());
        Optional<WhatsAppResponse> foundMessage = findLatestMessageForPhone(phoneNumber1);

        if (!foundMessage.isPresent()){
            throw new BadRequestRestApiException()
                    .developerMessage("Message to reply to not found")
                    .userMessage("Sorry, message to reply to not found");
        }

        tumaMessage.replyMessage(foundUser, request, foundMessage.get().getMessageId(), media);
    }

    @Override
    public void sendMessage(WhatsAppRequest request, MultipartFile file, String userName) throws IOException, InterruptedException {
        Optional<Person> foundUser = requirePerson(userName);
        if (request.customerPhone() == null || request.customerPhone().isBlank()) {
            throw new BadRequestRestApiException()
                    .developerMessage("customerPhone is required")
                    .userMessage("Please provide the customer phone number");
        }
        rejectEmptyNamedFile(file);
        if (!hasOpenCustomerWindow(request.customerPhone())) {
            if (file != null && !file.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Cannot send session media outside the 24-hour window")
                        .userMessage("WhatsApp cannot send a file to a number that has not messaged us. Send text first; after they reply you can attach a file.");
            }
            if (request.messageBody() == null || request.messageBody().isBlank()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Template body is empty")
                        .userMessage("Please enter a message");
            }
            tumaMessage.sendTemplateMessage(foundUser, request);
            return;
        }
        WhatsAppOutboundMedia media = requireMessageOrFile(request, file);
        tumaMessage.sendMessage(foundUser, request, media);
    }

    @Override
    public void saveReplyMessage(String body, WhatsAppRequest whatsAppRequest, Optional<Person> foundUser, WhatsAppOutboundMedia media) {
        JSONObject json = new JSONObject(body);

        JSONObject messageObj = json.getJSONArray("messages").getJSONObject(0);

        String id = messageObj.getString("id");

        String messageType = media != null ? media.messageType() : "text";
        String mimeType = media != null ? media.mimeType() : null;
        String message = resolveReplyBody(whatsAppRequest.messageBody(), messageType);

        String phoneNumber1 = canonicalWhatsAppPhone(whatsAppRequest.customerPhone());

        WhatsAppResponse saved = saveWhatsAppMessage(phoneNumber1, message, "System", null, id, null,
                foundUser.get().getFullName(), true, messageType, null, mimeType);
        persistOutboundMedia(saved, media);
    }

    @Override
    public void readMessage(String phoneNumber, String email) {
        Person user = personRepository.findByEmailAndDeletedIsFalse(email)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("User not found for email: " + email)
                        .userMessage("Sorry, user not found"));

        String lastNineDigits = getLastNineDigits(phoneNumber);

        List<WhatsAppResponse> responses = whatsAppResponseRepository
                .findByPhoneNumberContainingIgnoreCaseAndSourceTypeIgnoreCase(lastNineDigits, "Direct");

        responses.forEach(response -> {
            response.setCxRead(true);
            response.setUpdatedBy(user);
        });

        whatsAppResponseRepository.saveAll(responses);
    }

    @Override
    public Map<String, Long> getUnreadCount() {
        LocalDateTime since = LocalDate.now().minusDays(7).atStartOfDay();
        return Map.of(
                "unreadCount",
                whatsAppResponseRepository.countByCxReadFalseAndSourceTypeIgnoreCaseAndCreationTimeGreaterThanEqual(
                        "Direct", since)
        );
    }


    private String getOriginalMessage(String processType, String customerName, String salesOrderNumber) {
        if (processType.equalsIgnoreCase("order_planning")) {
            return String.format("Habari %s, Oda yako %s imepokelewa na ipo katika maandalizi kwa ajili ya usafirishaji. Tarajia kupokea oda yako siku ya kesho. Asante kwa kuchagua Itibari", customerName, salesOrderNumber);
        } else if (processType.equalsIgnoreCase("order_transit")) {
            return String.format("Habari %s, Oda yako %s ipo njiani na mhudumu wetu wa usafirishaji atafika dukani kwako leo. Asante kwa kuichagua Itibari.", customerName, salesOrderNumber);
        } else {
            return String.format("Habari %s, Oda yako %s imefikishwa kikamilifu. Tafadhali pakua risiti yako kupitia Itibari Customer App. Asante kwa kuichagua Itibari.", customerName, salesOrderNumber);
        }
    }

    private String getCustomerDetails(String from) {
        String lastNineDigits = getLastNineDigits(from);
        List<Customers> customer = customersRepository.findByPhoneNumberLastNine(lastNineDigits);
        if (!customer.isEmpty()) {
            return customer.get(0).getName();
        }
        return "New Customer";
    }

    private ParsedInboundMessage parseInboundMessage(Map<String, Object> msg, String messageType) {
        if ("text".equals(messageType)) {
            Map<String, Object> text = (Map<String, Object>) msg.get("text");
            String bodyText = text != null ? (String) text.get("body") : null;
            return new ParsedInboundMessage(bodyText, null, null);
        }

        if (MEDIA_TYPES.contains(messageType)) {
            Map<String, Object> media = (Map<String, Object>) msg.get(messageType);
            if (media == null) {
                return new ParsedInboundMessage("[" + capitalize(messageType) + "]", null, null);
            }

            String caption = (String) media.get("caption");
            String mediaId = (String) media.get("id");
            String mimeType = (String) media.get("mime_type");
            String bodyText = (caption != null && !caption.isBlank())
                    ? caption
                    : "[" + capitalize(messageType) + "]";

            return new ParsedInboundMessage(bodyText, mediaId, mimeType);
        }

        return new ParsedInboundMessage("[" + capitalize(messageType) + "]", null, null);
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private void attachMediaUrl(WhatsAppResponse response) {
        if (response.getMessageType() == null || !MEDIA_TYPES.contains(response.getMessageType().toLowerCase(Locale.ROOT))) {
            return;
        }
        try {
            FileResponseDTO media = fileService.getImages(response.getPublicId(), ReferenceType.WHATSAPP_MEDIA);
            if (media != null) {
                response.setMediaUrl(media.getUrl());
                if (response.getMediaMimeType() == null) {
                    response.setMediaMimeType(media.getContentType());
                }
            }
        } catch (Exception e) {
            log.warn("Could not resolve media URL for WhatsApp message {}", response.getPublicId());
        }
    }

    private WhatsAppResponse saveWhatsAppMessage(String from, String bodyText, String sourceType, String customerName, String waMid,
                                     String messageStatus, String responder, boolean isReply, String messageType,
                                     String mediaId, String mimeType) {
        String lastNineDigits = getLastNineDigits(from);
        List<Customers> customer = customersRepository.findByPhoneNumberLastNine(lastNineDigits);

        String storeName = null;
        if (!customer.isEmpty()) {
            storeName = customer.get(0).getStoreName();
        }

        WhatsAppResponse response = new WhatsAppResponse();
        response.setPhoneNumber(canonicalWhatsAppPhone(from));
        response.setMessage(bodyText);
        response.setSourceType(sourceType);
        response.setCustomerName(customerName);
        response.setMessageId(waMid);
        response.setReplyByName(responder);
        response.setCxReply(isReply);
        response.setStoreName(storeName);
        response.setMessageType(messageType != null ? messageType : "text");
        response.setMediaMimeType(mimeType);

        if (messageStatus != null && !messageStatus.isEmpty()) {
            response.setStatus(WhatsAppStatus.valueOf(messageStatus));
        }

        WhatsAppResponse saved = whatsAppResponseRepository.save(response);
        persistInboundMedia(saved, mediaId);
        return saved;
    }

    private void persistInboundMedia(WhatsAppResponse response, String mediaId) {
        if (mediaId == null || mediaId.isBlank()) {
            return;
        }
        try {
            TumaMessage.WhatsAppMedia media = tumaMessage.downloadMedia(mediaId);
            fileService.uploadWhatsAppMedia(media.content(), media.mimeType(), media.fileName(), response.getPublicId());
            if (response.getMediaMimeType() == null) {
                response.setMediaMimeType(media.mimeType());
                whatsAppResponseRepository.save(response);
            }
            log.info("Stored WhatsApp media {} for message {}", mediaId, response.getPublicId());
        } catch (Exception e) {
            log.error("Failed to download/store WhatsApp media {} for message {}", mediaId, response.getPublicId(), e);
        }
    }

    private void persistOutboundMedia(WhatsAppResponse response, WhatsAppOutboundMedia media) {
        if (media == null || media.content() == null || media.content().length == 0) {
            return;
        }
        try {
            String fileName = media.fileName() != null && !media.fileName().isBlank()
                    ? media.fileName()
                    : response.getPublicId() + ".bin";
            fileService.uploadWhatsAppMedia(media.content(), media.mimeType(), fileName, response.getPublicId());
            log.info("Stored outbound WhatsApp media for message {}", response.getPublicId());
        } catch (Exception e) {
            log.error("Failed to store outbound WhatsApp media for message {}", response.getPublicId(), e);
        }
    }

    private Optional<Person> requirePerson(String userName) {
        Optional<Person> foundUser = personRepository.findByEmailAndDeletedIsFalse(userName);
        if (!foundUser.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("User not found")
                    .userMessage("Sorry, user not found");
        }
        return foundUser;
    }

    private void rejectEmptyNamedFile(MultipartFile file) {
        if (file != null && file.isEmpty() && file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()) {
            throw new BadRequestRestApiException()
                    .developerMessage("file part was empty: " + file.getOriginalFilename())
                    .userMessage("The file was empty. Re-select it in Postman and remove any Content-Type: application/json header.");
        }
    }

    private WhatsAppOutboundMedia requireMessageOrFile(WhatsAppRequest request, MultipartFile file) throws IOException {
        if (request.customerPhone() == null || request.customerPhone().isBlank()) {
            throw new BadRequestRestApiException()
                    .developerMessage("customerPhone is required")
                    .userMessage("Please provide the customer phone number");
        }

        rejectEmptyNamedFile(file);

        boolean hasFile = file != null && !file.isEmpty();
        boolean hasMessage = request.messageBody() != null && !request.messageBody().isBlank();
        if (!hasFile && !hasMessage) {
            throw new BadRequestRestApiException()
                    .developerMessage("Message requires messageBody or file")
                    .userMessage("Please enter a message or attach a file");
        }

        return hasFile ? toOutboundMedia(file) : null;
    }

    private boolean hasOpenCustomerWindow(String customerPhone) {
        String lastNine = getLastNineDigits(customerPhone);
        if (lastNine == null || lastNine.isBlank()) {
            return false;
        }
        return whatsAppResponseRepository
                .existsByPhoneNumberContainingIgnoreCaseAndSourceTypeIgnoreCaseAndCreationTimeGreaterThanEqual(
                        lastNine, "Direct", LocalDateTime.now().minusHours(24));
    }

    private Optional<WhatsAppResponse> findLatestMessageForPhone(String canonicalPhone) {
        Optional<WhatsAppResponse> exact = whatsAppResponseRepository
                .findTopByPhoneNumberIgnoreCaseOrderByCreationTimeDesc(canonicalPhone);
        if (exact.isPresent()) {
            return exact;
        }
        String lastNine = getLastNineDigits(canonicalPhone);
        if (lastNine == null || lastNine.isBlank()) {
            return Optional.empty();
        }
        return whatsAppResponseRepository.findTopByPhoneNumberContainingIgnoreCaseOrderByCreationTimeDesc(lastNine);
    }

    private String canonicalWhatsAppPhone(String phone) {
        if (phone == null) {
            return null;
        }
        String trimmed = phone.replaceAll("[\\s-]", "");
        if (trimmed.startsWith("+") && trimmed.length() >= 11) {
            return trimmed;
        }
        if ((trimmed.startsWith("254") || trimmed.startsWith("255")) && trimmed.length() >= 12) {
            return "+" + trimmed;
        }
        return String.format("+255%s", getLastNineDigits(phone));
    }

    private WhatsAppOutboundMedia toOutboundMedia(MultipartFile file) throws IOException {
        String mimeType = resolveMimeType(file);
        String messageType = resolveWhatsAppMessageType(mimeType);
        long maxBytes = maxBytesForType(messageType);
        if (file.getSize() > maxBytes) {
            throw new BadRequestRestApiException()
                    .developerMessage("File exceeds WhatsApp limit for " + messageType + ": " + file.getSize())
                    .userMessage("Sorry, this file is too large to send on WhatsApp");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            fileName = "attachment";
        }

        return new WhatsAppOutboundMedia(messageType, mimeType, file.getBytes(), fileName);
    }

    private String resolveMimeType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank() && !"application/octet-stream".equalsIgnoreCase(contentType)) {
            return contentType;
        }

        String name = file.getOriginalFilename();
        if (name != null) {
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                return "image/jpeg";
            }
            if (lower.endsWith(".png")) {
                return "image/png";
            }
            if (lower.endsWith(".pdf")) {
                return "application/pdf";
            }
            if (lower.endsWith(".mp4")) {
                return "video/mp4";
            }
            if (lower.endsWith(".3gp")) {
                return "video/3gpp";
            }
            if (lower.endsWith(".mp3")) {
                return "audio/mpeg";
            }
            if (lower.endsWith(".ogg")) {
                return "audio/ogg";
            }
            if (lower.endsWith(".aac")) {
                return "audio/aac";
            }
            if (lower.endsWith(".doc")) {
                return "application/msword";
            }
            if (lower.endsWith(".docx")) {
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            }
            if (lower.endsWith(".xls")) {
                return "application/vnd.ms-excel";
            }
            if (lower.endsWith(".xlsx")) {
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            }
        }
        return contentType != null && !contentType.isBlank() ? contentType : "application/octet-stream";
    }

    private String resolveWhatsAppMessageType(String mimeType) {
        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        if (mime.equals("image/jpeg") || mime.equals("image/jpg") || mime.equals("image/png")) {
            return "image";
        }
        if (mime.startsWith("video/")) {
            return "video";
        }
        if (mime.startsWith("audio/")) {
            return "audio";
        }
        return "document";
    }

    private long maxBytesForType(String messageType) {
        return switch (messageType) {
            case "image" -> IMAGE_MAX_BYTES;
            case "video" -> VIDEO_MAX_BYTES;
            case "audio" -> AUDIO_MAX_BYTES;
            default -> DOCUMENT_MAX_BYTES;
        };
    }

    private String resolveReplyBody(String messageBody, String messageType) {
        if (messageBody != null && !messageBody.isBlank()) {
            return messageBody;
        }
        return "[" + capitalize(messageType) + "]";
    }

    private record ParsedInboundMessage(String bodyText, String mediaId, String mimeType) {
    }
}
