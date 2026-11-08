package com.app.itibari.utils.rsaKeys;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Created by christopher oigo (oigoc60@gmail.com) on May 2025
 */
@Component
@Slf4j
public class KeyStoreInitializer {

    private final RSAKeyProperties rsaKeyProperties;

    private final ResourceLoader resourceLoader;

    @Value("${security.jwt.key-store.path}")
    private String keyStorePath;

    @Value("${security.jwt.key-store.password}")
    private String keyStorePassword;

    @Value("${security.jwt.key-store.key-alias}")
    private String keyAlias;

    @Value("${security.jwt.key-store.private-key-passphrase}")
    private String privateKeyPassphrase;

    public KeyStoreInitializer(RSAKeyProperties rsaKeyProperties, ResourceLoader resourceLoader) {

        this.rsaKeyProperties = rsaKeyProperties;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {

        try {
            // Try to load the existing keystore
            Resource resource = resourceLoader.getResource(keyStorePath);

            if (!resource.exists()) {
                // Keystore doesn't exist, create one
                log.info("Keystore not found at: {}. Creating new keystore...", keyStorePath);
                createKeyStore();
            }

            // Initialize keys from the keystore (existing or newly created)
            rsaKeyProperties.initializeKeys();
        } catch (Exception e) {
            log.error("Failed to initialize keystore", e);
            throw new RuntimeException("Failed to initialize keystore", e);
        }
    }

    private void createKeyStore() {

        try {
            // Create directory structure if it doesn't exist
            String path = keyStorePath.replace("classpath:", "src/main/resources/");
            File keystoreFile = new File(path);

            if (!keystoreFile.getParentFile().exists()) {
                keystoreFile.getParentFile().mkdirs();
            }

            // Generate a new key pair
            KeyPair keyPair = KeyPairGeneratorUtils.generateKeyPair();

            // Create a self-signed certificate
            X509Certificate certificate = CertificateUtils.generateSelfSignedCertificate(
                    keyPair,
                    "CN=Itibari JWT Signing Key, O=Itibari, C=KE",
                    new Date(),
                    new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L));

            // Create a new keystore
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);

            // Add the key entry with the certificate
            keyStore.setKeyEntry(
                    keyAlias,
                    keyPair.getPrivate(),
                    privateKeyPassphrase.toCharArray(),
                    new Certificate[]{certificate}
            );

            // Save the keystore to file
            try (FileOutputStream fos = new FileOutputStream(keystoreFile)) {
                keyStore.store(fos, keyStorePassword.toCharArray());
                log.info("Keystore created successfully at: {}", keystoreFile.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Failed to create keystore", e);
            throw new RuntimeException("Failed to create keystore", e);
        }
    }
}
