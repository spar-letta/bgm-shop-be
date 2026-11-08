package com.app.itibari.services;

import com.app.itibari.dataTypes.WhatsAppStatus;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.WhatsAppResponse;
import com.app.itibari.models.request.WhatsAppOutboundMedia;
import com.app.itibari.models.request.WhatsAppRequest;
import com.app.itibari.models.response.WhatsAppResponseWrapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

public interface WhatsAppResponseService {
    String verifyChallenge(Map<String, String> params);

    void runPersistData(Map<String, Object> body);

    Page<WhatsAppResponseWrapper> fetchAll(String searchParam,
                                           WhatsAppStatus status,
                                           LocalDate startDate,
                                           LocalDate endDate,
                                           Boolean unread,
                                           Pageable pageable);

    void saveMessage(String body, String processType, String phoneNumber, String customerName, String salesOrderNumber);

    void saveBroadcastMessage(String body, String phoneNumber, String recipientName, String message);

    Page<WhatsAppResponse> fetchAllByCustomerTel(String phoneNumber, Pageable pageable);

    void updateCustomerCallStatus(@Valid WhatsAppRequest request, MultipartFile file, String name) throws IOException, InterruptedException;

    void sendMessage(@Valid WhatsAppRequest request, MultipartFile file, String name) throws IOException, InterruptedException;

    void saveReplyMessage(String body, WhatsAppRequest whatsAppRequest, Optional<Person> foundUser, WhatsAppOutboundMedia media);

    void readMessage(String phoneNumber, String name);

    Map<String, Long> getUnreadCount();
}
