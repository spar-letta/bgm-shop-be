package com.app.itibari.utils.formatter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateFormatter {

    public static LocalDate parseDate(String dateStr) {

        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format for: " + dateStr + ". Expected yyyy-MM-dd");
        }
    }

    public static String formatDate(LocalDateTime dateTime) {

        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static String formatDateToString(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    public static String format2DateToString(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return localDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
    }

    public static String formatTime(LocalDateTime creationTime) {
        if (creationTime == null) {
            return null;
        }
        return creationTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public static String formatDateToEastAfrica(LocalDateTime dateTime) {

        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
    }
}
