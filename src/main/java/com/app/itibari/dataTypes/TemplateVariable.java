package com.app.itibari.dataTypes;

import lombok.Getter;

/**
 * Supported placeholder variables for reminder message templates.
 * Use the {@link #placeholder()} value inside double curly braces, e.g. {{customerName}}.
 */
@Getter
public enum TemplateVariable {

    CUSTOMER_NAME("customerName", "Full name of the customer"),
    INVOICE_NUMBER("invoiceNumber", "Invoice / order reference number"),
    DUE_DATE("dueDate", "Original due date of the invoice"),
    DAYS_OVERDUE("daysOverdue", "Number of days the invoice is overdue"),
    AMOUNT_DUE("amountDue", "Outstanding amount due"),
    COMPANY_NAME("companyName", "Your organisation name");

    /**
     * -- GETTER --
     * Returns the raw key as used in the template, e.g.
     * .
     */
    private final String key;
    private final String description;

    TemplateVariable(String key, String description) {
        this.key = key;
        this.description = description;
    }

    /** Returns the full placeholder token, e.g. {@code {{customerName}}}. */
    public String placeholder() {
        return "{{" + key + "}}";
    }

    /**
     * Replaces all occurrences of this variable's placeholder in the given template.
     *
     * @param template the raw message template
     * @param value    the runtime value to substitute
     * @return the template with this placeholder replaced
     */
    public String resolve(String template, String value) {
        if (template == null) return null;
        return template.replace(placeholder(), value != null ? value : "");
    }
}
