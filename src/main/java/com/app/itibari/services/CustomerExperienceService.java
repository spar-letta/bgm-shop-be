package com.app.itibari.services;

import com.app.itibari.dataTypes.SmsChannel;
import com.app.itibari.models.entities.CustomerExperience;
import com.app.itibari.models.entities.CustomerNotes;
import com.app.itibari.models.entities.PrioritySetting;
import com.app.itibari.models.request.*;
import com.app.itibari.models.response.CustomerNotesDTO;
import com.app.itibari.models.response.FollowUpDto;
import com.app.itibari.models.response.PrioritySettingResponse;
import com.app.itibari.models.response.SmsTemplateResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public interface CustomerExperienceService {
    CustomerExperience createCustomerExperience(@Valid CustomerExperienceRequest customerExperienceRequest, Principal principal);

    Page<CustomerExperience> getCustomerExperience(String searchParam, LocalDate startDate, LocalDate endDate, UUID issueTypePublicId, Boolean isActive, TicketDto.CustomerType customerType, Pageable pageable);

    PrioritySettingResponse createPrioritySetting(@Valid PrioritySettingRequest prioritySettingRequest, Principal principal);

    Page<PrioritySettingResponse> getPrioritySettings(String searchParam, LocalDate startDate, LocalDate endDate, Boolean isActive, Pageable pageable);

    PrioritySettingResponse updatePrioritySetting(UUID publicId, @Valid PrioritySettingRequest prioritySettingRequest, Principal principal);

    PrioritySettingResponse fetchPrioritySetting(UUID publicId);

    void deletePrioritySetting(UUID publicId, Principal principal);

    PrioritySetting actionOnPrioritySetting(UUID publicId, Boolean isActive, Principal principal);

    CustomerExperience fetchCustomerExperience(UUID publicId, Principal principal);

    CustomerExperience actionOnCustomerExperience(UUID publicId, Boolean isActive, Principal principal);

    CustomerExperience updateCustomerExperience(UUID publicId, @Valid CustomerExperienceRequest customerExperienceRequest, Principal principal);

    void actionDeleteTemplate(UUID publicId, Principal principal);

    CustomerNotesDTO addCustomerNotes(UUID customerPublicId, @Valid CustomerNotesRequest customerNotesRequest, Principal principal);

    CustomerNotesDTO updateCustomerNotes(UUID publicId, @Valid CustomerNotesRequest customerNotesRequest, Principal principal);

    CustomerNotesDTO getCustomerNotesDTO(UUID publicId, Principal principal);

    Page<CustomerNotesDTO> listCustomerNotes(UUID publicId, Pageable pageable);

    Map<String, String> deleteCustomerNotes(UUID publicId, Principal principal);

    SmsTemplateResponse createSmsTemplateRequest(@Valid SmsTemplateRequest smsTemplateRequest, Principal principal);

    Page<SmsTemplateResponse> getSmsTemplates(String searchParam, Boolean status, SmsChannel channel, Pageable pageable, Principal principal);

    SmsTemplateResponse updateSmsTemplateRequest(UUID publicId, @Valid SmsTemplateRequest smsTemplateRequest, Principal principal);

    void deleteSmsTemplateRequest(UUID publicId, Principal principal);

    void actionSmsTemplateRequest(UUID publicId, Principal principal);

    void sendCustomerNotesToDepartmentOfficers() throws IOException;
}
