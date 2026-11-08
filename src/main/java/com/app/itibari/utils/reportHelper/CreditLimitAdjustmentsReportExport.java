package com.app.itibari.utils.reportHelper;

import com.app.itibari.models.response.BnplCreditLimitAdjustmentResponse;
import com.app.itibari.utils.helpers.ExcelUtils;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class CreditLimitAdjustmentsReportExport {
    private static final String TITLE = "Credit Limit Adjustments Report";
    private static final List<String> HEADERS = List.of(
            "Customer Name", "Customer Phone Number", "Adjustment Type", "Percentage",
            "Old Credit Limit", "New Credit Limit", "Old Credit Limit Balance", "New Credit Limit Balance",
            "Period Start", "Period End");
    private static final DateTimeFormatter PRINTED_ON = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final ExcelUtils excelUtils;

    public ByteArrayOutputStream generateExcel(List<BnplCreditLimitAdjustmentResponse> content, String name) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Credit Limit Adjustments");
            CellStyle headerStyle = excelUtils.getHeaderCellStyle(workbook, excelUtils.getHeaderFont(workbook));
            CellStyle textStyle = excelUtils.getStringDataCellStyleLeftAlign(workbook, excelUtils.getContentFont(workbook));
            CellStyle numericStyle = excelUtils.getNumericDataCellStyleRightAlign(workbook, excelUtils.getContentFont(workbook));
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.cloneStyleFrom(textStyle);
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));
            String[] metadata = {TITLE, "Printed By: " + name, "Printed On: " + PRINTED_ON.format(LocalDateTime.now())};
            for (int i = 0; i < metadata.length; i++) {
                excelUtils.createStringDataCell(sheet.createRow(i), 0, metadata[i], i == 0 ? headerStyle : textStyle);
                sheet.addMergedRegion(new CellRangeAddress(i, i, 0, HEADERS.size() - 1));
            }
            Row header = sheet.createRow(4);
            for (int i = 0; i < HEADERS.size(); i++) {
                excelUtils.createStringDataCell(header, i, HEADERS.get(i), headerStyle);
            }
            int rowIndex = 5;
            for (BnplCreditLimitAdjustmentResponse item : content) {
                Row row = sheet.createRow(rowIndex++);
                Object[] values = values(item);
                for (int i = 0; i < values.length; i++) {
                    Cell cell = row.createCell(i);
                    Object value = values[i];
                    cell.setCellStyle(textStyle);
                    if (value instanceof BigDecimal number) {
                        cell.setCellValue(number.doubleValue());
                        cell.setCellStyle(numericStyle);
                    } else if (value instanceof LocalDate date) {
                        cell.setCellValue(date.atStartOfDay());
                        cell.setCellStyle(dateStyle);
                    } else {
                        cell.setCellValue(text(value));
                    }
                }
            }
            sheet.createFreezePane(0, 5);
            sheet.setAutoFilter(new CellRangeAddress(4, Math.max(4, rowIndex - 1), 0, HEADERS.size() - 1));
            for (int i = 0; i < HEADERS.size(); i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.min(255 * 256, sheet.getColumnWidth(i) + 512));
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            return output;
        } catch (IOException e) {
            log.error("Error generating credit limit adjustments Excel report", e);
            throw new IllegalStateException("Error generating credit limit adjustments Excel report", e);
        }
    }

    public ByteArrayOutputStream generatePdf(List<BnplCreditLimitAdjustmentResponse> content, String name) {
        Document document = new Document(PageSize.A4.rotate(), 24f, 24f, 24f, 24f);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BaseColor green = new BaseColor(83, 112, 76);
        try {
            PdfWriter.getInstance(document, output);
            document.open();
            Paragraph company = new Paragraph("ITIBARI TECHNOLOGIES LTD", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, green));
            document.add(company);
            Paragraph title = new Paragraph(TITLE, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, green));
            title.setSpacingAfter(10f);
            document.add(title);
            document.add(new Paragraph("Printed By: " + name, FontFactory.getFont(FontFactory.HELVETICA, 9)));
            document.add(new Paragraph("Printed On: " + PRINTED_ON.format(LocalDateTime.now()), FontFactory.getFont(FontFactory.HELVETICA, 9)));
            PdfPTable table = new PdfPTable(HEADERS.size());
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 1.7f, 1.4f, 1f, 1.4f, 1.4f, 1.5f, 1.5f, 1.3f, 1.3f});
            table.setSpacingBefore(12f);
            table.setHeaderRows(1);
            table.setSplitLate(false);
            for (String heading : HEADERS) {
                PdfPCell cell = new PdfPCell(new Phrase(heading, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.WHITE)));
                cell.setBackgroundColor(green);
                cell.setPadding(5f);
                table.addCell(cell);
            }
            boolean shaded = false;
            for (BnplCreditLimitAdjustmentResponse item : content) {
                for (Object value : values(item)) {
                    PdfPCell cell = new PdfPCell(new Phrase(text(value), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    cell.setPadding(5f);
                    cell.setHorizontalAlignment(value instanceof BigDecimal ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                    cell.setBackgroundColor(shaded ? new BaseColor(248, 250, 248) : BaseColor.WHITE);
                    table.addCell(cell);
                }
                shaded = !shaded;
            }
            // Keep column headings visible even when there are no matching adjustments.
            if (content.isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("No credit limit adjustments found.", FontFactory.getFont(FontFactory.HELVETICA, 9)));
                empty.setColspan(HEADERS.size());
                empty.setPadding(8f);
                table.addCell(empty);
            }
            document.add(table);
        } catch (DocumentException e) {
            log.error("Error generating credit limit adjustments PDF report", e);
            throw new IllegalStateException("Error generating credit limit adjustments PDF report", e);
        } finally {
            document.close();
        }
        return output;
    }

    private Object[] values(BnplCreditLimitAdjustmentResponse item) {
        return new Object[]{item.getCustomerName(), item.getCustomerPhoneNumber(), item.getAdjustmentType(),
                item.getPercentage(), item.getOldCreditLimit(), item.getNewCreditLimit(),
                item.getOldCreditLimitBalance(), item.getNewCreditLimitBalance(), item.getPeriodStart(), item.getPeriodEnd()};
    }

    private String text(Object value) {
        return value == null ? "" : value instanceof BigDecimal number ? number.toPlainString() : value.toString();
    }
}
