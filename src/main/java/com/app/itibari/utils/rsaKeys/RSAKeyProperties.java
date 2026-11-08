package com.app.itibari.utils.rsaKeys;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Getter
@Setter
@Component
@Slf4j
public class RSAKeyProperties {

    private RSAPrivateKey privateKey;

    private RSAPublicKey publicKey;

    @Value("${security.jwt.key-store.path}")
    private String keyStorePath;

    @Value("${security.jwt.key-store.password}")
    private String keyStorePassword;

    @Value("${security.jwt.key-store.key-alias}")
    private String keyAlias;

    @Value("${security.jwt.key-store.private-key-passphrase}")
    private String privateKeyPassphrase;

    private final ResourceLoader resourceLoader;

    public RSAKeyProperties(ResourceLoader resourceLoader) {

        this.resourceLoader = resourceLoader;
    }

    /**
     * Initialize RSA keys from the keystore.
     * This method is called by Spring after property values are set.
     */
    @PostConstruct
    public void initializeKeys() {

        try {
            Resource resource = resourceLoader.getResource(keyStorePath);
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

            try (var inputStream = resource.getInputStream()) {
                keyStore.load(inputStream, keyStorePassword.toCharArray());

                // Extract private key
                this.privateKey = (RSAPrivateKey) keyStore.getKey(
                        keyAlias,
                        privateKeyPassphrase.toCharArray()
                );

                // Extract public key from the certificate
                this.publicKey = (RSAPublicKey) keyStore.getCertificate(keyAlias).getPublicKey();

                log.info("Successfully loaded RSA keys from keystore");
            }
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException |
                 CertificateException | UnrecoverableKeyException e) {
            log.error("Failed to load RSA keys from keystore, falling back to generating new keys", e);
            // Fall back to generating keys if loading fails
            fallbackToGeneratedKeys();
        }
    }

    /**
     * Generate new keys if loading from keystore fails.
     * This is a fallback method.
     */
    private void fallbackToGeneratedKeys() {

        log.warn("Using generated RSA keys - FOR DEVELOPMENT USE ONLY");
        var keyPair = KeyPairGeneratorUtils.generateKeyPair();
        this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        this.publicKey = (RSAPublicKey) keyPair.getPublic();
    }
}
