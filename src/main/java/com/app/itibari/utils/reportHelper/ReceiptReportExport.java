package com.app.itibari.utils.reportHelper;

import com.app.itibari.models.entities.FileData;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.ExcelUtils;
import com.app.itibari.utils.helpers.PdfUtil;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReceiptReportExport {

    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,##0.##");
    private static final ZoneId EAT = ZoneId.of("Africa/Nairobi");
    private final ExcelUtils excelUtils;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ByteArrayOutputStream generateExcel(List<FileData> content, String fullName,
                                               String deliveryAgent, String deliveryRoute) {
        try {
            String documentTitle = "Receipts Report";
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet(documentTitle);

            Font headerFont = excelUtils.getHeaderFont(workbook);
            Font font = excelUtils.getContentFont(workbook);
            CellStyle headerCellStyle = excelUtils.getHeaderCellStyle(workbook, headerFont);
            CellStyle headerCellStyleCentreAlign = excelUtils.getStringDataCellStyleCentreAlign(workbook, headerFont);
            CellStyle stringDataCellStyleCentreAlign = excelUtils.getStringDataCellStyleCentreAlign(workbook, font);
            CellStyle stringDataCellStyleLeftAlign = excelUtils.getStringDataCellStyleLeftAlign(workbook, font);
            CellStyle numericDataCellStyleRightAlign = excelUtils.getNumericDataCellStyleRightAlign(workbook, font);

            int rowIndex = 1;
            XSSFRow titleRow = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(titleRow, 0, documentTitle, headerCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 3));

            XSSFRow printedByRow = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(printedByRow, 0, "Printed By: " + fullName, stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 3));

            XSSFRow printedOnRow = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(printedOnRow, 0, "Printed On: " + formatter.format(LocalDateTime.now(EAT)), stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 3));

            XSSFRow agentRow = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(agentRow, 0, "Delivery Agent: " + emptyIfNull(deliveryAgent), stringDataCellStyleLeftAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 3));

            XSSFRow routeRow = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(routeRow, 0, "Delivery Route: " + emptyIfNull(deliveryRoute), stringDataCellStyleLeftAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 3));

            List<String> contentHeaders = Arrays.asList("No.", "Payment Code", "Amount", "Uploaded From");
            Row headerRow = sheet.createRow(rowIndex += 2);
            for (int i = 0; i < contentHeaders.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(contentHeaders.get(i));
                cell.setCellStyle(headerCellStyle);
            }

            int rowNumber = 0;
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (FileData item : content) {
                if (item == null) {
                    continue;
                }
                XSSFRow dataRow = sheet.createRow(++rowIndex);
                int colIndex = -1;
                excelUtils.createStringDataCell(dataRow, ++colIndex, String.valueOf(++rowNumber), stringDataCellStyleLeftAlign);
                excelUtils.createStringDataCell(dataRow, ++colIndex, paymentCode(item), stringDataCellStyleLeftAlign);
                BigDecimal amount = amountValue(item);
                totalAmount = totalAmount.add(amount);
                excelUtils.createStringDataCell(dataRow, ++colIndex, formatAmount(amount), numericDataCellStyleRightAlign);
                excelUtils.createStringDataCell(dataRow, ++colIndex, uploadedFrom(item), stringDataCellStyleLeftAlign);
            }

            XSSFRow totalRow = sheet.createRow(rowIndex += 2);
            excelUtils.createStringDataCell(totalRow, 0, "Total Amount", headerCellStyle);
            excelUtils.createStringDataCell(totalRow, 2, formatAmount(totalAmount), numericDataCellStyleRightAlign);

            for (int i = 0; i < contentHeaders.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            bos.close();
            return bos;
        } catch (IOException e) {
            throw new BadRequestRestApiException()
                    .developerMessage("Error in create file")
                    .userMessage("Sorry. Error in create file");
        }
    }

    public ByteArrayOutputStream generatePdf(List<FileData> content, String fullName,
                                             String deliveryAgent, String deliveryRoute) {
        Document document = new Document(PageSize.A4, 24f, 24f, 24f, 24f);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BaseColor receiptGreen = new BaseColor(83, 112, 76);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PdfUtil());
            document.open();

            Image logo_2 = Image.getInstance(new ClassPathResource("static/images/logo_2.png").getInputStream().readAllBytes());
            logo_2.scaleToFit(630, 200);
            float y = document.getPageSize().getHeight()
                    - document.topMargin()
                    - logo_2.getScaledHeight()
                    + 50f;
            logo_2.setAbsolutePosition(-22, y);
            writer.getDirectContent().addImage(logo_2);

            document.add(new Paragraph("\n\n\n"));

            Image logo = Image.getInstance(new ClassPathResource("static/images/logoImage_1.jpg").getInputStream().readAllBytes());
            logo.scaleToFit(120, 120);
            logo.setAlignment(Image.ALIGN_LEFT);
            logo.setIndentationLeft(-10f);
            document.add(logo);

            Paragraph title = new Paragraph("RECEIPTS REPORT",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, receiptGreen));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(6f);
            document.add(title);

            Paragraph companyName = new Paragraph("ITIBARI TECHNOLOGIES LTD",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, receiptGreen));
            companyName.setAlignment(Element.ALIGN_LEFT);
            companyName.setSpacingBefore(8f);
            companyName.setSpacingAfter(2f);
            document.add(companyName);

            Paragraph companyAddress = new Paragraph("Mikocheni light industrial area, Dar es Salaam",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, receiptGreen));
            companyAddress.setAlignment(Element.ALIGN_LEFT);
            companyAddress.setSpacingAfter(2f);
            document.add(companyAddress);

            Paragraph companyEmail = new Paragraph("Email: commercial@itibari.io",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, receiptGreen));
            companyEmail.setAlignment(Element.ALIGN_LEFT);
            companyEmail.setSpacingAfter(2f);
            document.add(companyEmail);

            Paragraph agentLine = new Paragraph("Delivery Agent: " + emptyIfNull(deliveryAgent),
                    FontFactory.getFont(FontFactory.HELVETICA, 10, receiptGreen));
            agentLine.setAlignment(Element.ALIGN_LEFT);
            agentLine.setSpacingAfter(2f);
            document.add(agentLine);

            Paragraph routeLine = new Paragraph("Delivery Route: " + emptyIfNull(deliveryRoute),
                    FontFactory.getFont(FontFactory.HELVETICA, 10, receiptGreen));
            routeLine.setAlignment(Element.ALIGN_LEFT);
            routeLine.setSpacingAfter(8f);
            document.add(routeLine);

            LineSeparator separator = new LineSeparator();
            separator.setLineColor(new BaseColor(190, 190, 190));
            document.add(new Chunk(separator));

            BigDecimal totalAmount = addItems(document, content);

            Paragraph total = new Paragraph("Total Amount: " + formatAmount(totalAmount),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.BLACK));
            total.setSpacingBefore(8f);
            document.add(total);

            Paragraph printedBy = new Paragraph("Printed By: " + fullName,
                    FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK));
            printedBy.setAlignment(Element.ALIGN_LEFT);
            printedBy.setSpacingBefore(8f);
            document.add(printedBy);

            Paragraph printedOn = new Paragraph("Printed On: " + formatter.format(LocalDateTime.now(EAT)),
                    FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK));
            printedOn.setAlignment(Element.ALIGN_LEFT);
            document.add(printedOn);

            document.close();
        } catch (Exception e) {
            log.error("Error generating receipts PDF: {}", e.getMessage(), e);
            throw new BadRequestRestApiException()
                    .developerMessage("Error generating receipts PDF")
                    .userMessage("Sorry. Error generating receipts PDF");
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return out;
    }

    private BigDecimal addItems(Document document, List<FileData> content) throws DocumentException {
        com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.WHITE);
        com.itextpdf.text.Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

        PdfPTable lineItems = new PdfPTable(4);
        lineItems.setWidthPercentage(100);
        lineItems.setWidths(new float[]{0.8f, 3.0f, 1.8f, 2.6f});
        lineItems.setSpacingBefore(10f);
        lineItems.setSpacingAfter(6f);

        lineItems.addCell(createHeaderCell("No.", headerFont));
        lineItems.addCell(createHeaderCell("Payment Code", headerFont));
        lineItems.addCell(createHeaderCell("Amount", headerFont));
        lineItems.addCell(createHeaderCell("Uploaded From", headerFont));

        boolean evenRow = false;
        int rowNumber = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (FileData item : content) {
            if (item == null) {
                continue;
            }
            BaseColor rowColor = evenRow ? new BaseColor(248, 250, 248) : BaseColor.WHITE;
            BigDecimal amount = amountValue(item);
            totalAmount = totalAmount.add(amount);
            lineItems.addCell(createCell(String.valueOf(++rowNumber), cellFont, Element.ALIGN_CENTER, rowColor));
            lineItems.addCell(createCell(paymentCode(item), cellFont, Element.ALIGN_LEFT, rowColor));
            lineItems.addCell(createCell(formatAmount(amount), cellFont, Element.ALIGN_RIGHT, rowColor));
            lineItems.addCell(createCell(uploadedFrom(item), cellFont, Element.ALIGN_LEFT, rowColor));
            evenRow = !evenRow;
        }

        document.add(Chunk.NEWLINE);
        document.add(lineItems);
        return totalAmount;
    }

    private PdfPCell createHeaderCell(String text, com.itextpdf.text.Font font) {
        BaseColor receiptGreen = new BaseColor(83, 112, 76);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(7f);
        cell.setBackgroundColor(receiptGreen);
        cell.setBorderColor(new BaseColor(230, 230, 230));
        cell.setBorderWidth(0.8f);
        return cell;
    }

    private PdfPCell createCell(String text, com.itextpdf.text.Font font, int alignment, BaseColor backgroundColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        cell.setMinimumHeight(20f);
        cell.setBackgroundColor(backgroundColor);
        cell.setBorderColor(new BaseColor(230, 230, 230));
        cell.setBorderWidth(0.6f);
        return cell;
    }

    /**
     * Matches cash recon Receipts table parsing: {@code paymentCode#amount#source}.
     */
    static String paymentCode(FileData item) {
        String[] parts = descriptionParts(item.getDescription());
        if (parts.length > 0 && !parts[0].isBlank()) {
            return parts[0].trim();
        }
        return emptyIfNull(item.getReceipt());
    }

    static BigDecimal amountValue(FileData item) {
        String[] parts = descriptionParts(item.getDescription());
        if (parts.length > 1) {
            String raw = parts[1].replace(",", "").trim();
            try {
                return new BigDecimal(raw);
            } catch (NumberFormatException ignored) {
                // fall through to entity amount
            }
        }
        return item.getAmount() == null ? BigDecimal.ZERO : item.getAmount();
    }

    static String uploadedFrom(FileData item) {
        String[] parts = descriptionParts(item.getDescription());
        if (parts.length > 2 && "accountsUpload".equals(parts[2].trim())) {
            return "accounts office";
        }
        return "DA app description";
    }

    private static String[] descriptionParts(String description) {
        if (description == null || description.isBlank()) {
            return new String[0];
        }
        return description.split("#", -1);
    }

    private static String formatAmount(BigDecimal amount) {
        return AMOUNT_FORMAT.format(amount == null ? BigDecimal.ZERO : amount);
    }

    private static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}
