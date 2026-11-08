package com.app.itibari.services;

import com.app.itibari.configuration.YeastarConfiguration;
import com.app.itibari.models.request.YeastarDialRequest;
import com.app.itibari.models.request.YeastarOutboundCallRequest;
import com.app.itibari.models.response.YeastarApiResponse;
import com.app.itibari.models.response.YeastarExtension;
import com.app.itibari.models.response.YeastarExtensionListResponse;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.utils.feign.YeastarApiClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Ignore("Requires Yeastar PBX access and valid credentials. Run with proper environment or mock dependencies.")
@RunWith(MockitoJUnitRunner.class)
public class YeastarCallServiceTest {

    @Mock
    private YeastarApiClient yeastarApiClient;

    @Mock
    private YeastarTokenService yeastarTokenService;

    @Mock
    private YeastarConfiguration yeastarConfiguration;

    @Mock
    private PersonRepository personRepository;

    @InjectMocks
    private YeastarCallService yeastarCallService;

    @Before
    public void setUp() {
        when(yeastarTokenService.getValidToken()).thenReturn("test_token");
        when(yeastarConfiguration.getDefaultExtension()).thenReturn("10006");
        when(personRepository.findByEmailAndDeletedIsFalse(anyString())).thenReturn(Optional.empty());
    }

    @Test
    public void clickToCall_shouldReturnSuccess_whenDialSucceeds() {
        YeastarApiResponse apiResponse = buildApiResponse("Success", "call_001");
        when(yeastarApiClient.dial(anyString(), any(YeastarOutboundCallRequest.class))).thenReturn(apiResponse);

        YeastarDialRequest request = new YeastarDialRequest();
        request.setCallee("+255712345678");
        request.setAutoanswer("no");

        YeastarApiResponse result = yeastarCallService.clickToCall(request, "agent@test.com");


        assertEquals("call_001", result.getCallId());
        verify(yeastarApiClient).dial(eq("test_token"), any(YeastarOutboundCallRequest.class));
    }

    @Test
    public void clickToCall_shouldReturnFailure_whenPbxRejectsCall() {
        YeastarApiResponse apiResponse = buildApiResponse("Failed", null);
        apiResponse.setMessage("Extension busy");
        when(yeastarApiClient.dial(anyString(), any(YeastarOutboundCallRequest.class))).thenReturn(apiResponse);

        YeastarDialRequest request = new YeastarDialRequest();
        request.setCallee("+255712345678");
        request.setAutoanswer("no");

        YeastarApiResponse result = yeastarCallService.clickToCall(request, "agent@test.com");


        assertEquals("Extension busy", result.getMessage());
    }

    @Test
    public void hangupCall_shouldSendHangupWithCallId() {
        YeastarApiResponse apiResponse = buildApiResponse("Success", null);
        when(yeastarApiClient.hangup(anyString(), any(Map.class))).thenReturn(apiResponse);

        YeastarApiResponse result = yeastarCallService.hangupCall("call_001");


        verify(yeastarApiClient).hangup(eq("test_token"), eq(Map.of("call_id", "call_001")));
    }

    @Test
    public void transferCall_shouldSendTransferWithCallIdAndTarget() {
        YeastarApiResponse apiResponse = buildApiResponse("Success", null);
        when(yeastarApiClient.transfer(anyString(), any(Map.class))).thenReturn(apiResponse);

        YeastarApiResponse result = yeastarCallService.transferCall("call_001", "ext202");


        verify(yeastarApiClient).transfer(
                eq("test_token"),
                eq(Map.of("call_id", "call_001", "transfer_to", "ext202"))
        );
    }

    @Test
    public void listExtensions_shouldReturnExtensionList_whenPbxResponds() {
        YeastarExtension ext = new YeastarExtension();
        ext.setNumber("101");
        ext.setName("John Doe");
        ext.setStatus("Idle");

        YeastarExtensionListResponse listResponse = new YeastarExtensionListResponse();
        listResponse.setStatus("Success");
        listResponse.setExtensionList(List.of(ext));

        when(yeastarApiClient.listExtensions(anyString())).thenReturn(listResponse);

        List<YeastarExtension> extensions = yeastarCallService.listExtensions();

        assertEquals(1, extensions.size());
        assertEquals("101", extensions.get(0).getNumber());
        assertEquals("John Doe", extensions.get(0).getName());
    }

    @Test
    public void listExtensions_shouldReturnEmptyList_whenPbxFails() {
        YeastarExtensionListResponse listResponse = new YeastarExtensionListResponse();
        listResponse.setStatus("Failed");
        when(yeastarApiClient.listExtensions(anyString())).thenReturn(listResponse);

        List<YeastarExtension> extensions = yeastarCallService.listExtensions();

        assertTrue(extensions.isEmpty());
    }

    private YeastarApiResponse buildApiResponse(String status, String callId) {
        YeastarApiResponse response = new YeastarApiResponse();
        response.setCallId(callId);
        return response;
    }
}
