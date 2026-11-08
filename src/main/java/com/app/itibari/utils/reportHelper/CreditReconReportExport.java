package com.app.itibari.utils.reportHelper;


import com.app.itibari.models.entities.CreditRecon;
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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class CreditReconReportExport {
    private final ExcelUtils excelUtils;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");

    public ByteArrayOutputStream generateCreditReconExcel(List<CreditRecon> content, String name) {
        try {
            String documentTitle = "CREDIT RECONCILIATION REPORT";

            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet(documentTitle);

            org.apache.poi.ss.usermodel.Font headerFont = excelUtils.getHeaderFont(workbook);
            org.apache.poi.ss.usermodel.Font font = excelUtils.getContentFont(workbook);

            CellStyle headerCellStyle = excelUtils.getHeaderCellStyle(workbook, headerFont);
            CellStyle headerCellStyleCentreAlign = excelUtils.getStringDataCellStyleCentreAlign(workbook, headerFont);
            CellStyle stringDataCellStyleLeftAlign = excelUtils.getStringDataCellStyleLeftAlign(workbook, font);
            CellStyle numericDataCellStyleRightAlign = excelUtils.getNumericDataCellStyleRightAlign(workbook, font);
            CellStyle stringDataCellStyleCentreAlign = excelUtils.getStringDataCellStyleCentreAlign(workbook, font);

            int rowIndex = 1;

            // Title
            XSSFRow row1 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row1, 0, documentTitle, headerCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 12));

            // Printed By
            XSSFRow row3 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row3, 0, "Printed By: " + name, stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 12));

            // Printed On
            XSSFRow row4 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row4, 0, "Printed On: " + formatter.format(LocalDateTime.now()), stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 12));
            // Headers
            // customer, phone number, route, order number, order value, credit amount, interest rate , interest amount.
            List<String> contentHeaders = Arrays.asList(
                    "Order Date",
                    "Paid Date",
                    "Delivery Agent",
                    "Customer",
                    "Order Number",
                    "Credit Amount",
                    "Interest Rate",
                    "Interest Amount",
                    "Order Total Amount",
                    "Paid Amount",
                    "Status",
                    "Reconciled",
                    "Payment Reference"
            );

            Row headerRow = sheet.createRow(rowIndex += 2);
            for (int i = 0; i < contentHeaders.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(contentHeaders.get(i));
                cell.setCellStyle(headerCellStyle);
            }

            ++rowIndex;

            // Add parent + child data
            addRecordData(workbook, content, sheet, rowIndex, stringDataCellStyleLeftAlign, numericDataCellStyleRightAlign);

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

    public void addRecordData(XSSFWorkbook workbook,
                              List<CreditRecon> resp,
                              XSSFSheet sheet,
                              int rowIndex,
                              CellStyle dataCellStyle,
                              CellStyle dataCellStyleRightAlign) {

        CellStyle parentRowStyle = workbook.createCellStyle();
        parentRowStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());

        Font parentFont = workbook.createFont();
        parentFont.setBold(true);
        parentRowStyle.setFont(parentFont);

        for (CreditRecon creditRecon : resp) {

            XSSFRow parentRow = sheet.createRow(++rowIndex);
            int colIndex = -1;

            String orderCreatedDate = "--";
            if (creditRecon.getOrderCreationDateTime() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
                orderCreatedDate = creditRecon.getOrderCreationDateTime().format(formatter);
            }

            String paidDate = "--";
            if (creditRecon.getPaidDateTime() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
                paidDate = creditRecon.getPaidDateTime().format(formatter);
            }

            String paidStatus = creditRecon.getPaymentStatus() != null ? creditRecon.getPaymentStatus().name() : "--";
            String reconciled = creditRecon.isReconciled() ? "Settled" : "Outstanding";
            excelUtils.createStringDataCell(parentRow, ++colIndex, orderCreatedDate, parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, paidDate, parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, creditRecon.getDeliveryAgentName(), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, creditRecon.getCustomerName(), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, creditRecon.getOrderNumber(), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, formatMoney(creditRecon.getCreditAmount()), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, creditRecon.getInterestRate() == null ? "" : creditRecon.getInterestRate().toString(), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, formatMoney(creditRecon.getInterestAmount()), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, formatMoney(creditRecon.getOrderTotalAmount()), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, formatMoney(creditRecon.getPaidAmount()), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, paidStatus, parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, reconciled, parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, creditRecon.getPaymentReference(), parentRowStyle);

        }
    }

    public ByteArrayOutputStream generateCreditReconPdf(List<CreditRecon> content, String fullName) {
        Document document = new Document(PageSize.A4.rotate(), 18f, 18f, 24f, 24f);
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


            // title

            Paragraph title = new Paragraph("CREDIT RECONCILIATION REPORT",
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
            companyEmail.setSpacingAfter(8f);
            document.add(companyEmail);

            LineSeparator separator = new LineSeparator();
            separator.setLineColor(new BaseColor(190, 190, 190));
            document.add(new Chunk(separator));

            addItems(document, content);

            Paragraph printedBy = new Paragraph("Printed By: " + fullName,
                    FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK));
            printedBy.setAlignment(Element.ALIGN_LEFT);
            printedBy.setIndentationLeft(0f);
            printedBy.setSpacingBefore(8f);
            document.add(printedBy);

            Paragraph printedOn = new Paragraph("Printed On: " + formatter.format(LocalDateTime.now()),
                    FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK));
            printedOn.setAlignment(Element.ALIGN_LEFT);
            printedOn.setIndentationLeft(0f);
            document.add(printedOn);


            Paragraph sytemGenerated = new Paragraph(
                    """
                            This is a system generated document. No signature is required." 
                            "For any inquiries, please contact the company at commercial@itibari.io.
                            """,
                    FontFactory.getFont(FontFactory.TIMES_ITALIC, 6, BaseColor.DARK_GRAY));
            sytemGenerated.setAlignment(Element.ALIGN_CENTER);
            document.add(sytemGenerated);


            document.add(new Paragraph("\n"));


            document.close();

        } catch (Exception e) {   // catch broader to log everything
            log.error("Error generating receipt PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error generating receipt PDF", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }

        return out;
    }

    private void addItems(Document document, List<CreditRecon> content) throws DocumentException {
        com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.WHITE);
        com.itextpdf.text.Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

        PdfPTable lineItems = new PdfPTable(13);
        lineItems.setWidthPercentage(100);
        lineItems.setWidths(new float[]{1.8f, 1.8f, 1.8f, 1.8f, 1.4f, 1.0f, 1.4f, 1.2f, 1.2f, 1.0f, 1.2f, 1.2f, 1.2f});
        lineItems.setSpacingBefore(10f);
        lineItems.setSpacingAfter(6f);

        lineItems.addCell(createHeaderCell("Order Date", headerFont));
        lineItems.addCell(createHeaderCell("Paid Date", headerFont));
        lineItems.addCell(createHeaderCell("Delivery Agent", headerFont));
        lineItems.addCell(createHeaderCell("Customer", headerFont));
        lineItems.addCell(createHeaderCell("Order Number", headerFont));
        lineItems.addCell(createHeaderCell("Credit Amount", headerFont));
        lineItems.addCell(createHeaderCell("Interest Rate", headerFont));
        lineItems.addCell(createHeaderCell("Interest Amount", headerFont));
        lineItems.addCell(createHeaderCell("Order Total Amount", headerFont)); //added
        lineItems.addCell(createHeaderCell("Paid Amount", headerFont)); //added
        lineItems.addCell(createHeaderCell("Status", headerFont)); //added
        lineItems.addCell(createHeaderCell("Reconciled", headerFont));
        lineItems.addCell(createHeaderCell("Payment Reference", headerFont));

        boolean evenRow = false;
        for (CreditRecon item : content) {
            if (item == null) {
                continue;
            }

            String orderCreatedDate = "--";
            if (item.getOrderCreationDateTime() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
                orderCreatedDate = item.getOrderCreationDateTime().format(formatter);
            }

            String paidDate = "--";
            if (item.getPaidDateTime() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
                paidDate = item.getPaidDateTime().format(formatter);
            }
            String paidStatus = item.getPaymentStatus() != null ? item.getPaymentStatus().name() : "--";
            String reconciled = item.isReconciled() ? "Settled" : "Outstanding";

            BaseColor rowColor = evenRow ? new BaseColor(248, 250, 248) : BaseColor.WHITE;

            lineItems.addCell(createCell(orderCreatedDate, cellFont, Element.ALIGN_LEFT, rowColor));
            lineItems.addCell(createCell(paidDate, cellFont, Element.ALIGN_LEFT, rowColor));  //to be fetched
            lineItems.addCell(createCell(item.getDeliveryAgentName(), cellFont, Element.ALIGN_LEFT, rowColor));
            lineItems.addCell(createCell(item.getCustomerName(), cellFont, Element.ALIGN_LEFT, rowColor));
            lineItems.addCell(createCell(item.getOrderNumber(), cellFont, Element.ALIGN_LEFT, rowColor));
            lineItems.addCell(createCell(formatMoney(item.getCreditAmount()), cellFont, Element.ALIGN_RIGHT, rowColor));
            lineItems.addCell(createCell(item.getInterestRate() == null ? "" : item.getInterestRate().toString(), cellFont, Element.ALIGN_RIGHT, rowColor));
            lineItems.addCell(createCell(formatMoney(item.getInterestAmount()), cellFont, Element.ALIGN_RIGHT, rowColor));
            lineItems.addCell(createCell(formatMoney(item.getOrderTotalAmount()), cellFont, Element.ALIGN_RIGHT, rowColor));
            lineItems.addCell(createCell(formatMoney(item.getPaidAmount()), cellFont, Element.ALIGN_RIGHT, rowColor));
            lineItems.addCell(createCell(paidStatus, cellFont, Element.ALIGN_RIGHT, rowColor));
            lineItems.addCell(createCell(reconciled, cellFont, Element.ALIGN_LEFT, rowColor));
            lineItems.addCell(createCell(item.getPaymentReference(), cellFont, Element.ALIGN_CENTER, rowColor));

            evenRow = !evenRow;
        }

        document.add(Chunk.NEWLINE);
        document.add(lineItems);
    }


    private PdfPCell createHeaderCell(String text, com.itextpdf.text.Font font) {
        BaseColor receiptGreen = new BaseColor(83, 112, 76);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        cell.setBackgroundColor(receiptGreen);
        cell.setBorderColor(new BaseColor(230, 230, 230));
        cell.setBorderWidth(0.8f);
        return cell;
    }

    private PdfPCell createCell(String text, com.itextpdf.text.Font font, int alignment, BaseColor backgroundColor) {
        PdfPCell cell = new PdfPCell(new Phrase(safe(text), font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5f);
        cell.setMinimumHeight(18f);
        cell.setBackgroundColor(backgroundColor);
        cell.setBorderColor(new BaseColor(230, 230, 230));
        cell.setBorderWidth(0.6f);
        return cell;

    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String formatMoney(Number value) {
        if (value == null) {
            return "";
        }
        synchronized (MONEY_FORMAT) {
            return MONEY_FORMAT.format(value);
        }
    }
}
