package com.app.itibari.services.helper;

import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Stores blobs in GCS by default, or on the local filesystem when
 * {@code itibari.storage.local.enabled=true} (local profile).
 */
@Component
@Slf4j
public class ObjectStorageHelper {

    private static final String LOCAL_SCHEME = "local://";

    private final String bucketName;
    private final Resource credentials;
    private final boolean localEnabled;
    private final Path localRoot;

    public ObjectStorageHelper(
            @Value("${spring.cloud.gc.bucket.name}") String bucketName,
            @Value("${spring.cloud.gc.storage.credentials.location}") Resource credentials,
            @Value("${itibari.storage.local.enabled:false}") boolean localEnabled,
            @Value("${itibari.storage.local.path:./.local-storage}") String localPath) {
        this.bucketName = bucketName;
        this.credentials = credentials;
        this.localEnabled = localEnabled;
        this.localRoot = Paths.get(localPath).toAbsolutePath().normalize();
    }

    public boolean isLocalEnabled() {
        return localEnabled;
    }

    public String store(UUID publicId, String contentType, byte[] bytes) {
        if (localEnabled) {
            try {
                Files.createDirectories(localRoot);
                Path target = localRoot.resolve(publicId.toString());
                Files.write(target, bytes);
                log.debug("Stored local blob at {}", target);
                return LOCAL_SCHEME + publicId;
            } catch (IOException e) {
                log.error("Error while uploading file to local storage", e);
                throw new BadRequestRestApiException()
                        .developerMessage("Error while uploading file to local storage")
                        .userMessage("Sorry. We are unable to upload the file. Please try again later.");
            }
        }

        try {
            Storage storage = gcs();
            BlobId blobId = BlobId.of(bucketName, publicId.toString());
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();
            Blob blob = storage.create(blobInfo, bytes);
            return blob.getMediaLink();
        } catch (StorageException | IOException e) {
            log.error("Error while uploading file to storage", e);
            throw new BadRequestRestApiException()
                    .developerMessage("Error while uploading file to storage")
                    .userMessage("Sorry. We are unable to upload the file. Please try again later.");
        }
    }

    public Optional<byte[]> load(UUID publicId) {
        if (localEnabled) {
            Path target = localRoot.resolve(publicId.toString());
            if (!Files.exists(target)) {
                return Optional.empty();
            }
            try {
                return Optional.of(Files.readAllBytes(target));
            } catch (IOException e) {
                log.error("Error while reading local blob {}", publicId, e);
                return Optional.empty();
            }
        }

        try {
            Storage storage = gcs();
            Blob blob = storage.get(bucketName, publicId.toString());
            if (blob == null || !blob.exists()) {
                return Optional.empty();
            }
            return Optional.of(blob.getContent());
        } catch (IOException e) {
            log.error("Error while reading blob {}", publicId, e);
            return Optional.empty();
        }
    }

    public Optional<String> signedUrl(UUID publicId, long duration, TimeUnit unit) {
        if (localEnabled) {
            Path target = localRoot.resolve(publicId.toString());
            if (!Files.exists(target)) {
                return Optional.empty();
            }
            // Local downloads go through the existing authenticated download API.
            return Optional.of("/files/download/" + publicId);
        }

        try {
            Storage storage = gcs();
            Blob blob = storage.get(bucketName, publicId.toString());
            if (blob == null || !blob.exists()) {
                return Optional.empty();
            }
            String url = storage.signUrl(
                    BlobInfo.newBuilder(blob.getBlobId()).build(),
                    duration,
                    unit,
                    Storage.SignUrlOption.withV4Signature()
            ).toString();
            return Optional.of(url);
        } catch (IOException e) {
            log.error("Error while signing blob url {}", publicId, e);
            return Optional.empty();
        }
    }

    public void delete(UUID publicId) {
        if (localEnabled) {
            try {
                Files.deleteIfExists(localRoot.resolve(publicId.toString()));
            } catch (IOException e) {
                log.error("Error while deleting local blob {}", publicId, e);
                throw new BadRequestRestApiException()
                        .developerMessage("Error while deleting file from storage")
                        .userMessage("Sorry. We are unable to delete the file. Please try again later.");
            }
            return;
        }

        try {
            Storage storage = gcs();
            storage.delete(BlobId.of(bucketName, publicId.toString()));
        } catch (IOException e) {
            log.error("Error while deleting blob {}", publicId, e);
            throw new BadRequestRestApiException()
                    .developerMessage("Error while deleting file from storage")
                    .userMessage("Sorry. We are unable to delete the file. Please try again later.");
        }
    }

    private Storage gcs() throws IOException {
        return StorageOptions.newBuilder()
                .setCredentials(ServiceAccountCredentials.fromStream(credentials.getInputStream()))
                .build()
                .getService();
    }
}
