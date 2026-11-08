package com.app.itibari.utils.reportHelper;

import com.app.itibari.models.response.WalletResponseDTO;
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
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class WalletReportExport {
    private final ExcelUtils excelUtils;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");

    public ByteArrayOutputStream generateWalletExcel(List<WalletResponseDTO> content, String name) {
        try {
            String documentTitle = "BNPL Wallet Report";

            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet(documentTitle);

            org.apache.poi.ss.usermodel.Font headerFont = excelUtils.getHeaderFont(workbook);
            org.apache.poi.ss.usermodel.Font font = excelUtils.getContentFont(workbook);

            CellStyle headerCellStyle = excelUtils.getHeaderCellStyle(workbook, headerFont);
            CellStyle headerCellStyleCentreAlign = excelUtils.getStringDataCellStyleCentreAlign(workbook, headerFont);
            CellStyle stringDataCellStyleCentreAlign = excelUtils.getStringDataCellStyleCentreAlign(workbook, font);

            int rowIndex = 1;

            XSSFRow row1 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row1, 0, documentTitle, headerCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 12));

            XSSFRow row3 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row3, 0, "Printed By: " + name, stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 12));

            XSSFRow row4 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row4, 0, "Printed On: " + formatter.format(LocalDateTime.now()), stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 12));

            List<String> contentHeaders = Arrays.asList(
                    "Customer",
                    "Customer Type",
                    "Region",
                    "Zone",
                    "Onboarding Date",
                    "Credit Status",
                    "Available Balance",
                    "Credit Limit",
                    "Total Credit Used",
                    "Total Payments",
                    "Unpaid Amount",
                    "Wallet Balance",
                    "Total Credit Taken"
            );

            Row headerRow = sheet.createRow(rowIndex += 2);
            for (int i = 0; i < contentHeaders.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(contentHeaders.get(i));
                cell.setCellStyle(headerCellStyle);
            }

            CellStyle parentRowStyle = workbook.createCellStyle();
            parentRowStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            Font parentFont = workbook.createFont();
            parentFont.setBold(true);
            parentRowStyle.setFont(parentFont);

            for (WalletResponseDTO wallet : content) {
                XSSFRow row = sheet.createRow(++rowIndex);
                int col = -1;
                excelUtils.createStringDataCell(row, ++col, wallet.getCustomerName(), parentRowStyle);
                excelUtils.createStringDataCell(row, ++col, String.valueOf(wallet.getCustomerType()), parentRowStyle);
                excelUtils.createStringDataCell(row, ++col, wallet.getRegion(), parentRowStyle);
                excelUtils.createStringDataCell(row, ++col, wallet.getZone(), parentRowStyle);
                excelUtils.createStringDataCell(row, ++col, String.valueOf(wallet.getOnBoardingDate()), parentRowStyle);
                excelUtils.createStringDataCell(row, ++col, String.valueOf(wallet.getCreditStatus()), parentRowStyle);
                excelUtils.createStringDataCell(row, ++col, String.valueOf(wallet.getAvailableBalance()), parentRowStyle);
                excelUtils.createStringDataCell(row, ++col, String.valueOf(wallet.getCreditLimit()), parentRowStyle);
                excelUtils.createStringDataCell(row, ++col, String.valueOf(wallet.getTotalCreditUsed()), parentRowStyle);
                excelUtils.createStringDataCell(row, ++col, String.valueOf(wallet.getTotalPaymentsMade()), parentRowStyle);
                excelUtils.createStringDataCell(row, ++col, String.valueOf(wallet.getUnpaidAmount()), parentRowStyle);
                excelUtils.createStringDataCell(row, ++col, String.valueOf(wallet.getWalletBalance()), parentRowStyle);
                excelUtils.createStringDataCell(row, ++col, String.valueOf(wallet.getTotalCreditTaken()), parentRowStyle);
            }

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

    public ByteArrayOutputStream generateWalletPdf(List<WalletResponseDTO> content, String fullName) {
        Document document = new Document(PageSize.A4.rotate(), 18f, 18f, 24f, 24f);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BaseColor receiptGreen = new BaseColor(83, 112, 76);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PdfUtil());
            document.open();

            Image logo2 = Image.getInstance(new ClassPathResource("static/images/logo_2.png").getInputStream().readAllBytes());
            logo2.scaleToFit(630, 200);
            float y = document.getPageSize().getHeight() - document.topMargin() - logo2.getScaledHeight() + 50f;
            logo2.setAbsolutePosition(-22, y);
            writer.getDirectContent().addImage(logo2);

            document.add(new Paragraph("\n\n\n"));

            Image logo = Image.getInstance(new ClassPathResource("static/images/logoImage_1.jpg").getInputStream().readAllBytes());
            logo.scaleToFit(120, 120);
            logo.setAlignment(Image.ALIGN_LEFT);
            logo.setIndentationLeft(-10f);
            document.add(logo);

            Paragraph title = new Paragraph("BNPL WALLET REPORT",
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
            printedBy.setSpacingBefore(8f);
            document.add(printedBy);

            Paragraph printedOn = new Paragraph("Printed On: " + formatter.format(LocalDateTime.now()),
                    FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK));
            printedOn.setAlignment(Element.ALIGN_LEFT);
            document.add(printedOn);

            document.close();
        } catch (Exception e) {
            log.error("Error generating wallet report PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error generating wallet report PDF", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }

        return out;
    }

    private void addItems(Document document, List<WalletResponseDTO> content) throws DocumentException {
        com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.WHITE);
        com.itextpdf.text.Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

        PdfPTable lineItems = new PdfPTable(13);
        lineItems.setWidthPercentage(100);
        lineItems.setWidths(new float[]{1.8f, 1.2f, 1.3f, 1.3f, 1.3f, 1.3f, 1.6f, 1.6f, 1.6f, 1.6f, 1.6f, 1.6f, 1.6f});
        lineItems.setSpacingBefore(10f);
        lineItems.setSpacingAfter(6f);

        lineItems.addCell(createHeaderCell("Customer", headerFont));
        lineItems.addCell(createHeaderCell("Type", headerFont));
        lineItems.addCell(createHeaderCell("Region", headerFont));
        lineItems.addCell(createHeaderCell("Zone", headerFont));
        lineItems.addCell(createHeaderCell("Onboarding Date", headerFont));
        lineItems.addCell(createHeaderCell("Credit Status", headerFont));
        lineItems.addCell(createHeaderCell("Available Balance", headerFont));
        lineItems.addCell(createHeaderCell("Credit Limit", headerFont));
        lineItems.addCell(createHeaderCell("Total Credit Used", headerFont));
        lineItems.addCell(createHeaderCell("Total Payments", headerFont));
        lineItems.addCell(createHeaderCell("Unpaid Amount", headerFont));
        lineItems.addCell(createHeaderCell("Wallet Balance", headerFont));
        lineItems.addCell(createHeaderCell("Total Credit Taken", headerFont));

        boolean evenRow = false;
        for (WalletResponseDTO item : content) {
            if (item == null) {
                continue;
            }
            BaseColor rowColor = evenRow ? new BaseColor(248, 250, 248) : BaseColor.WHITE;
            lineItems.addCell(createCell(item.getCustomerName(), cellFont, Element.ALIGN_LEFT, rowColor));
            lineItems.addCell(createCell(String.valueOf(item.getCustomerType()), cellFont, Element.ALIGN_LEFT, rowColor));
            lineItems.addCell(createCell(item.getRegion(), cellFont, Element.ALIGN_LEFT, rowColor));
            lineItems.addCell(createCell(item.getZone(), cellFont, Element.ALIGN_LEFT, rowColor));
            lineItems.addCell(createCell(String.valueOf(item.getOnBoardingDate()), cellFont, Element.ALIGN_CENTER, rowColor));
            lineItems.addCell(createCell(String.valueOf(item.getCreditStatus()), cellFont, Element.ALIGN_LEFT, rowColor));
            lineItems.addCell(createCell(formatStringAmount(item.getAvailableBalance()), cellFont, Element.ALIGN_RIGHT, rowColor));
            lineItems.addCell(createCell(formatStringAmount(item.getCreditLimit()), cellFont, Element.ALIGN_RIGHT, rowColor));
            lineItems.addCell(createCell(formatStringAmount(item.getTotalCreditUsed()), cellFont, Element.ALIGN_RIGHT, rowColor));
            lineItems.addCell(createCell(formatStringAmount(item.getTotalPaymentsMade()), cellFont, Element.ALIGN_RIGHT, rowColor));
            lineItems.addCell(createCell(formatStringAmount(item.getUnpaidAmount()), cellFont, Element.ALIGN_RIGHT, rowColor));
            lineItems.addCell(createCell(formatStringAmount(item.getWalletBalance()), cellFont, Element.ALIGN_RIGHT, rowColor));
            lineItems.addCell(createCell(formatStringAmount(item.getTotalCreditTaken()), cellFont, Element.ALIGN_RIGHT, rowColor));
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

    private String formatStringAmount(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        synchronized (MONEY_FORMAT) {
            return MONEY_FORMAT.format(amount);
        }
    }
}
