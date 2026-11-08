package com.app.itibari.utils.rsaKeys;

import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

@Slf4j
public class KeyPairGeneratorUtils {

    public static KeyPair generateKeyPair() {

        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(4096);
            keyPair = keyPairGenerator.generateKeyPair();
            return keyPair;
        } catch (Exception e) {
            throw new IllegalStateException("Exception occurred while generating RSA key pair", e);
        }
    }
}
