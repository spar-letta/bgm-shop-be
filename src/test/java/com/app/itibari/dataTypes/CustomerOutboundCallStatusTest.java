package com.app.itibari.dataTypes;

import com.app.itibari.models.request.UpdateCustomerCallStatusRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerOutboundCallStatusTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void fromValue_acceptsHangUpCatalogCode() {
        assertEquals(CustomerOutboundCallStatus.HANG_UP, CustomerOutboundCallStatus.fromValue("Hang Up"));
        assertEquals(CustomerOutboundCallStatus.HANG_UP, CustomerOutboundCallStatus.fromValue("HANG_UP"));
        assertEquals(CustomerOutboundCallStatus.HANG_UP, CustomerOutboundCallStatus.fromValue("Hangup"));
    }

    @Test
    void catalogCode_hangUpMatchesDbRow() {
        assertEquals("Hang Up", CustomerOutboundCallStatus.HANG_UP.catalogCode());
        assertEquals("SUCCESSFUL", CustomerOutboundCallStatus.SUCCESSFUL.catalogCode());
    }

    @Test
    void jackson_deserializesHangUpInUpdateRequest() throws Exception {
        UpdateCustomerCallStatusRequest request = objectMapper.readValue(
                "{\"status\":\"Hang Up\"}",
                UpdateCustomerCallStatusRequest.class);
        assertEquals(CustomerOutboundCallStatus.HANG_UP, request.getStatus());
    }

    @Test
    void jackson_stillAcceptsCanonicalEnumNames() throws Exception {
        UpdateCustomerCallStatusRequest request = objectMapper.readValue(
                "{\"status\":\"SUCCESSFUL\"}",
                UpdateCustomerCallStatusRequest.class);
        assertEquals(CustomerOutboundCallStatus.SUCCESSFUL, request.getStatus());
    }

    @Test
    void fromValue_rejectsUnknownOutcome() {
        assertThrows(IllegalArgumentException.class,
                () -> CustomerOutboundCallStatus.fromValue("NOT_A_REAL_STATUS"));
    }
}
