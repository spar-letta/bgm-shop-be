package com.app.itibari.utils.feign;

public class AuthEncoder {

    public static String encode(String username, String password) {

        return "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }
}
