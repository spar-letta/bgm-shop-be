# JWT Key Management for Itibari Service

## Overview

This document explains the key management system for JWT authentication in the Itibari service. The service uses RSA keys stored in a Java KeyStore (JKS) file for signing and verifying JWTs.

## Configuration

The RSA key configuration is stored in `application.yaml`:

```yaml
security:
  jwt:
    key-store:
      path: classpath:keys/keystore.jks
      password: ${KEY_STORE_PASSWORD:changeit}
      key-alias: itibari-jwt-key
      private-key-passphrase: ${PRIVATE_KEY_PASSPHRASE:changeit}
```

## Environment Variables

For production environments, you should set these environment variables:

- `KEY_STORE_PASSWORD`: Password to access the keystore
- `PRIVATE_KEY_PASSPHRASE`: Password to access the private key

## Key Generation

The system automatically manages keys:

1. On first startup, if no keystore exists, it will:
   - Generate a new RSA key pair
   - Create a self-signed certificate
   - Store both in a new keystore file

2. On subsequent startups, it will:
   - Load the existing keys from the keystore
   - Use the same keys for JWT operations

## Key Rotation

To rotate keys:

1. Delete or rename the existing keystore file (`src/main/resources/keys/keystore.jks`)
2. Restart the application
3. The system will generate a new keystore with fresh keys

For a scheduled key rotation strategy:
- Implement a scheduled job that creates new keys
- Update all services to use the new keys
- Ensure a transition period where both old and new keys are valid

## Security Best Practices

1. **Environment Separation**: Use different keystores for development, testing, and production
2. **Access Control**: Restrict access to the keystore file and its passwords
3. **Backup**: Keep secure backups of the keystore
4. **Regular Rotation**: Rotate keys periodically (e.g., every 6 months)
5. **Monitoring**: Audit and log all key access and operations

## Troubleshooting

If you encounter issues with JWT authentication:

1. Verify the keystore file exists in the correct location
2. Check that environment variables are set correctly
3. Ensure the keystore and key passwords are correct
4. Examine logs for errors related to key loading or JWT operations

For persistent issues, you can force the generation of a new keystore by deleting the existing one and restarting the application.
