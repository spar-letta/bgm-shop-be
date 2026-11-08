package com.app.itibari.utils.reportHelper;

import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.entities.ReconciliationStaging;
import com.app.itibari.models.entities.ReconciliationStagingItem;
import com.app.itibari.models.entities.SalesAgents;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.ExcelUtils;
import com.itextpdf.io.source.ByteArrayOutputStream;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SalesAgentCustomer {
    private final ExcelUtils excelUtils;

    public ByteArrayOutputStream createReport(List<SalesAgents> salesList, String name) {
        try {
            String documentTitle = "Sales Agent - Customer Report";

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
            excelUtils.createStringDataCell(row3, 0, "Printed By: " + name, stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 6));

            // Printed On
            XSSFRow row4 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row4, 0, "Printed On: " + LocalDateTime.now(), stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 6));

            // Headers
            List<String> contentHeaders = Arrays.asList(
                    "SA Name",
                    "Email",
                    "Phone Number",
                    "Verified",
                    "Customer Name",
                    "Tel No.",
                    "Email."
            );

            Row headerRow = sheet.createRow(rowIndex += 2);
            for (int i = 0; i < contentHeaders.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(contentHeaders.get(i));
                cell.setCellStyle(headerCellStyle);
            }

            ++rowIndex;

            // Add parent + child data
            addRecordData(workbook, salesList, sheet, rowIndex, stringDataCellStyleLeftAlign, numericDataCellStyleRightAlign);

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
                              List<SalesAgents> parents,
                              XSSFSheet sheet,
                              int rowIndex,
                              CellStyle dataCellStyle,
                              CellStyle dataCellStyleRightAlign) {

        CellStyle parentRowStyle = workbook.createCellStyle();
        parentRowStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        parentRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font parentFont = workbook.createFont();
        parentFont.setBold(true);
        parentRowStyle.setFont(parentFont);

        for (SalesAgents parent : parents) {

            XSSFRow parentRow = sheet.createRow(++rowIndex);
            int colIndex = -1;

            excelUtils.createStringDataCell(parentRow, ++colIndex, parent.getName(), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, parent.getEmail(), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, parent.getPhoneNumber(), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex,
                    parent.getIsVerified() != null ? parent.getIsVerified().toString():"--", parentRowStyle);

            for (Customers child : parent.getCustomers()) {
                XSSFRow childRow = sheet.createRow(++rowIndex);
                colIndex = 3;

                excelUtils.createStringDataCell(childRow, ++colIndex, child.getName(), dataCellStyleRightAlign);
                excelUtils.createStringDataCell(childRow, ++colIndex, child.getPhoneNumber(), dataCellStyleRightAlign);
                excelUtils.createStringDataCell(childRow, ++colIndex, child.getEmail(), dataCellStyleRightAlign);
            }
        }
    }
}
