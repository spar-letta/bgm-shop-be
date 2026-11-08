package com.app.itibari.models.request;

public record WhatsAppOutboundMedia(String messageType, String mimeType, byte[] content, String fileName) {
}
