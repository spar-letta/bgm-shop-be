package com.app.itibari.services.helper;

import com.app.itibari.utils.helpers.WebhookClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Posts the nightly {@link StockHealthReport} to the Stock Health Google Chat
 * space as a cardsV2 summary card: layer-drift summary, worst drift offenders,
 * and low-stock products with supplier contacts for immediate purchase orders.
 *
 * Routed via {@link WebhookClient#acceptAndSend(String, String)} — same pattern
 * as TRA Errors and other department webhooks.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockHealthNotifier {

    private static final String STOCK_HEALTH_CHANNEL = "Stock Health";

    private final WebhookClient webhookClient;
    private final ObjectMapper objectMapper;

    public void send(StockHealthReport report) {
        try {
            webhookClient.acceptAndSend(STOCK_HEALTH_CHANNEL, buildCard(report).toString());
            log.info("STOCK-HEALTH Google Chat card sent for {}", report.date());
        } catch (Exception e) {
            log.error("STOCK-HEALTH Google Chat card failed for {}", report.date(), e);
        }
    }

    private ObjectNode buildCard(StockHealthReport report) {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode cardWrapper = root.putArray("cardsV2").addObject();
        cardWrapper.put("cardId", "stock-health-" + report.date());
        ObjectNode card = cardWrapper.putObject("card");

        ObjectNode header = card.putObject("header");
        header.put("title", "Stock Health Report — " + report.date());
        header.put("subtitle", "Nightly layer-drift and low-stock check (04:30 EAT)");

        ArrayNode sections = card.putArray("sections");
        addSummarySection(sections, report);

        if (report.drifted() > 0) {
            addDriftSection(sections, report);
        }
        if (!report.lowStock().isEmpty()) {
            addLowStockSection(sections, report);
        }
        if (report.drifted() == 0 && report.lowStockCount() == 0) {
            ObjectNode section = sections.addObject();
            section.putArray("widgets").addObject()
                    .putObject("textParagraph")
                    .put("text", "All clear: no layer drift and no low-stock products detected.");
        }
        return root;
    }

    private void addSummarySection(ArrayNode sections, StockHealthReport report) {
        ObjectNode section = sections.addObject();
        section.put("header", "Summary");
        ArrayNode widgets = section.putArray("widgets");
        addDecoratedText(widgets, "Products checked", String.valueOf(report.checked()), null);
        addDecoratedText(widgets, "Layer drift", report.drifted() + " product(s) with disagreeing stock layers", null);
        addDecoratedText(widgets, "Low stock (≤ reorder point)", report.lowStockCount() + " product(s)", null);
        addDecoratedText(widgets, "Missing rows",
                report.missingSnapshot() + " snapshot / " + report.missingLive() + " live-stock", null);
    }

    private void addDriftSection(ArrayNode sections, StockHealthReport report) {
        ObjectNode section = sections.addObject();
        section.put("header", "Top drift offenders (showing " + report.topDrifts().size()
                + " of " + report.drifted() + ")");
        ArrayNode widgets = section.putArray("widgets");
        for (StockHealthReport.DriftEntry entry : report.topDrifts()) {
            addDecoratedText(widgets,
                    "SKU " + safe(entry.sku()) + " • " + safe(entry.warehouse()),
                    safe(entry.productName()),
                    "Operational " + entry.operational()
                            + " • Snapshot " + orDash(entry.snapshot())
                            + " • Live closing " + orDash(entry.liveClosing()));
        }
    }

    private void addLowStockSection(ArrayNode sections, StockHealthReport report) {
        ObjectNode section = sections.addObject();
        section.put("header", "Low stock — raise purchase orders (showing "
                + report.lowStock().size() + " of " + report.lowStockCount() + ")");
        ArrayNode widgets = section.putArray("widgets");
        for (StockHealthReport.LowStockEntry entry : report.lowStock()) {
            addDecoratedText(widgets,
                    "SKU " + safe(entry.sku()) + " • " + safe(entry.warehouse()),
                    safe(entry.productName()) + " — " + entry.available() + " remaining",
                    "Supplier(s): " + safe(entry.suppliers()));
        }
    }

    private void addDecoratedText(ArrayNode widgets, String topLabel, String text, String bottomLabel) {
        ObjectNode decorated = widgets.addObject().putObject("decoratedText");
        decorated.put("topLabel", topLabel);
        decorated.put("text", text);
        decorated.put("wrapText", true);
        if (bottomLabel != null) {
            decorated.put("bottomLabel", bottomLabel);
        }
    }

    private String orDash(Number value) {
        return value == null ? "—" : value.toString();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
