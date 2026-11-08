package com.app.itibari.utils.reportHelper;

import com.app.itibari.dataTypes.CashCreditReconLineStatus;
import com.app.itibari.models.response.CashCreditReconReportDTO;
import com.app.itibari.models.response.CashCreditReconReportLineDTO;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.ExcelUtils;
import com.app.itibari.utils.helpers.PdfUtil;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.text.*;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Component
@Slf4j
@RequiredArgsConstructor
public class CashCreditReconReportExport {

    private static final String DOCUMENT_TITLE = "CASH AND CREDIT RECONCILIATION REPORT";
    private static final DateTimeFormatter PRINTED_ON_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String ACCOUNTING_FORMAT = "#,##0;[Red](#,##0);-";
    private static final Color LIGHT_GREEN = new Color(215, 249, 232);
    private static final Color TOTALS_GREY = new Color(242, 242, 242);

    private final ExcelUtils excelUtils;

    public ByteArrayOutputStream generateCashCreditReconExcel(CashCreditReconReportDTO report, String fullName) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Cash Credit Recon");
            List<CashCreditReconReportLineDTO> lines = lines(report);

            CellStyle titleStyle = textStyle(workbook, true, 14, HorizontalAlignment.CENTER, null);
            CellStyle infoStyle = textStyle(workbook, false, 10, HorizontalAlignment.CENTER, null);
            CellStyle labelStyle = textStyle(workbook, true, 10, HorizontalAlignment.LEFT, Color.WHITE);
            CellStyle headerStyle = textStyle(workbook, true, 10, HorizontalAlignment.CENTER, LIGHT_GREEN);
            CellStyle totalsHeaderStyle = textStyle(workbook, true, 10, HorizontalAlignment.LEFT, TOTALS_GREY);
            CellStyle moneyStyle = moneyStyle(workbook, LIGHT_GREEN, false);
            CellStyle exceptionMoneyStyle = moneyStyle(workbook, Color.WHITE, false);
            CellStyle totalsMoneyStyle = moneyStyle(workbook, TOTALS_GREY, false);
            CellStyle boldMoneyStyle = moneyStyle(workbook, LIGHT_GREEN, true);
            CellStyle boldExceptionMoneyStyle = moneyStyle(workbook, Color.WHITE, true);
            CellStyle boldTotalsMoneyStyle = moneyStyle(workbook, TOTALS_GREY, true);

