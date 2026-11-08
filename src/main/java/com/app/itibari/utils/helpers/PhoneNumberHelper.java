package com.app.itibari.utils.helpers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PhoneNumberHelper {

    public String getPhoneNumber(String phoneNumber) {

        if (phoneNumber.startsWith("+")) {
            return phoneNumber;
        } else if (phoneNumber.startsWith("0")) {
            return "+255" + phoneNumber.substring(1);
        } else {
            return "+255" + phoneNumber;
        }
    }

    public String formatPhoneNumberForDisplay(String phoneNumber) {

        if (phoneNumber.startsWith("+255")) {
            return "0" + phoneNumber.substring(4);
        } else {
            return phoneNumber;
        }
    }

    public static String getLastNineDigits(String phoneNumber) {

        if (phoneNumber == null) {
            return null;
        }

        String cleaned = phoneNumber.replaceAll("[^0-9]", "");

        if (cleaned.length() <= 9) {
            return cleaned;
        }

        return cleaned.substring(cleaned.length() - 9);
    }

    public static String toInternationalFormat(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        phoneNumber = phoneNumber.replaceAll("[\\s-]", "");

        if (phoneNumber.startsWith("+")) {
            return phoneNumber;
        } else if (phoneNumber.startsWith("0")) {
            return "+255" + phoneNumber.substring(1);
        } else if (phoneNumber.startsWith("255")) {
            return "+" + phoneNumber;
        } else {
            return "+255" + phoneNumber;
        }
    }

    public static Map<Boolean, String> checkPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return Map.of(false, "Phone number cannot be null or empty");
        }

        String normalized = toInternationalFormat(phoneNumber);

        String digitsOnly = normalized.substring(1);
        if (!digitsOnly.matches("\\d+")) {
            return Map.of(false, "Phone number must contain only digits");
        }

        if (digitsOnly.length() != 12) {
            return Map.of(false, "Phone number must have exactly 12 digits including country code");
        }

        return Map.of(true, "Success: " + normalized);
    }

}
