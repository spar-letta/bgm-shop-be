package com.app.itibari.dataTypes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum StatementExportType {
    PDF("PDF", ".pdf", "application/pdf"),
    EXCEL("EXCEL", ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    public final String label;
    public final String extension;
    public final String contentType;

    StatementExportType(String label, String extension, String contentType) {
        this.label = label;
        this.extension = extension;
        this.contentType = contentType;
    }

    @JsonValue
    public String getLabel() {
        return this.label;
    }

    @JsonCreator
    public static StatementExportType forValue(String value) {
        if (value != null) {
            switch (value.toUpperCase().replaceAll(" ", "")) {
                case "PDF":
                    return PDF;
                case "EXCEL":
                    return EXCEL;
                default:
                    throw new IllegalStateException("Unexpected value: " + value.toUpperCase());
            }
        } else {
            throw new IllegalStateException("Unexpected value: " + value);
        }
    }
}
