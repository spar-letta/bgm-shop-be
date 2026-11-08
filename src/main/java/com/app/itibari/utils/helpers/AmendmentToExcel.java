package com.app.itibari.utils.helpers;

import com.app.itibari.models.entities.CustomerNotes;
import com.app.itibari.models.entities.Department;
import com.app.itibari.models.entities.OrderAmendment;
import com.app.itibari.models.entities.Person;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.services.EmailService;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.formatter.DateFormatter;
import com.itextpdf.io.source.ByteArrayOutputStream;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Component
public class AmendmentToExcel {

    private final ExcelUtils excelUtils;

    private final PersonRepository personRepository;
    private final EmailBodyCrafter emailBodyCrafter;
    private final EmailService emailService;

    public AmendmentToExcel(ExcelUtils excelUtils, PersonRepository personRepository, EmailBodyCrafter emailBodyCrafter, EmailService emailService) {
        this.excelUtils = excelUtils;
        this.personRepository = personRepository;
        this.emailBodyCrafter = emailBodyCrafter;
        this.emailService = emailService;
    }


    public ByteArrayOutputStream generateExcel(List<OrderAmendment> content, String departmentName) {
        try {

            String documentTitle = "Daily Amendment Report";

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
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 5));

            XSSFRow row3 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row3,
                    0,
                    "Printed By: \t \t  Auto-Gen",
                    stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 5));

            XSSFRow row4 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row4, 0, "Printed On: \t \t     " + DateFormatter.formatDate(LocalDateTime.now()), stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 5));

            XSSFRow row5 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row5, 0, "Total Items: \t \t     " + content.size(), stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 5));

            XSSFRow row6 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row6, 0, "Department : \t \t     " + departmentName, stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 5));

            List<String> contentHeaders = Arrays.asList(
                    "order number",
                    "route number",
                    "delivery agent",
                    "customer name",
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

    public void addRecordData(List<OrderAmendment> orderAmendmentList,
                              XSSFSheet sheet,
                              int rowIndex,
                              CellStyle dataCellStyle,
                              CellStyle dataCellStyleRightAlign) {

        int colIndex;

        for (OrderAmendment amendment : orderAmendmentList) {
            colIndex = -1;
            XSSFRow row = sheet.createRow(++rowIndex);


            excelUtils.createStringDataCell(row, ++colIndex, amendment.getSalesOrder() != null ? amendment.getSalesOrder().getSalesOrderNumber() : "--", dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, amendment.getRouteId(), dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, amendment.getDeliveryAgent() != null ? amendment.getDeliveryAgent().getFullName() : "--", dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, amendment.getSalesOrder() != null ? amendment.getSalesOrder().getCustomer() != null ? amendment.getSalesOrder().getCustomer().getName(): "--" : "--", dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, amendment.getRejectionsCode() != null ? amendment.getRejectionsCode().getCode() : "--", dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, amendment.getRejectionsCode() != null ? amendment.getRejectionsCode().getDescription() : "--", dataCellStyle);
        }
    }

    public File convertToFile(List<OrderAmendment> content, String departmentName) throws IOException {
        ByteArrayOutputStream outputStream = generateExcel(content, departmentName);

        File file = File.createTempFile("amendment_report_", ".xlsx");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            outputStream.writeTo(fos);
        }
        return file;
    }

    public void sendNotificationToDepartmentOfficer(File toFile, Department department) {
        List<Person> personList = personRepository.findByDepartmentAndDeletedIsFalse(department);
        if (!personList.isEmpty()) {
            for (Person person : personList) {
                sendEmail(person.getEmail(), person.getFullName(), toFile, department.getName());
                log.info("-----check email person {}", person.getEmail());
            }
        }
    }

    private void sendEmail(String emailTo, String recipientFullName, File toFile, String departmentName) {
        String message = String.format(
                "Please find attached the Amendment Report. This report has been tagged to you as members of the %s Department and is pending your action.\n" +
                        "\n" +
                        "Kindly review the report and take the necessary steps at the earliest convenience.\n" +
                        "\n" +
                        "Best regards.",
                departmentName);
        String emailBody = emailBodyCrafter.craftDepartment2Email(recipientFullName, message);
        emailService.sendMessage(emailTo, "Amendment Notification", toFile, emailBody, Collections.emptyList());
    }
}
