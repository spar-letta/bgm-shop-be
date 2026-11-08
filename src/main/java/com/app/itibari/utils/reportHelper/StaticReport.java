package com.app.itibari.utils.reportHelper;

import com.app.itibari.models.entities.ReconciliationStaging;
import com.app.itibari.models.entities.ReconciliationStagingItem;
import com.app.itibari.models.response.LiveStockValue;
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
public class StaticReport {

    private final ExcelUtils excelUtils;


    public ByteArrayOutputStream createReport(List<LiveStockValue> liveStockValueList, String name) {
        try {
            String documentTitle = "Static Stock Report";

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
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 15));

            // Printed By
            XSSFRow row3 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row3, 0, "Printed By: " + name, stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 15));

            // Printed On
            XSSFRow row4 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row4, 0, "Printed On: " + LocalDateTime.now(), stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 15));

            // Headers
            List<String> contentHeaders = Arrays.asList(
                            "Product Name",
                            "Opening Stock",
                            "Issued Stock",
                            "Expected Stock After Issuance",
                            "Goods Received",
                            "Goods Returned",
                            "Returns From Trade",
                            "Replacements",
                            "Returned Quarantine Stock",
                            "Expected Closing Stock",
                            "Quarantine Stock",
                            "Total Stock",
                            "Sold Stock",
                            "Stock Adjustment",
                            "Negative Adjusted Stock",
                            "Closing Stock"
            );

            Row headerRow = sheet.createRow(rowIndex += 2);
            for (int i = 0; i < contentHeaders.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(contentHeaders.get(i));
                cell.setCellStyle(headerCellStyle);
            }

            ++rowIndex;

            // Add parent + child data
            addRecordData(workbook, liveStockValueList, sheet, rowIndex, stringDataCellStyleLeftAlign, numericDataCellStyleRightAlign);

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
                              List<LiveStockValue> liveStockValueList,
                              XSSFSheet sheet,
                              int rowIndex,
                              CellStyle dataCellStyle,
                              CellStyle dataCellStyleRightAlign) {

        CellStyle parentRowStyle = workbook.createCellStyle();
        parentRowStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
//        parentRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font parentFont = workbook.createFont();
        parentFont.setBold(true);
        parentRowStyle.setFont(parentFont);

        for (LiveStockValue liveStockValue : liveStockValueList) {

            XSSFRow parentRow = sheet.createRow(++rowIndex);
            int colIndex = -1;

            excelUtils.createStringDataCell(parentRow, ++colIndex, liveStockValue.getProductName().toString(), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, String.valueOf(liveStockValue.getOpeningStock()), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, String.valueOf(liveStockValue.getIssuedStock()),parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, String.valueOf(liveStockValue.getExpectedStockAfterIssuance()),parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, String.valueOf(liveStockValue.getGoodsReceivedNote()),parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, String.valueOf(liveStockValue.getGoodsReturnedNote()),parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, String.valueOf(liveStockValue.getReturnsFromTrade()),parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, String.valueOf(liveStockValue.getReplacements()), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, String.valueOf(liveStockValue.getReturnedQuarantineStock()), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, String.valueOf(liveStockValue.getExpectedClosingStock()), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, String.valueOf(liveStockValue.getQuarantineStock()), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, String.valueOf(liveStockValue.getTotalStock()), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, String.valueOf(liveStockValue.getSoldStock()), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, String.valueOf(liveStockValue.getStockAdjustment()), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, String.valueOf(liveStockValue.getNegativeAdjustedStock()), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, String.valueOf(liveStockValue.getClosingStock()), parentRowStyle);
        }
    }
}
