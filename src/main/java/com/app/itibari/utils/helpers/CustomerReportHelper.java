package com.app.itibari.utils.helpers;

import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.entities.Person;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
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
public class CustomerReportHelper {

    private final PdfUtil pdfUtil;

    private final PersonRepository personRepository;

    private final ExcelUtils excelUtils;

    //CUSTOMER NAME	PHONE NUMBER	REGISTRATION DATE	SALES CHANNEL	REGION	ZONE	LANDMARK	REGISTERED BY
    public ByteArrayOutputStream createCustomersReportPDF(List<Customers> content, String name) {

        try {
            Person downloadedBy = personRepository.findByEmailAndDeletedIsFalse(name)
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Person with name " + name + " not found")
                            .userMessage("Person not found"));

            String subtitle = "Customer Information Report";

            Document document = new Document(PageSize.A3, 5f, 5f, 60f, 50f);
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            PdfWriter writer = PdfWriter.getInstance(document, out);
            PdfUtil event = new PdfUtil();
            writer.setPageEvent(event);
            document.open();

            Font companyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.GREEN);
            Paragraph companyHeader = new Paragraph("ITIBARI TECHNOLOGIES LTD", companyFont);
            companyHeader.setAlignment(Element.ALIGN_LEFT);
            companyHeader.setSpacingAfter(5);
            document.add(companyHeader);

            // Company details
            Font detailsFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
            Paragraph companyDetails = new Paragraph();
            companyDetails.add(new Chunk("TIN: 176-727-497", detailsFont));
            companyDetails.add(Chunk.NEWLINE);
            companyDetails.add(new Chunk("VPN: XX-XXXXXX-A", detailsFont));
            companyDetails.add(Chunk.NEWLINE);
            companyDetails.add(new Chunk("Mikocheni light industrial area, Dar es Salaam\nEmail: commercial@itibari.io", detailsFont));
            companyDetails.setAlignment(Element.ALIGN_LEFT);
            companyDetails.setSpacingAfter(20);
            document.add(companyDetails);

            // Divider line
            Paragraph divider = new Paragraph("_____________________________________________________________");
            divider.setAlignment(Element.ALIGN_CENTER);
            divider.setSpacingAfter(20);
            document.add(divider);

