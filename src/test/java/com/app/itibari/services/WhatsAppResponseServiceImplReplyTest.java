package com.app.itibari.services;

import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.WhatsAppResponse;
import com.app.itibari.models.request.WhatsAppOutboundMedia;
import com.app.itibari.models.request.WhatsAppRequest;
import com.app.itibari.repository.CustomersRepository;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.WhatsAppResponseRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.TumaMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WhatsAppResponseServiceImplReplyTest {

    @Mock private WhatsAppResponseRepository whatsAppResponseRepository;
    @Mock private CustomersRepository customersRepository;
    @Mock private TumaMessage tumaMessage;
    @Mock private PersonRepository personRepository;
    @Mock private FileService fileService;

    @InjectMocks
    private WhatsAppResponseServiceImpl service;

    private Person agent;
    private WhatsAppRequest request;

    @Before
    public void setUp() {
        agent = new Person();
        agent.setFirstName("CX");
        agent.setLastName("Agent");
        agent.setEmail("cx@example.com");
        when(personRepository.findByEmailAndDeletedIsFalse("cx@example.com")).thenReturn(Optional.of(agent));

        request = WhatsAppRequest.builder()
                .customerPhone("+255111111111")
                .messageBody("Asante")
                .build();
    }

    @Test
    public void inboundCustomerMessageIsSavedWithPlus() {
        when(customersRepository.findByPhoneNumberLastNine("672903554")).thenReturn(List.of());
        when(whatsAppResponseRepository.save(any(WhatsAppResponse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> body = Map.of(
                "entry", List.of(Map.of(
                        "changes", List.of(Map.of(
                                "value", Map.of(
                                        "messages", List.of(Map.of(
                                                "from", "255672903554",
                                                "id", "wamid.in.1",
                                                "type", "text",
                                                "text", Map.of("body", "habari")
                                        ))
                                )
                        ))
                ))
        );

        service.runPersistData(body);

        ArgumentCaptor<WhatsAppResponse> captor = ArgumentCaptor.forClass(WhatsAppResponse.class);
        verify(whatsAppResponseRepository).save(captor.capture());
        assertEquals("+255672903554", captor.getValue().getPhoneNumber());
        assertEquals("Direct", captor.getValue().getSourceType());
    }

    @Test
    public void replyFindsInboundStoredWithoutPlus() throws Exception {
        WhatsAppResponse latest = new WhatsAppResponse();
        latest.setMessageId("wamid.123");
        when(whatsAppResponseRepository.findTopByPhoneNumberIgnoreCaseOrderByCreationTimeDesc("+255111111111"))
                .thenReturn(Optional.empty());
        when(whatsAppResponseRepository.findTopByPhoneNumberContainingIgnoreCaseOrderByCreationTimeDesc("111111111"))
                .thenReturn(Optional.of(latest));

        service.updateCustomerCallStatus(request, null, "cx@example.com");

        verify(tumaMessage).replyMessage(eq(Optional.of(agent)), eq(request), eq("wamid.123"), isNull());
    }

    @Test
    public void textReplySendsWithoutMedia() throws Exception {
        WhatsAppResponse latest = new WhatsAppResponse();
        latest.setMessageId("wamid.123");
        when(whatsAppResponseRepository.findTopByPhoneNumberIgnoreCaseOrderByCreationTimeDesc("+255111111111"))
                .thenReturn(Optional.of(latest));

        service.updateCustomerCallStatus(request, null, "cx@example.com");

        verify(tumaMessage).replyMessage(eq(Optional.of(agent)), eq(request), eq("wamid.123"), isNull());
    }

    @Test
    public void imageReplyIsSentAsWhatsAppImage() throws Exception {
        WhatsAppResponse latest = new WhatsAppResponse();
        latest.setMessageId("wamid.123");
        when(whatsAppResponseRepository.findTopByPhoneNumberIgnoreCaseOrderByCreationTimeDesc("+255111111111"))
                .thenReturn(Optional.of(latest));

        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
        service.updateCustomerCallStatus(request, file, "cx@example.com");

        ArgumentCaptor<WhatsAppOutboundMedia> mediaCaptor = ArgumentCaptor.forClass(WhatsAppOutboundMedia.class);
        verify(tumaMessage).replyMessage(eq(Optional.of(agent)), eq(request), eq("wamid.123"), mediaCaptor.capture());
        WhatsAppOutboundMedia media = mediaCaptor.getValue();
        assertEquals("image", media.messageType());
        assertEquals("image/jpeg", media.mimeType());
        assertEquals("photo.jpg", media.fileName());
        assertEquals(3, media.content().length);
    }

    @Test
    public void pdfReplyIsSentAsWhatsAppDocument() throws Exception {
        WhatsAppResponse latest = new WhatsAppResponse();
        latest.setMessageId("wamid.123");
        when(whatsAppResponseRepository.findTopByPhoneNumberIgnoreCaseOrderByCreationTimeDesc("+255111111111"))
                .thenReturn(Optional.of(latest));

        WhatsAppRequest fileOnly = WhatsAppRequest.builder()
                .customerPhone("+255111111111")
                .messageBody("")
                .build();
        MockMultipartFile file = new MockMultipartFile("file", "invoice.pdf", "application/pdf", new byte[]{9});
        service.updateCustomerCallStatus(fileOnly, file, "cx@example.com");

        ArgumentCaptor<WhatsAppOutboundMedia> mediaCaptor = ArgumentCaptor.forClass(WhatsAppOutboundMedia.class);
        verify(tumaMessage).replyMessage(eq(Optional.of(agent)), eq(fileOnly), eq("wamid.123"), mediaCaptor.capture());
        assertEquals("document", mediaCaptor.getValue().messageType());
        assertEquals("application/pdf", mediaCaptor.getValue().mimeType());
    }

    @Test
    public void textSendToNewNumberUsesTemplate() throws Exception {
        service.sendMessage(request, null, "cx@example.com");

        verify(tumaMessage).sendTemplateMessage(eq(Optional.of(agent)), eq(request));
        verify(tumaMessage, never()).sendMessage(any(), any(), any());
        verify(tumaMessage, never()).replyMessage(any(), any(), any(), any());
    }

    @Test
    public void textSendUsesSessionMessageWhenCustomerWroteInLast24Hours() throws Exception {
        when(whatsAppResponseRepository
                .existsByPhoneNumberContainingIgnoreCaseAndSourceTypeIgnoreCaseAndCreationTimeGreaterThanEqual(
                        eq("111111111"), eq("Direct"), any()))
                .thenReturn(true);

        service.sendMessage(request, null, "cx@example.com");

        verify(tumaMessage).sendMessage(eq(Optional.of(agent)), eq(request), isNull());
        verify(tumaMessage, never()).sendTemplateMessage(any(), any());
    }

    @Test
    public void fileSendToNewNumberIsRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        BadRequestRestApiException ex = assertThrows(BadRequestRestApiException.class,
                () -> service.sendMessage(request, file, "cx@example.com"));
        assertEquals(
                "WhatsApp cannot send a file to a number that has not messaged us. Send text first; after they reply you can attach a file.",
                ex.getRestApiError().getUserMessage());
        verify(tumaMessage, never()).sendMessage(any(), any(), any());
        verify(tumaMessage, never()).sendTemplateMessage(any(), any());
    }

    @Test
    public void fileSendUploadsMediaWhenCustomerWroteInLast24Hours() throws Exception {
        when(whatsAppResponseRepository
                .existsByPhoneNumberContainingIgnoreCaseAndSourceTypeIgnoreCaseAndCreationTimeGreaterThanEqual(
                        eq("111111111"), eq("Direct"), any()))
                .thenReturn(true);
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
        service.sendMessage(request, file, "cx@example.com");

        ArgumentCaptor<WhatsAppOutboundMedia> mediaCaptor = ArgumentCaptor.forClass(WhatsAppOutboundMedia.class);
        verify(tumaMessage).sendMessage(eq(Optional.of(agent)), eq(request), mediaCaptor.capture());
        assertEquals("image", mediaCaptor.getValue().messageType());
    }

    @Test
    public void emptyNamedFileIsRejectedInsteadOfSendingTextOnly() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "bigger_log.png", "image/png", new byte[0]);

        BadRequestRestApiException ex = assertThrows(BadRequestRestApiException.class,
                () -> service.sendMessage(request, emptyFile, "cx@example.com"));
        verify(tumaMessage, never()).sendMessage(any(), any(), any());
        assertEquals("The file was empty. Re-select it in Postman and remove any Content-Type: application/json header.",
                ex.getRestApiError().getUserMessage());
    }

    @Test
    public void emptySendToNewNumberIsRejected() throws Exception {
        WhatsAppRequest empty = WhatsAppRequest.builder()
                .customerPhone("+255111111111")
                .messageBody("  ")
                .build();

        BadRequestRestApiException ex = assertThrows(BadRequestRestApiException.class,
                () -> service.sendMessage(empty, null, "cx@example.com"));
        assertEquals("Please enter a message", ex.getRestApiError().getUserMessage());
        verify(tumaMessage, never()).sendTemplateMessage(any(), any());
        verify(tumaMessage, never()).sendMessage(any(), any(), any());
    }

    @Test
    public void emptySendWithoutFileIsRejectedWhenSessionIsOpen() throws Exception {
        when(whatsAppResponseRepository
                .existsByPhoneNumberContainingIgnoreCaseAndSourceTypeIgnoreCaseAndCreationTimeGreaterThanEqual(
                        eq("111111111"), eq("Direct"), any()))
                .thenReturn(true);
        WhatsAppRequest empty = WhatsAppRequest.builder()
                .customerPhone("+255111111111")
                .messageBody("  ")
                .build();

        assertThrows(BadRequestRestApiException.class,
                () -> service.sendMessage(empty, null, "cx@example.com"));
        verify(tumaMessage, never()).sendMessage(any(), any(), any());
        verify(tumaMessage, never()).sendTemplateMessage(any(), any());
    }

    @Test
    public void emptyReplyWithoutFileIsRejected() throws Exception {
        WhatsAppRequest empty = WhatsAppRequest.builder()
                .customerPhone("+255111111111")
                .messageBody("  ")
                .build();

        assertThrows(BadRequestRestApiException.class,
                () -> service.updateCustomerCallStatus(empty, null, "cx@example.com"));
        verify(tumaMessage, never()).replyMessage(any(), any(), any(), any());
    }

    @Test
    public void oversizedImageIsRejectedBeforeSend() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(5L * 1024 * 1024 + 1);
        when(file.getContentType()).thenReturn("image/jpeg");

        assertThrows(BadRequestRestApiException.class,
                () -> service.updateCustomerCallStatus(request, file, "cx@example.com"));
        verify(tumaMessage, never()).replyMessage(any(), any(), any(), any());
    }

    @Test
    public void saveReplyMessagePersistsOutboundMedia() {
        when(customersRepository.findByPhoneNumberLastNine("111111111")).thenReturn(List.of());
        when(whatsAppResponseRepository.save(any(WhatsAppResponse.class))).thenAnswer(invocation -> {
            WhatsAppResponse saved = invocation.getArgument(0);
            if (saved.getPublicId() == null) {
                saved.setPublicId(UUID.randomUUID());
            }
            return saved;
        });

        WhatsAppOutboundMedia media = new WhatsAppOutboundMedia(
                "image", "image/jpeg", new byte[]{4, 5}, "photo.jpg");
        String apiBody = "{\"messages\":[{\"id\":\"wamid.out.1\"}]}";

        service.saveReplyMessage(apiBody, request, Optional.of(agent), media);

        ArgumentCaptor<WhatsAppResponse> savedCaptor = ArgumentCaptor.forClass(WhatsAppResponse.class);
        verify(whatsAppResponseRepository).save(savedCaptor.capture());
        WhatsAppResponse saved = savedCaptor.getValue();
        assertEquals("image", saved.getMessageType());
        assertEquals("Asante", saved.getMessage());
        assertEquals("image/jpeg", saved.getMediaMimeType());
        verify(fileService).uploadWhatsAppMedia(eq(media.content()), eq("image/jpeg"), eq("photo.jpg"), eq(saved.getPublicId()));
    }
}
