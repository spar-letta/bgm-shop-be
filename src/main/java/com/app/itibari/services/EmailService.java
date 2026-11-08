package com.app.itibari.services;

import java.io.File;
import java.util.List;

public interface EmailService {
    void sendPriceChangeNotificationEmail(String message);
    void sendMessage(String recipient, String subject, File attachment, String htmlBody, List<String> ccRecipients);
}
