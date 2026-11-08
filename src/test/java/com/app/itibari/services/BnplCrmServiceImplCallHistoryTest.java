package com.app.itibari.services;

import com.app.itibari.converter.BnplLeadNotesToDTO;
import com.app.itibari.converter.CustomerNotesToResponse;
import com.app.itibari.dataTypes.BnplCallStatus;
import com.app.itibari.models.entities.BnplCallAttempt;
import com.app.itibari.models.entities.BnplLead;
import com.app.itibari.models.entities.CallStatus;
import com.app.itibari.models.response.BnplCallAttemptResponse;
import com.app.itibari.repository.*;
import com.app.itibari.services.helper.BnplHelper;
import com.app.itibari.utils.helpers.PhoneNumberHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class BnplCrmServiceImplCallHistoryTest {

    private static final UUID LEAD_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @InjectMocks
    private BnplCrmServiceImpl service;

    @Mock private BnplLeadRepository bnplLeadRepository;
    @Mock private BnplCallAttemptRepository bnplCallAttemptRepository;
    @Mock private BnplFieldVisitRepository bnplFieldVisitRepository;
    @Mock private BnplReasonCodeRepository bnplReasonCodeRepository;
    @Mock private CustomersRepository customersRepository;
    @Mock private PersonRepository personRepository;
    @Mock private BnplHelper bnplHelper;
    @Mock private RejectionReasonRepository rejectionReasonRepository;
    @Mock private RejectionCodeRepository rejectionCodeRepository;
    @Mock private CustomerNotesRepository customerNotesRepository;
    @Mock private CustomerNotesToResponse customerNotesToResponse;
    @Mock private BnplLeadNotesToDTO bnplLeadNotesToDTO;
    @Mock private SalesAgentsRepository salesAgentsRepository;
    @Mock private SalesOrdersRepository salesOrdersRepository;
    @Mock private ItibariSmsService itibariSmsService;
    @Mock private PhoneNumberHelper phoneNumberHelper;
    @Mock private YeastarCallService yeastarCallService;
    @Mock private CallStatusRepository callStatusRepository;

    private BnplLead lead;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        lead = new BnplLead();
        lead.setPublicId(LEAD_ID);
        when(bnplLeadRepository.findByPublicIdAndDeletedIsFalse(LEAD_ID)).thenReturn(Optional.of(lead));
    }

    @Test
    void getCallHistory_catalogOutcomeCode_doesNotThrowAndReturnsOutcomeFields() {
        CallStatus catalogStatus = new CallStatus();
        catalogStatus.setCode("WILL_ORDER_LATER");
        catalogStatus.setDescription("Customer interested, will order later");

        BnplCallAttempt attempt = BnplCallAttempt.builder()
                .lead(lead)
                .attemptNumber(1)
                .outComeStatus(catalogStatus)
                .build();

        when(bnplCallAttemptRepository.findByLeadAndDeletedIsFalseOrderByStartTimeDesc(eq(lead), any()))
                .thenReturn(new PageImpl<>(List.of(attempt)));

        Page<BnplCallAttemptResponse> page = service.getCallHistory(LEAD_ID, PageRequest.of(0, 20));
        BnplCallAttemptResponse response = page.getContent().get(0);

        assertNull(response.getCallStatus());
        assertEquals("WILL_ORDER_LATER", response.getOutcomeStatusCode());
        assertEquals("Customer interested, will order later", response.getOutcomeStatusDescription());
    }

    @Test
    void getCallHistory_bnplEnumOnAttempt_stillReturnsCallStatus() {
        BnplCallAttempt attempt = BnplCallAttempt.builder()
                .lead(lead)
                .attemptNumber(1)
                .callStatus(BnplCallStatus.NO_ANSWER)
                .build();

        when(bnplCallAttemptRepository.findByLeadAndDeletedIsFalseOrderByStartTimeDesc(eq(lead), any()))
                .thenReturn(new PageImpl<>(List.of(attempt)));

        Page<BnplCallAttemptResponse> page = service.getCallHistory(LEAD_ID, PageRequest.of(0, 20));

        assertEquals(BnplCallStatus.NO_ANSWER, page.getContent().get(0).getCallStatus());
    }

    @Test
    void getCallHistory_matchingCatalogCode_stillMapsLegacyCallStatus() {
        CallStatus catalogStatus = new CallStatus();
        catalogStatus.setCode("ANSWERED");
        catalogStatus.setDescription("Answered");

        BnplCallAttempt attempt = BnplCallAttempt.builder()
                .lead(lead)
                .attemptNumber(1)
                .outComeStatus(catalogStatus)
                .build();

        when(bnplCallAttemptRepository.findByLeadAndDeletedIsFalseOrderByStartTimeDesc(eq(lead), any()))
                .thenReturn(new PageImpl<>(List.of(attempt)));

        Page<BnplCallAttemptResponse> page = service.getCallHistory(LEAD_ID, PageRequest.of(0, 20));
        BnplCallAttemptResponse response = page.getContent().get(0);

        assertEquals(BnplCallStatus.ANSWERED, response.getCallStatus());
        assertEquals("ANSWERED", response.getOutcomeStatusCode());
    }
}
