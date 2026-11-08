package com.app.itibari.services;

import com.app.itibari.dataTypes.ReferenceType;
import com.app.itibari.models.entities.FileData;
import com.app.itibari.repository.FileDataRepository;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FileServiceImplTest {
    @Mock private FileDataRepository fileDataRepository;
    @Mock private PersonRepository personRepository;
    @Mock private ReconciliationService reconciliationService;
    @InjectMocks private FileServiceImpl fileService;

    @Test
    public void uploadDebitNoteWithAmountAppliesPaymentToReferencedNote() throws Exception {
        UUID noteId = UUID.randomUUID();
        FileData result = upload(noteId, "40.00");
        assertEquals(noteId.toString(), result.getReferenceId());
        assertEquals(new BigDecimal("40.00"), result.getAmount());
        verify(reconciliationService).updateDebitNote(noteId, new BigDecimal("40.00"), "admin@example.com");
    }

    @Test
    public void uploadDebitNoteWithoutAmountOnlyAttachesDocument() throws Exception {
        FileData result = upload(UUID.randomUUID(), null);
        assertEquals(BigDecimal.ZERO, result.getAmount());
        verifyNoInteractions(reconciliationService);
    }

    @Test
    public void invalidPaymentIsRejectedBeforeFileIsStored() {
        assertThrows(BadRequestRestApiException.class, () -> fileService.uploadFile(
                new MockMultipartFile("file", new byte[]{1}), UUID.randomUUID(),
                ReferenceType.DELIVERY_AGENT_DEBIT_NOTE, null, null, "invalid", null,
                null, true, "admin@example.com", null));
        verifyNoInteractions(fileDataRepository, reconciliationService);
    }

    private FileData upload(UUID noteId, String amount) throws Exception {
        Resource resource = mock(Resource.class);
        var stream = new ByteArrayInputStream(new byte[0]);
        when(resource.getInputStream()).thenReturn(stream);
        ReflectionTestUtils.setField(fileService, "resource", resource);
        ReflectionTestUtils.setField(fileService, "bucketName", "test-bucket");
        when(personRepository.findByEmailAndDeletedIsFalse("admin@example.com")).thenReturn(Optional.empty());
        when(fileDataRepository.save(any(FileData.class))).thenAnswer(invocation -> {
            FileData data = invocation.getArgument(0);
            if (data.getPublicId() == null) data.setPublicId(UUID.randomUUID());
            return data;
        });
        ServiceAccountCredentials credentials = mock(ServiceAccountCredentials.class);
        StorageOptions.Builder builder = mock(StorageOptions.Builder.class);
        StorageOptions options = mock(StorageOptions.class);
        Storage storage = mock(Storage.class);
        Blob blob = mock(Blob.class);
        when(builder.setCredentials(credentials)).thenReturn(builder);
        when(builder.build()).thenReturn(options);
        when(options.getService()).thenReturn(storage);
        when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(blob);
        when(blob.getMediaLink()).thenReturn("https://storage.example/test");
        try (var credentialsMock = mockStatic(ServiceAccountCredentials.class);
             var optionsMock = mockStatic(StorageOptions.class)) {
            credentialsMock.when(() -> ServiceAccountCredentials.fromStream(stream)).thenReturn(credentials);
            optionsMock.when(StorageOptions::newBuilder).thenReturn(builder);
            return fileService.uploadFile(new MockMultipartFile("file", "receipt.txt", "text/plain", new byte[]{1}),
                    noteId, ReferenceType.DELIVERY_AGENT_DEBIT_NOTE, null, null, amount, null,
                    null, true, "admin@example.com", null);
        }
    }
}
