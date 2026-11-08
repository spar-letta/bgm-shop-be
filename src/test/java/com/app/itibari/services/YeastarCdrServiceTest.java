package com.app.itibari.services;

import com.app.itibari.models.request.YeastarCdrRequest;
import com.app.itibari.models.response.YeastarCallRecord;
import com.app.itibari.models.response.YeastarCdrResponse;
import com.app.itibari.utils.feign.YeastarApiClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class YeastarCdrServiceTest {

    @Mock
    private YeastarApiClient yeastarApiClient;

    @Mock
    private YeastarTokenService yeastarTokenService;

    @InjectMocks
    private YeastarCdrService yeastarCdrService;

    @Before
    public void setUp() {
        when(yeastarTokenService.getValidToken()).thenReturn("test_token");
    }

    @Test
    public void getCallHistoryByPhone_shouldReturnRecords_whenPbxResponds() {
        YeastarCallRecord record = buildCallRecord("call_001", "ext101", "+255712345678");
        YeastarCdrResponse cdrResponse = buildCdrResponse(List.of(record));
        when(yeastarApiClient.searchCdr(anyString(), any(YeastarCdrRequest.class))).thenReturn(cdrResponse);

        List<YeastarCallRecord> records = yeastarCdrService.getCallHistoryByPhone(
                "+255712345678",
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31)
        );

        assertEquals(1, records.size());
        assertEquals("call_001", records.get(0).getCallId());
        assertEquals("+255712345678", records.get(0).getCallee());
    }

    @Test
    public void getCallHistoryByPhone_shouldPassCorrectDateRange() {
        YeastarCdrResponse cdrResponse = buildCdrResponse(List.of());
        when(yeastarApiClient.searchCdr(anyString(), any(YeastarCdrRequest.class))).thenReturn(cdrResponse);

        ArgumentCaptor<YeastarCdrRequest> captor = ArgumentCaptor.forClass(YeastarCdrRequest.class);

        yeastarCdrService.getCallHistoryByPhone(
                "+255712345678",
                LocalDate.of(2024, 3, 1),
                LocalDate.of(2024, 3, 31)
        );

        verify(yeastarApiClient).searchCdr(anyString(), captor.capture());
        YeastarCdrRequest captured = captor.getValue();
        assertEquals("+255712345678", captured.getCallee());
        assertEquals("2024-03-01 00:00:00", captured.getStartTime());
        assertEquals("2024-03-31 23:59:59", captured.getEndTime());
        assertEquals(1, captured.getPage());
        assertEquals(50, captured.getLimit());
    }

    @Test
    public void getCallHistoryByPhone_shouldReturnEmptyList_whenPbxFails() {
        YeastarCdrResponse failResponse = new YeastarCdrResponse();
        failResponse.setStatus("Failed");
        when(yeastarApiClient.searchCdr(anyString(), any(YeastarCdrRequest.class))).thenReturn(failResponse);

        List<YeastarCallRecord> records = yeastarCdrService.getCallHistoryByPhone(
                "+255712345678",
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31)
        );

        assertTrue(records.isEmpty());
    }

    @Test
    public void getCallHistoryByExtension_shouldQueryByCaller() {
        YeastarCallRecord record = buildCallRecord("call_002", "ext101", "+255799000000");
        YeastarCdrResponse cdrResponse = buildCdrResponse(List.of(record));
        when(yeastarApiClient.searchCdr(anyString(), any(YeastarCdrRequest.class))).thenReturn(cdrResponse);

        ArgumentCaptor<YeastarCdrRequest> captor = ArgumentCaptor.forClass(YeastarCdrRequest.class);

        List<YeastarCallRecord> records = yeastarCdrService.getCallHistoryByExtension(
                "ext101",
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 2, 29)
        );

        verify(yeastarApiClient).searchCdr(anyString(), captor.capture());
        assertEquals("ext101", captor.getValue().getCaller());
        assertNull(captor.getValue().getCallee());
        assertEquals(1, records.size());
    }

    @Test
    public void getCallHistoryByPhone_shouldReturnEmptyList_whenCdrListIsNull() {
        YeastarCdrResponse cdrResponse = new YeastarCdrResponse();
        cdrResponse.setStatus("Success");
        cdrResponse.setCdrList(null);
        when(yeastarApiClient.searchCdr(anyString(), any(YeastarCdrRequest.class))).thenReturn(cdrResponse);

        List<YeastarCallRecord> records = yeastarCdrService.getCallHistoryByPhone(
                "+255712345678",
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31)
        );

        assertTrue(records.isEmpty());
    }

    private YeastarCallRecord buildCallRecord(String callId, String caller, String callee) {
        YeastarCallRecord record = new YeastarCallRecord();
        record.setCallId(callId);
        record.setCaller(caller);
        record.setCallee(callee);
        record.setStatus("ANSWERED");
        record.setDuration(120);
        record.setTalkDuration(90);
        return record;
    }

    private YeastarCdrResponse buildCdrResponse(List<YeastarCallRecord> records) {
        YeastarCdrResponse response = new YeastarCdrResponse();
        response.setStatus("Success");
        response.setTotalNumber(records.size());
        response.setCdrList(records);
        return response;
    }
}