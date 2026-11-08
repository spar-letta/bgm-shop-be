package com.app.itibari.utils.reportHelper;

import com.app.itibari.models.response.CallStatusSummary;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ChurnedCustomerExportHelperTest {

    @Test
    void prefersPersistedDescription() {
        CallStatusSummary status = new CallStatusSummary(UUID.randomUUID(), "Hang Up", "Customer hung up");
        assertEquals("Customer hung up", ChurnedCustomerExportHelper.resolveCallStatusLabel(status));
    }

    @Test
    void resolvesHangUpCatalogAliasesWhenDescriptionMissing() {
        assertEquals("Hang Up",
                ChurnedCustomerExportHelper.resolveCallStatusLabel(
                        new CallStatusSummary(UUID.randomUUID(), "Hang Up", null)));
        assertEquals("Hang Up",
                ChurnedCustomerExportHelper.resolveCallStatusLabel(
                        new CallStatusSummary(UUID.randomUUID(), "HANG_UP", "")));
        assertEquals("Hang Up",
                ChurnedCustomerExportHelper.resolveCallStatusLabel(
                        new CallStatusSummary(UUID.randomUUID(), "Hangup", null)));
    }

    @Test
    void resolvesCanonicalEnumCodeWhenDescriptionMissing() {
        assertEquals("Successful",
                ChurnedCustomerExportHelper.resolveCallStatusLabel(
                        new CallStatusSummary(UUID.randomUUID(), "SUCCESSFUL", null)));
    }

    @Test
    void unknownCatalogCodeDoesNotThrow_fallsBackToCode() {
        String label = assertDoesNotThrow(() ->
                ChurnedCustomerExportHelper.resolveCallStatusLabel(
                        new CallStatusSummary(UUID.randomUUID(), "SOME_FUTURE_STATUS", null)));
        assertEquals("SOME_FUTURE_STATUS", label);
    }

    @Test
    void nullOrBlankSafe() {
        assertEquals("--", ChurnedCustomerExportHelper.resolveCallStatusLabel(null));
        assertEquals("--",
                ChurnedCustomerExportHelper.resolveCallStatusLabel(
                        new CallStatusSummary(UUID.randomUUID(), null, null)));
        assertEquals("--",
                ChurnedCustomerExportHelper.resolveCallStatusLabel(
                        new CallStatusSummary(UUID.randomUUID(), "  ", "")));
    }
}
