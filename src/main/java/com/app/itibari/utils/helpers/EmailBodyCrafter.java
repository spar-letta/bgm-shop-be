package com.app.itibari.utils.helpers;

import org.springframework.stereotype.Component;

@Component
public class EmailBodyCrafter {

    public String craftDepartmentEmail(String message, String name) {

        return String.format("""
                <html>
                    <body>
                        <h2>Hello %s,</h2>
                        <p><b>%s</b></p>
                        <p>Regards.</p>
                    </body>
                </html>
                """, name, message);

    }

    public String craftSecondDepartmentEmail(String message) {

        return String.format("""
                <html>
                    <body>
                        <h2>Hello Sir/Madam,</h2>
                        <p><b>%s</b></p>
                        <p>Regards.</p>
                    </body>
                </html>
                """, message);
    }

    public String craftDocumentExpiryEmail(String supplierName, String documentName, String days) {
        String messageBody = String.format("""
            We hope this finds you well.<br>
            Please be informed %s (document) will expire in %s days.<br>
            Kindly share updated %s.<br><br>
            Best regards,<br>
            Itibari Commercial Team.
            """, documentName, days, documentName);

        return String.format("""
            <html>
                <body>
                    <h2>Dear %s,</h2>
                    <p>%s</p>
                </body>
            </html>
            """, supplierName, messageBody);
    }

    public String craftFollowUpEmail(String officerName, String dateString, String customerName, String customerTel) {
        String messageBody = String.format("""
            We hope this finds you well.<br>
            Kindly adhere to this reminder: %s, you have a scheduled follow-up task with customer %s, reachable at telephone number %s.
            Best regards,<br>
            Itibari Tech Team.
            """, dateString, customerName, customerTel);

        return String.format("""
            <html>
                <body>
                    <h2>Dear %s,</h2>
                    <p>%s</p>
                </body>
            </html>
            """, officerName, messageBody);
    }

    public String craftDepartment2Email(String name, String message) {

        return String.format("""
                <html>
                    <body>
                        <h2>Dear %s,</h2>
                        <p><b>%s</b></p>
                    </body>
                </html>
                """, name, message);

    }

}
