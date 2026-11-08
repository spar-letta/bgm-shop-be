package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.models.request.YeastarDialRequest;
import com.app.itibari.models.request.YeastarWebhookEvent;
import com.app.itibari.models.response.YeastarApiResponse;
import com.app.itibari.models.response.YeastarCallRecord;
import com.app.itibari.models.response.YeastarExtension;
import com.app.itibari.services.YeastarCallService;
import com.app.itibari.services.YeastarCdrService;
import io.restassured.http.ContentType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class YeastarControllerTests extends ItibariApplicationTests {

    @MockBean
    private YeastarCallService yeastarCallService;

    @MockBean
    private YeastarCdrService yeastarCdrService;

    @Before
    public void mockYeastarResponses() {
        // dial
        YeastarApiResponse dialResponse = new YeastarApiResponse();

        dialResponse.setCallId("call_test_001");
        YeastarDialRequest request = new YeastarDialRequest();

        when(yeastarCallService.clickToCall(any(), anyString())).thenReturn(dialResponse);

        // hangup
        YeastarApiResponse hangupResponse = new YeastarApiResponse();

        when(yeastarCallService.hangupCall(anyString())).thenReturn(hangupResponse);

        // transfer
        YeastarApiResponse transferResponse = new YeastarApiResponse();

        when(yeastarCallService.transferCall(anyString(), anyString())).thenReturn(transferResponse);

        // extensions
        YeastarExtension ext1 = new YeastarExtension();
        ext1.setNumber("101");
        ext1.setName("Amina Hassan");
        ext1.setCallerIdName("Amina Hassan");
        ext1.setStatus("Idle");
        ext1.setEmail("amina.hassan@itibari.com");
        ext1.setMobile("+255711000101");

        YeastarExtension ext2 = new YeastarExtension();
        ext2.setNumber("102");
        ext2.setName("Juma Salim");
        ext2.setCallerIdName("Juma Salim");
        ext2.setStatus("OnCall");
        ext2.setEmail("juma.salim@itibari.com");
        ext2.setMobile("+255711000102");

        when(yeastarCallService.listExtensions()).thenReturn(List.of(ext1, ext2));

        // CDR records
        YeastarCallRecord record1 = new YeastarCallRecord();
        record1.setCallId("cdr_001");
        record1.setCaller("101");
        record1.setCallee("+255712345678");
        record1.setStartTime("2024-01-15 09:05:00");
        record1.setEndTime("2024-01-15 09:06:30");
        record1.setDuration(90);
        record1.setTalkDuration(85);
        record1.setStatus("ANSWERED");
        record1.setDirection("Outbound");
        record1.setRecordingFile("recordings/cdr_001.mp3");

        YeastarCallRecord record2 = new YeastarCallRecord();
        record2.setCallId("cdr_002");
        record2.setCaller("+255787654321");
        record2.setCallee("102");
        record2.setStartTime("2024-01-15 11:20:00");
        record2.setEndTime("2024-01-15 11:20:18");
        record2.setDuration(18);
        record2.setTalkDuration(0);
        record2.setStatus("NO ANSWER");
        record2.setDirection("Inbound");
        record2.setRecordingFile(null);

        when(yeastarCdrService.getCallHistoryByPhone(anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(record1, record2));

        when(yeastarCdrService.getCallHistoryByExtension(anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(record1, record2));
    }

    @Ignore
    @Test
    public void testDial_extension1006_toCallee255764840059() {
        YeastarDialRequest request = new YeastarDialRequest();
        request.setCallee("+255764840059");
        request.setIsChurned(Boolean.TRUE);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request).log().all()
                .post("/yeastar/call/dial")
                .then().log().all()
                .statusCode(200)
                .body("status", equalTo("Success"))
                .body("callid", equalTo("call_test_001"));
    }

    @Ignore
    @Test
    public void testDial_shouldReturn200_withCallId() {
        YeastarDialRequest request = new YeastarDialRequest();
        request.setCallee("+255712345678");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/yeastar/call/dial")
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("status", equalTo("Success"))
                .body("call_id", equalTo("call_test_001"));
    }

    @Ignore
    @Test
    public void testDial_shouldReturn401_withoutToken() {
        YeastarDialRequest request = new YeastarDialRequest();
        request.setCallee("+255712345678");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .post("/yeastar/call/dial")
                .then()
                .statusCode(401);
    }


    @Ignore
    @Test
    public void testHangup_shouldReturn200() {
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .post("/yeastar/call/{callId}/hangup", "call_test_001")
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("status", equalTo("Success"));
    }

    @Ignore
    @Test
    public void testTransfer_shouldReturn200() {
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("targetExtension", "ext202")
                .post("/yeastar/call/{callId}/transfer", "call_test_001")
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("status", equalTo("Success"));
    }

    @Ignore
    @Test
    public void testListExtensions_shouldReturn200_withExtensions() {
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/yeastar/extensions")
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("size()", equalTo(2))
                .body("[0].number", equalTo("101"))
                .body("[0].name", equalTo("Amina Hassan"))
                .body("[0].status", equalTo("Idle"))
                .body("[1].number", equalTo("102"))
                .body("[1].name", equalTo("Juma Salim"))
                .body("[1].status", equalTo("OnCall"));
    }

    @Ignore
    @Test
    public void testGetCdrByPhone_shouldReturn200_withRecords() {
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("phoneNumber", "+255712345678")
                .queryParam("from", "2024-01-01")
                .queryParam("to", "2024-01-31")
                .get("/yeastar/cdr")
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("size()", equalTo(2))
                .body("[0].call_id", equalTo("cdr_001"))
                .body("[0].caller", equalTo("101"))
                .body("[0].callee", equalTo("+255712345678"))
                .body("[0].status", equalTo("ANSWERED"))
                .body("[0].direction", equalTo("Outbound"))
                .body("[1].call_id", equalTo("cdr_002"))
                .body("[1].status", equalTo("NO ANSWER"))
                .body("[1].direction", equalTo("Inbound"));
    }

    @Ignore
    @Test
    public void testGetCdrByExtension_shouldReturn200_withRecords() {
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("extension", "ext101")
                .queryParam("from", "2024-01-01")
                .queryParam("to", "2024-01-31")
                .get("/yeastar/cdr/extension")
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("size()", equalTo(2))
                .body("[0].call_id", equalTo("cdr_001"))
                .body("[0].caller", equalTo("101"))
                .body("[0].direction", equalTo("Outbound"));
    }

    @Ignore
    @Test
    public void testWebhook_shouldReturn200_withoutAuthentication() {
        YeastarWebhookEvent event = new YeastarWebhookEvent();
        event.setEvent("CALL_INCOMING");
        event.setCallId("call_inbound_001");
        event.setCaller("+255712345678");
        event.setCallee("ext101");
        event.setStatus("RINGING");
        event.setCallDirection("Inbound");

        // Webhook is whitelisted — no JWT required
        given()
                .contentType(ContentType.JSON)
                .body(event)
                .post("/yeastar/webhook")
                .then().log().ifValidationFails()
                .statusCode(200);
    }

    @Ignore
    @Test
    public void testGetCdrByPhone_shouldReturn400_whenMissingRequiredParams() {
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("phoneNumber", "+255712345678")
                .queryParam("from", "2024-01-01")
                .queryParam("to", "2024-01-31")
                .get("/yeastar/cdr")
                .then()
                .statusCode(200);
    }
}