            // Document title section
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.GREEN);
            Paragraph title = new Paragraph(subtitle, titleFont);
            title.setAlignment(Element.ALIGN_LEFT);
            title.setSpacingAfter(20);
            document.add(title);

            // Table headers
            document.add(new Paragraph(" "));

            PdfPTable contentTable = new PdfPTable(9);
            contentTable.setWidthPercentage(100);
            contentTable.setWidths(new int[]{
                    2, 2, 2, 2, 2, 2, 2, 2, 2
            });

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            headerFont.setStyle(Font.BOLD);
            Font contentFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            String[] contentHeaders = {
                    "Customer Name",
                    "Phone Number",
                    "Registration Date",
                    "Sales Channel",
                    "Region",
                    "Zone",
                    "Landmark",
                    "Registered By",
                    "Status",
            };
            for (String header : contentHeaders) {
                PdfPCell headerCell = new PdfPCell(new Phrase(header, headerFont));
                headerCell.setBorder(PdfPCell.NO_BORDER);
                headerCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                contentTable.addCell(headerCell);
            }

            for (Customers customer : content) {
                String customerName = customer.getName() != null ? customer.getName() : "N/A";
                String phoneNumber = customer.getPhoneNumber() != null ? customer.getPhoneNumber() : "N/A";
                String registrationDate = customer.getRegistrationDate() != null ? customer.getRegistrationDate().toString() : "N/A";
                String sourceChannel = customer.getSourceChannel() != null ? customer.getSourceChannel().name() : "N/A";
                String region = customer.getOrganizationRegions() != null ? customer.getOrganizationRegions().getName() : "N/A";
                String zone = customer.getOrganizationLocation() != null ? customer.getOrganizationLocation().getName() : "N/A";
                String landmark = customer.getLandmark() != null ? customer.getLandmark() : "N/A";
                String registeredBy = customer.getCreatedBy() != null ? customer.getRegisteredBy() : "N/A";
                String status = customer.getCustomerActiveStatus() != null ? customer.getCustomerActiveStatus().name() : "Active";

                PdfPCell customerCell = new PdfPCell(new Phrase(customerName, contentFont));
                customerCell.setBorder(PdfPCell.NO_BORDER);
                customerCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                contentTable.addCell(customerCell);

                PdfPCell phoneCell = new PdfPCell(new Phrase(phoneNumber, contentFont));
                phoneCell.setBorder(PdfPCell.NO_BORDER);
                phoneCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                contentTable.addCell(phoneCell);

                PdfPCell registrationCell = new PdfPCell(new Phrase(registrationDate, contentFont));
                registrationCell.setBorder(PdfPCell.NO_BORDER);
                registrationCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                contentTable.addCell(registrationCell);

                PdfPCell salesChannelCell = new PdfPCell(new Phrase(sourceChannel, contentFont));
                salesChannelCell.setBorder(PdfPCell.NO_BORDER);
                salesChannelCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                contentTable.addCell(salesChannelCell);

                PdfPCell regionCell = new PdfPCell(new Phrase(region, contentFont));
                regionCell.setBorder(PdfPCell.NO_BORDER);
                regionCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                contentTable.addCell(regionCell);

                PdfPCell zoneCell = new PdfPCell(new Phrase(zone, contentFont));
                zoneCell.setBorder(PdfPCell.NO_BORDER);
                zoneCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                contentTable.addCell(zoneCell);

                PdfPCell landmarkCell = new PdfPCell(new Phrase(landmark, contentFont));
                landmarkCell.setBorder(PdfPCell.NO_BORDER);
                landmarkCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                contentTable.addCell(landmarkCell);

                PdfPCell registeredByCell = new PdfPCell(new Phrase(registeredBy, contentFont));
                registeredByCell.setBorder(PdfPCell.NO_BORDER);
                registeredByCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                contentTable.addCell(registeredByCell);

                PdfPCell statusCell = new PdfPCell(new Phrase(status, contentFont));
                statusCell.setBorder(PdfPCell.NO_BORDER);
                statusCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                contentTable.addCell(statusCell);
            }

            document.add(contentTable);
            document.add(new Paragraph(" "));
            document.add(new LineSeparator());
            document.add(new Paragraph(" "));

            // approved /generated by:

            Paragraph generatedBy = new Paragraph(String.format("Generated By: \t \t    %s ", downloadedBy.getFullName()), contentFont);
            generatedBy.setAlignment(Element.ALIGN_RIGHT);
            document.add(generatedBy);
            document.add(new Paragraph(" "));

            Font nbFont = new Font();
            nbFont.setFamily(Font.FontFamily.TIMES_ROMAN.name());
            nbFont.setSize(9);
            nbFont.setStyle(Font.ITALIC);
            PdfPCell nbCell = pdfUtil.createCell("", Element.ALIGN_LEFT, false, 4, null, true);

            nbCell.addElement(new Paragraph("ITIBARI TECHNOLOGIES LTD", nbFont));
            nbCell.addElement(new Paragraph("Regulated by the Industrial and Co-operative Societies Act, 2003", nbFont));
            PdfPTable descriptionInfoTable = new PdfPTable(4);
            descriptionInfoTable.setWidthPercentage(100);
            descriptionInfoTable.addCell(nbCell);

            document.add(descriptionInfoTable);

            document.close();
            return out;
        } catch (Exception e) {
            throw new BadRequestRestApiException().developerMessage("Error in create file")
                    .userMessage("Sorry. Error in create file");
        }
    }

    public ByteArrayOutputStream createCustomersReportExcel(List<Customers> content, String name) {

        try {
            Person downloadedBy = personRepository.findByEmailAndDeletedIsFalse(name)
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Person with name " + name + " not found")
                            .userMessage("Person not found"));

            String documentTitle = "Customer Information Report";

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
                    "Customer Name",
                    "Phone Number",
                    "Registration Date",
                    "Sales Channel",
                    "Region",
                    "Zone",
                    "Landmark",
                    "Registered By",
                    "Status"
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

    public void addRecordData(List<Customers> customers,
                              XSSFSheet sheet,
                              int rowIndex,
                              CellStyle dataCellStyle,
                              CellStyle dataCellStyleRightAlign) {

        int colIndex;

        for (Customers customer : customers) {
            colIndex = -1;
            XSSFRow row = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row, ++colIndex, customer.getName(), dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, customer.getPhoneNumber(), dataCellStyle);
            excelUtils.createStringDataCell(row,
                                            ++colIndex,
                                            customer.getRegistrationDate() != null ? customer.getRegistrationDate().toString() : "",
                                            dataCellStyle);
            excelUtils.createStringDataCell(row,
                                            ++colIndex,
                                            customer.getSourceChannel() != null ? customer.getSourceChannel().name() : "",
                                            dataCellStyle);
            excelUtils.createStringDataCell(row,
                                            ++colIndex,
                                            customer.getOrganizationRegions() != null ? customer.getOrganizationRegions().getName() : "",
                                            dataCellStyle);
            excelUtils.createStringDataCell(row,
                                            ++colIndex,
                                            customer.getOrganizationLocation() != null ? customer.getOrganizationLocation().getName() : "",
                                            dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, customer.getLandmark() != null ? customer.getLandmark() : "", dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, customer.getRegisteredBy(), dataCellStyle);
            excelUtils.createStringDataCell(row,
                                            ++colIndex,
                                            customer.getCustomerActiveStatus() != null ? customer
                                                    .getCustomerActiveStatus()
                                                    .name() : "Active",
                                            dataCellStyle);
        }
    }
}
