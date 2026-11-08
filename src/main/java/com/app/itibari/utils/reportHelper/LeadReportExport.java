package com.app.itibari.utils.reportHelper;

import com.app.itibari.models.response.BnplLeadResponse;
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
@RequiredArgsConstructor
@Slf4j
public class LeadReportExport {
    private final ExcelUtils excelUtils;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0");


    public ByteArrayOutputStream generateLeadExcel(List<BnplLeadResponse> content, String fullName) {
        try {
            String documentTitle = "BNPL Leads Report";

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
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 6));

            // Printed By
            XSSFRow row3 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row3, 0, "Printed By: " + fullName, stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 6));

            // Printed On
            XSSFRow row4 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row4, 0, "Printed On: " + formatter.format(LocalDateTime.now()), stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 6));
            // Headers
            List<String> contentHeaders = Arrays.asList(
                    "Customer Name",
                    "Phone Number",
                    "Loan Eligibility Amount",
                    "Funnel Stage",
                    "Lead Status",
                    "Total Call Attempts",
                    "Scheduled Date",
                    "Credit Limit",
                    "Call Status",
                    "Outcome Status",
                    "Previous Order Date"
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

    private void addRecordData(XSSFWorkbook workbook, List<BnplLeadResponse> content, XSSFSheet sheet, int rowIndex, CellStyle stringDataCellStyleLeftAlign, CellStyle numericDataCellStyleRightAlign) {
        CellStyle parentRowStyle = workbook.createCellStyle();
        parentRowStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());

        Font parentFont = workbook.createFont();
        parentFont.setBold(true);
        parentRowStyle.setFont(parentFont);

        for (BnplLeadResponse bnplLeadResponse : content) {

            XSSFRow parentRow = sheet.createRow(++rowIndex);
            int colIndex = -1;

            excelUtils.createStringDataCell(parentRow, ++colIndex, bnplLeadResponse.getCustomerName(), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, bnplLeadResponse.getPhoneNumber(), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, bnplLeadResponse.getCreditLimit(), parentRowStyle);
            excelUtils.createNumericDataCell(parentRow, ++colIndex, bnplLeadResponse.getLoanEligibilityAmount(), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, bnplLeadResponse.getFunnelStage() != null ? bnplLeadResponse.getFunnelStage().name() : "", parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, bnplLeadResponse.getLeadStatus() != null ? bnplLeadResponse.getLeadStatus().name() : "", parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, bnplLeadResponse.getTotalCallAttempts() != null ? String.valueOf(bnplLeadResponse.getTotalCallAttempts()) : "0", parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, bnplLeadResponse.getScheduledDate(), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, bnplLeadResponse.getCallStatus() != null ? bnplLeadResponse.getCallStatus().description() : "", parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, bnplLeadResponse.getOutComeStatus() != null ? bnplLeadResponse.getOutComeStatus().description() : "", parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, bnplLeadResponse.getPreviousOrderDate() != null ? bnplLeadResponse.getPreviousOrderDate().toString() : "", parentRowStyle);


        }
    }

    public ByteArrayOutputStream generateLeadPdf(List<BnplLeadResponse> content, String fullName) {
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


            // title

            Paragraph title = new Paragraph("BNPL LEADS REPORT",
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
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, BaseColor.DARK_GRAY));
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

    private void addItems(Document document, List<BnplLeadResponse> content) throws DocumentException {

        com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.WHITE);
        com.itextpdf.text.Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

        PdfPTable lineItems = new PdfPTable(11);
        lineItems.setWidthPercentage(100);
        lineItems.setWidths(new float[]{2.6f, 2.2f, 1.8f, 1.7f, 1.7f, 1.7f, 1.7f, 1.7f, 1.7f, 1.7f, 1.7f});
        lineItems.setSpacingBefore(10f);
        lineItems.setSpacingAfter(6f);

        lineItems.addCell(createHeaderCell("Customer", headerFont));
        lineItems.addCell(createHeaderCell("Phone Number", headerFont));
        lineItems.addCell(createHeaderCell("Loan Eligibility Amount", headerFont));
        lineItems.addCell(createHeaderCell("Funnel Stage", headerFont));
        lineItems.addCell(createHeaderCell("Lead Status", headerFont));
        lineItems.addCell(createHeaderCell("Total Call Attempts", headerFont));
        lineItems.addCell(createHeaderCell("Scheduled Date", headerFont));
        lineItems.addCell(createHeaderCell("Credit Limit", headerFont));
        lineItems.addCell(createHeaderCell("Call Status", headerFont));
        lineItems.addCell(createHeaderCell("Outcome Status", headerFont));
        lineItems.addCell(createHeaderCell("Previous Order Date", headerFont));

        boolean evenRow = false;
        for (BnplLeadResponse item : content) {
            if (item == null) {
                continue;
            }
            BaseColor rowColor = evenRow ? new BaseColor(248, 250, 248) : BaseColor.WHITE;
            lineItems.addCell(createCell(item.getCustomerName(), cellFont, Element.ALIGN_LEFT, rowColor));
            lineItems.addCell(createCell(item.getPhoneNumber(), cellFont, Element.ALIGN_LEFT, rowColor));
            lineItems.addCell(createCell(item.getLoanEligibilityAmount() != null ? MONEY_FORMAT.format(item.getLoanEligibilityAmount()) : "", cellFont, Element.ALIGN_RIGHT, rowColor));
            lineItems.addCell(createCell(item.getFunnelStage() != null ? item.getFunnelStage().name() : "", cellFont, Element.ALIGN_LEFT, rowColor));
            lineItems.addCell(createCell(item.getLeadStatus() != null ? item.getLeadStatus().name() : "", cellFont, Element.ALIGN_LEFT, rowColor));
            lineItems.addCell(createCell(item.getTotalCallAttempts() != null ? String.valueOf(item.getTotalCallAttempts()) : "", cellFont, Element.ALIGN_RIGHT, rowColor));
            lineItems.addCell(createCell(item.getScheduledDate() != null ? item.getScheduledDate() : "", cellFont, Element.ALIGN_LEFT, rowColor));
            lineItems.addCell(createCell(item.getCreditLimit() != null ? MONEY_FORMAT.format(item.getCreditLimit()) : "", cellFont, Element.ALIGN_RIGHT, rowColor));
            lineItems.addCell(createCell(item.getCallStatus() != null ? item.getCallStatus().description() : "", cellFont, Element.ALIGN_LEFT, rowColor));
            lineItems.addCell(createCell(item.getOutComeStatus() != null ? item.getOutComeStatus().description() : "", cellFont, Element.ALIGN_LEFT, rowColor));
            lineItems.addCell(createCell(item.getPreviousOrderDate() != null ? item.getPreviousOrderDate().toString() : "", cellFont, Element.ALIGN_LEFT, rowColor));
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
}
