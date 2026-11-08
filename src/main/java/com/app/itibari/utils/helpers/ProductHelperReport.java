package com.app.itibari.utils.helpers;

import com.app.itibari.models.entities.Product;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.itextpdf.io.source.ByteArrayOutputStream;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
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
public class ProductHelperReport {

    private final ExcelUtils excelUtils;

    public ByteArrayOutputStream createProductsReportExcel(List<Product> content, String name) {

        try {

            String documentTitle = "Products";

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

            XSSFRow row1 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row1, 0, documentTitle, headerCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 20));

            XSSFRow row3 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row3,
                                            0,
                                            "Printed By: \t \t     " + name,
                                            stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 20));

            XSSFRow row4 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row4, 0, "Printed On: \t \t     " + LocalDateTime.now(), stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 20));

            List<String> contentHeaders = Arrays.asList(
                    "Product Name",
                    "Product Code",
                    "Category",
                    "Available Quantity",
                    "Unit Price",
                    "Cost Price"
            );

            int colIndex;
            Row headerRow = sheet.createRow(rowIndex += 2);

            for (int i = 0; i < contentHeaders.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(contentHeaders.get(i));
                cell.setCellStyle(headerCellStyle);
            }

            ++rowIndex;

            addRecordData(content,
                          sheet,
                          rowIndex,
                          stringDataCellStyleLeftAlign,
                          numericDataCellStyleRightAlign);

            for (int i = 0; i < contentHeaders.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            bos.close();

            return bos;
        } catch (IOException e) {
            throw new BadRequestRestApiException().developerMessage("Error in create file")
                    .userMessage("Sorry. Error in create file");
        }
    }

    public void addRecordData(List<Product> products,
                              XSSFSheet sheet,
                              int rowIndex,
                              CellStyle dataCellStyle,
                              CellStyle dataCellStyleRightAlign) {

        int colIndex;

        for (Product product : products) {
            colIndex = -1;
            XSSFRow row = sheet.createRow(++rowIndex);

            String productName = product.getProductName();
            String productCode = product.getSku();
            String productCategory = product.getCategory() != null ? product.getCategory().getName() : "N/A";
            String availableQuantity = "" + product.getAvailableStock();
            String unitPrice = "" + product.getSellPrice();
            String costPrice = "" + product.getBuyingPrice();

            excelUtils.createStringDataCell(row, ++colIndex, productName, dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, productCode, dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, productCategory, dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, availableQuantity, dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, unitPrice, dataCellStyleRightAlign);
            excelUtils.createStringDataCell(row, ++colIndex, costPrice, dataCellStyleRightAlign);
        }
    }
}
