package com.app.itibari.utils.rsaKeys;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Created by christopher oigo (oigoc60@gmail.com) on May 2025
 */
@Slf4j
public class CertificateUtils {

    /**
     * Generate a self-signed X509 certificate using Bouncy Castle
     *
     * @param keyPair   The key pair to use for the certificate
     * @param dn        The Distinguished Name for the certificate subject
     * @param notBefore Validity start date
     * @param notAfter  Validity end date
     * @return A self-signed X509 certificate
     */
    public static X509Certificate generateSelfSignedCertificate(
            KeyPair keyPair, String dn, Date notBefore, Date notAfter) {

        try {
            X500Name issuerName = new X500Name(dn);
            BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());

            X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    issuerName,
                    serial,
                    notBefore,
                    notAfter,
                    issuerName,
                    keyPair.getPublic()
            );

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                    .build(keyPair.getPrivate());

            return new JcaX509CertificateConverter()
                    .getCertificate(certBuilder.build(signer));
        } catch (Exception e) {
            log.error("Failed to generate self-signed certificate", e);
            throw new RuntimeException("Failed to generate self-signed certificate", e);
        }
    }
}
