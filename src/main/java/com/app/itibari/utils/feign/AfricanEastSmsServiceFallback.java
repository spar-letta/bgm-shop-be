package com.app.itibari.utils.feign;

public class AfricanEastSmsServiceFallback {

    public static final String SMS_FALLBACK_MESSAGE = "SMS service is currently unavailable. Please try again later.";

    public static String sendSms(String phoneNumber, String message) {

        return SMS_FALLBACK_MESSAGE;
    }
}
