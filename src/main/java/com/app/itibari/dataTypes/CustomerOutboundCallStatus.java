package com.app.itibari.dataTypes;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Fixed outcomes for {@code PATCH /customers/{publicId}/callStatus}.
 * Each constant maps to a {@code call_status.code} row (case-insensitive) that must be active.
 */
public enum CustomerOutboundCallStatus {
    ON_HOLD("On Hold"),
    SUCCESSFUL("Successful"),
    FAILED("Failed"),
    REJECTED("Rejected"),
    UNANSWERED("Unanswered"),
    CALL_DISCONNECTED("Call Disconnected"),
    USER_BUSY("User Busy"),
    CALL_BACK("Call Back"),
    SWITCHED_OFF("Switched Off"),
    WRONG_NUMBER("Wrong Number"),
    HANG_UP("Hang Up");

    private final String label;

    CustomerOutboundCallStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String catalogCode() {
        if (this == HANG_UP) {
            return "Hang Up";
        }
        return name();
    }

    @JsonCreator
    public static CustomerOutboundCallStatus fromValue(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        for (CustomerOutboundCallStatus status : values()) {
            if (status.name().equalsIgnoreCase(trimmed)
                    || status.catalogCode().equalsIgnoreCase(trimmed)) {
                return status;
            }
        }
        String compact = trimmed.replaceAll("[\\s_-]+", "");
        if (compact.equalsIgnoreCase("HANGUP")) {
            return HANG_UP;
        }
        throw new IllegalArgumentException(
                "Unknown CustomerOutboundCallStatus: " + raw);
    }
}
