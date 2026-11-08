package com.app.itibari.utils.helpers;

import com.app.itibari.models.entities.CustomerNotes;
import com.app.itibari.models.entities.Department;
import com.app.itibari.models.entities.Person;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.services.EmailService;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.formatter.DateFormatter;
import com.itextpdf.io.source.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class CsNotesToExcel {

    private final ExcelUtils excelUtils;

    public CsNotesToExcel(ExcelUtils excelUtils) {
        this.excelUtils = excelUtils;
    }

    public ByteArrayOutputStream generateCsNotesExcel(List<CustomerNotes> content, String departmentName) {

        try {

            String documentTitle = "Churned Cs Notes Report";

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
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 3));

            XSSFRow row3 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row3,
                    0,
                    "Printed By: \t \t  Auto-Gen",
                    stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 3));

            XSSFRow row4 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row4, 0, "Printed On: \t \t     " + DateFormatter.formatDate(LocalDateTime.now()), stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 3));

            XSSFRow row5 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row5, 0, "Total Items: \t \t     " + content.size(), stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 3));

            XSSFRow row6 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row6, 0, "Department : \t \t     " + departmentName, stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 3));

            List<String> contentHeaders = Arrays.asList(
                    "customer name",
                    "phone",
                    "code",
                    "description"
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

    public void addRecordData(List<CustomerNotes> customerNotes,
                              XSSFSheet sheet,
                              int rowIndex,
                              CellStyle dataCellStyle,
                              CellStyle dataCellStyleRightAlign) {

        int colIndex;

        for (CustomerNotes note : customerNotes) {
            colIndex = -1;
            XSSFRow row = sheet.createRow(++rowIndex);


            excelUtils.createStringDataCell(row, ++colIndex, note.getCustomer() != null ? note.getCustomer().getName():"--", dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, note.getCustomer() != null ? note.getCustomer().getPhoneNumber():"--", dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, note.getReasonCode() != null ? note.getReasonCode().getCode():"--", dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, note.getReasonCode() != null ? note.getReasonCode().getDescription():"--", dataCellStyle);
        }
    }

    public File convertToFile(List<CustomerNotes> content, String departmentName) throws IOException {
        ByteArrayOutputStream outputStream = generateCsNotesExcel(content, departmentName);

        File file = new File("output.xlsx");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            outputStream.writeTo(fos);
        }
        return file;
    }
}