            int dataLastColumn = lines.size() + 1;
            int headerLastColumn = Math.max(dataLastColumn, 12);
            int rowIndex = 0;
            Row titleRow = sheet.createRow(rowIndex++);
            createTextCell(titleRow, 0, DOCUMENT_TITLE, titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, headerLastColumn));

            Row reportInfoRow = sheet.createRow(rowIndex++);
            createTextCell(reportInfoRow, 0,
                    "Report Date: " + report.getReportDate() + "    Status: " + report.getStatus(), infoStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, headerLastColumn));

            Row printInfoRow = sheet.createRow(rowIndex++);
            createTextCell(printInfoRow, 0,
                    "Printed By: " + safe(fullName) + "    Printed On: " + PRINTED_ON_FORMAT.format(LocalDateTime.now()),
                    infoStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 2, 0, headerLastColumn));

            rowIndex++;
            Row headerRow = sheet.createRow(rowIndex++);
            headerRow.setHeightInPoints(48);
            createTextCell(headerRow, 0, "", labelStyle);
            createTextCell(headerRow, 1, "TOTALS", totalsHeaderStyle);
            for (int i = 0; i < lines.size(); i++) {
                CashCreditReconReportLineDTO line = lines.get(i);
                createTextCell(headerRow, i + 2, agentHeader(line),
                        isBalanced(line) ? headerStyle : textStyle(workbook, true, 10,
                                HorizontalAlignment.CENTER, Color.WHITE));
            }

            for (Metric metric : metrics()) {
                Row row = sheet.createRow(rowIndex++);
                createTextCell(row, 0, metric.label(), labelStyle);
                createNumberCell(row, 1, metric.total().apply(report),
                        metric.bold() ? boldTotalsMoneyStyle : totalsMoneyStyle);
                for (int i = 0; i < lines.size(); i++) {
                    CashCreditReconReportLineDTO line = lines.get(i);
                    CellStyle lineStyle = isBalanced(line)
                            ? (metric.bold() ? boldMoneyStyle : moneyStyle)
                            : (metric.bold() ? boldExceptionMoneyStyle : exceptionMoneyStyle);
                    createNumberCell(row, i + 2, metric.line().apply(line), lineStyle);
                }
            }

            Row statusRow = sheet.createRow(rowIndex);
            createTextCell(statusRow, 0, "STATUS", labelStyle);
            createStatusCell(statusRow, 1, overallStatus(lines), workbook, TOTALS_GREY);
            for (int i = 0; i < lines.size(); i++) {
                CashCreditReconLineStatus status = lines.get(i).getStatus();
                createStatusCell(statusRow, i + 2, status == null ? "" : status.name(), workbook,
                        isBalanced(lines.get(i)) ? LIGHT_GREEN : Color.WHITE);
            }

            sheet.setColumnWidth(0, 24 * 256);
            sheet.setColumnWidth(1, 15 * 256);
            for (int i = 2; i <= dataLastColumn; i++) {
                sheet.setColumnWidth(i, 15 * 256);
            }
            sheet.createFreezePane(2, 5);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            return output;
        } catch (IOException e) {
            log.error("Error generating cash and credit reconciliation Excel report", e);
            throw exportError();
        }
    }

    public ByteArrayOutputStream generateCashCreditReconPdf(CashCreditReconReportDTO report, String fullName) {
        List<CashCreditReconReportLineDTO> lines = lines(report);
        Document document = new Document(PageSize.A3.rotate(), 18f, 18f, 24f, 24f);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new PdfUtil());
            document.open();
            addPdfBranding(document, writer);

            Paragraph title = new Paragraph(DOCUMENT_TITLE,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new BaseColor(83, 112, 76)));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph details = new Paragraph(
                    "Report Date: " + report.getReportDate() + "    Status: " + report.getStatus(),
                    FontFactory.getFont(FontFactory.HELVETICA, 9));
            details.setAlignment(Element.ALIGN_CENTER);
            details.setSpacingAfter(8f);
            document.add(details);

            addPdfMatrix(document, report, lines);

            Paragraph printed = new Paragraph(
                    "Printed By: " + safe(fullName) + "    Printed On: " + PRINTED_ON_FORMAT.format(LocalDateTime.now()),
                    FontFactory.getFont(FontFactory.HELVETICA, 8));
            printed.setSpacingBefore(8f);
            document.add(printed);
        } catch (Exception e) {
            log.error("Error generating cash and credit reconciliation PDF report", e);
            throw exportError();
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return output;
    }

    private void addPdfMatrix(Document document, CashCreditReconReportDTO report,
                              List<CashCreditReconReportLineDTO> lines) throws Exception {
        int columns = lines.size() + 2;
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        float[] widths = new float[columns];
        widths[0] = 2.3f;
        widths[1] = 1.4f;
        for (int i = 2; i < columns; i++) {
            widths[i] = 1.35f;
        }
        table.setWidths(widths);
        table.setHeaderRows(1);

        float fontSize = lines.size() > 18 ? 4.5f : lines.size() > 12 ? 5.5f : 7f;
        com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, fontSize);
        com.itextpdf.text.Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, fontSize);
        com.itextpdf.text.Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, fontSize);

        table.addCell(pdfCell("", labelFont, Element.ALIGN_LEFT, BaseColor.WHITE));
        table.addCell(pdfCell("TOTALS", headerFont, Element.ALIGN_CENTER, pdfColor(TOTALS_GREY)));
        for (CashCreditReconReportLineDTO line : lines) {
            table.addCell(pdfCell(agentHeader(line), headerFont, Element.ALIGN_CENTER, lineBackground(line)));
        }

        for (Metric metric : metrics()) {
            table.addCell(pdfCell(metric.label(), labelFont, Element.ALIGN_LEFT, BaseColor.WHITE));
            table.addCell(pdfMoneyCell(metric.total().apply(report), metric.bold(), pdfColor(TOTALS_GREY), fontSize));
            for (CashCreditReconReportLineDTO line : lines) {
                table.addCell(pdfMoneyCell(metric.line().apply(line), metric.bold(), lineBackground(line), fontSize));
            }
        }

        table.addCell(pdfCell("STATUS", labelFont, Element.ALIGN_LEFT, BaseColor.WHITE));
        table.addCell(pdfStatusCell(overallStatus(lines), pdfColor(TOTALS_GREY), fontSize));
        for (CashCreditReconReportLineDTO line : lines) {
            table.addCell(pdfStatusCell(line.getStatus() == null ? "" : line.getStatus().name(),
                    lineBackground(line), fontSize));
        }
        document.add(table);
    }

    private void addPdfBranding(Document document, PdfWriter writer) throws Exception {
        Image banner = Image.getInstance(new ClassPathResource("static/images/logo_2.png").getInputStream().readAllBytes());
        banner.scaleToFit(900, 100);
        banner.setAbsolutePosition(-22, document.getPageSize().getHeight() - banner.getScaledHeight() - 8);
        writer.getDirectContent().addImage(banner);
        document.add(new Paragraph("\n\n"));

        Image logo = Image.getInstance(new ClassPathResource("static/images/logoImage_1.jpg").getInputStream().readAllBytes());
        logo.scaleToFit(90, 70);
        logo.setAlignment(Image.ALIGN_LEFT);
        document.add(logo);
        document.add(Chunk.NEWLINE);
    }

    private List<Metric> metrics() {
        return List.of(
                new Metric("GMV Delivered", CashCreditReconReportDTO::getTotalGmvDelivered,
                        CashCreditReconReportLineDTO::getGmvDelivered, true),
                new Metric("Discounts", CashCreditReconReportDTO::getTotalDiscounts,
                        CashCreditReconReportLineDTO::getDiscounts, false),
                new Metric("Complementaries", CashCreditReconReportDTO::getTotalComplementaries,
                        CashCreditReconReportLineDTO::getComplementaries, false),
                new Metric("NET GMV", CashCreditReconReportDTO::getTotalNetGmv,
                        CashCreditReconReportLineDTO::getNetGmv, true),
                new Metric("Loading and Offloading", CashCreditReconReportDTO::getTotalLoadingOffloading,
                        CashCreditReconReportLineDTO::getLoadingOffloading, false),
                new Metric("Credit Orders", CashCreditReconReportDTO::getTotalCreditOrders,
                        CashCreditReconReportLineDTO::getCreditOrders, false),
                new Metric("Parking Fee", CashCreditReconReportDTO::getTotalParkingFee,
                        CashCreditReconReportLineDTO::getParkingFee, false),
                new Metric("Ferry Costs", CashCreditReconReportDTO::getTotalFerryCost,
                        CashCreditReconReportLineDTO::getFerryCost, false),
                new Metric("Deposited Amount", CashCreditReconReportDTO::getTotalDepositedAmount,
                        CashCreditReconReportLineDTO::getDepositedAmount, false),
                new Metric("DIFFERENCE", CashCreditReconReportDTO::getTotalDifference,
                        CashCreditReconReportLineDTO::getDifference, true),
                new Metric("Outstanding as per Meta", CashCreditReconReportDTO::getTotalMetaOutstanding,
                        CashCreditReconReportLineDTO::getMetaOutstanding, true));
    }

    private CellStyle textStyle(XSSFWorkbook workbook, boolean bold, int size,
                                HorizontalAlignment alignment, Color background) {
        XSSFCellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(bold);
        font.setFontHeightInPoints((short) size);
        style.setFont(font);
        style.setAlignment(alignment);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        applyBorders(style);
        applyFill(style, background);
        return style;
    }

    private CellStyle moneyStyle(XSSFWorkbook workbook, Color background, boolean bold) {
        XSSFCellStyle style = (XSSFCellStyle) textStyle(workbook, bold, 10, HorizontalAlignment.RIGHT, background);
        style.setDataFormat(workbook.createDataFormat().getFormat(ACCOUNTING_FORMAT));
        return style;
    }

    private void createStatusCell(Row row, int column, String status, XSSFWorkbook workbook, Color background) {
        XSSFCellStyle style = (XSSFCellStyle) textStyle(workbook, true, 10, HorizontalAlignment.CENTER, background);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(statusColorIndex(status));
        style.setFont(font);
        createTextCell(row, column, status, style);
    }

    private short statusColorIndex(String status) {
        if ("BALANCED".equals(status)) {
            return IndexedColors.GREEN.getIndex();
        }
        if ("OVER".equals(status)) {
            return IndexedColors.BLUE.getIndex();
        }
        return IndexedColors.RED.getIndex();
    }

    private void createTextCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(safe(value));
        cell.setCellStyle(style);
    }

    private void createNumberCell(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(zero(value).doubleValue());
        cell.setCellStyle(style);
    }

    private void applyBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    }

    private void applyFill(XSSFCellStyle style, Color background) {
        if (background != null) {
            style.setFillForegroundColor(new XSSFColor(background, null));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
    }

    private PdfPCell pdfMoneyCell(BigDecimal amount, boolean bold, BaseColor background, float fontSize) {
        boolean negative = zero(amount).signum() < 0;
        com.itextpdf.text.Font font = FontFactory.getFont(
                bold ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA,
                fontSize, negative ? BaseColor.RED : BaseColor.BLACK);
        return pdfCell(formatMoney(amount), font, Element.ALIGN_RIGHT, background);
    }

    private PdfPCell pdfStatusCell(String status, BaseColor background, float fontSize) {
        BaseColor color = "BALANCED".equals(status) ? new BaseColor(0, 150, 0)
                : "OVER".equals(status) ? BaseColor.BLUE : BaseColor.RED;
        return pdfCell(status, FontFactory.getFont(FontFactory.HELVETICA_BOLD, fontSize, color),
                Element.ALIGN_CENTER, background);
    }

    private PdfPCell pdfCell(String text, com.itextpdf.text.Font font, int alignment, BaseColor background) {
        PdfPCell cell = new PdfPCell(new Phrase(safe(text), font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3f);
        cell.setBackgroundColor(background);
        cell.setBorderColor(new BaseColor(190, 190, 190));
        cell.setBorderWidth(0.5f);
        return cell;
    }

    private String formatMoney(BigDecimal value) {
        BigDecimal amount = zero(value);
        if (amount.signum() == 0) {
            return "-";
        }
        String formatted = String.format("%,.0f", amount.abs());
        return amount.signum() < 0 ? "(" + formatted + ")" : formatted;
    }

    private String overallStatus(List<CashCreditReconReportLineDTO> lines) {
        if (lines.stream().anyMatch(line -> line.getStatus() == CashCreditReconLineStatus.UNDER)) {
            return CashCreditReconLineStatus.UNDER.name();
        }
        if (lines.stream().anyMatch(line -> line.getStatus() == CashCreditReconLineStatus.OVER)) {
            return CashCreditReconLineStatus.OVER.name();
        }
        return CashCreditReconLineStatus.BALANCED.name();
    }

    private String agentHeader(CashCreditReconReportLineDTO line) {
        String name = safe(line.getDeliveryAgentName());
        return name.isBlank() ? safe(line.getRouteId()) : name;
    }

    private boolean isBalanced(CashCreditReconReportLineDTO line) {
        return line.getStatus() == CashCreditReconLineStatus.BALANCED;
    }

    private BaseColor lineBackground(CashCreditReconReportLineDTO line) {
        return isBalanced(line) ? pdfColor(LIGHT_GREEN) : BaseColor.WHITE;
    }

    private List<CashCreditReconReportLineDTO> lines(CashCreditReconReportDTO report) {
        return report == null || report.getLines() == null ? Collections.emptyList() : report.getLines();
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private BaseColor pdfColor(Color color) {
        return new BaseColor(color.getRed(), color.getGreen(), color.getBlue());
    }

    private BadRequestRestApiException exportError() {
        throw new BadRequestRestApiException()
                .developerMessage("Error generating cash and credit reconciliation report")
                .userMessage("Sorry. Error generating cash and credit reconciliation report");
    }

    private record Metric(String label,
                          Function<CashCreditReconReportDTO, BigDecimal> total,
                          Function<CashCreditReconReportLineDTO, BigDecimal> line,
                          boolean bold) {
    }
}
