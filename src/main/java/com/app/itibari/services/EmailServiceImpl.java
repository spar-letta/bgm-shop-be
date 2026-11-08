package com.app.itibari.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;

    @Value("${notification.emails}")
    private String notificationEmails;

    @Value("system@itibari.io")
    private String fromAddress;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendPriceChangeNotificationEmail(String message) {

        log.info("=========Sending Price Change Notification Email=========");
        log.info("=====Message=========: {}", message);

        String subject = "Price Change Notification";

        List<String> emails = getEmails();
        if (emails.isEmpty()) {
            log.error("No emails found");
            return;
        }
        for (String email : emails) {
            sendEmailWithSMTP(fromAddress, email.trim(), subject, message);
            log.info("Email sent successfully to {}", email);
        }
    }

    private List<String> getEmails() {
        if (notificationEmails == null || notificationEmails.isEmpty() || notificationEmails.isBlank()) {
            log.warn("No notification emails configured in application properties.");
            return List.of();
        }

        return List.of(notificationEmails.split(","));
    }

    private void sendEmailWithSMTP(String from, String to, String subject, String body)  {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);


        try {
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("==Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    public void sendMessage(String recipient, String subject, File attachment, String htmlBody, List<String> ccRecipients) {
        log.info("recipient send email to {}", recipient);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromAddress);
            helper.setTo(recipient);
            helper.setSubject(subject);

            helper.setText(htmlBody, true);

            if (ccRecipients != null && !ccRecipients.isEmpty()) {
                helper.setCc(ccRecipients.toArray(new String[0]));
            }

            if (attachment != null) {
                helper.addAttachment(attachment.getName(), attachment);
            }

            mailSender.send(message);

        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", recipient, e.getMessage());
        }
    }

}
