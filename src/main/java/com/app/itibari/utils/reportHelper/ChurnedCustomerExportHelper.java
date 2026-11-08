package com.app.itibari.utils.reportHelper;

import com.app.itibari.dataTypes.CustomerOutboundCallStatus;
import com.app.itibari.models.response.CallStatusSummary;
import com.app.itibari.models.response.ChurnedCustomer;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.formatter.DateFormatter;
import com.app.itibari.utils.helpers.ExcelUtils;
import com.app.itibari.utils.helpers.RoundedBackgroundCellEvent;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ChurnedCustomerExportHelper {
    private final ExcelUtils excelUtils;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String company = "Itibari Technologies Limited";

    private static final String companyAddress = "Plot 544/1, Mikocheni light industrial area, Dar es Salaam";

    private static final String companyEmail = "Email: commercial@itibari.io";

    private final RoundedBackgroundCellEvent backgroundEvent;

    public ByteArrayOutputStream createExport(List<ChurnedCustomer> fetchedList, String name, Boolean inActive) {

        String title;
        if (Boolean.TRUE.equals(inActive)) {
            title = "In Active Customer Report";
        } else if (Boolean.FALSE.equals(inActive)) {
            title = "Non Churned Customer Report";
        } else {
            title = "Churned Customer Report";
        }

        try {
            String documentTitle = title;

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
                    "Customer Name",
                    "Telephone",
                    "Last Order Date",
                    "Assigned Sales Agent",
                    "Last Contact Date",
                    "District",
                    "Ward",
                    "Call Status",
                    "Lead Status",
                    "Notes"
            );

            Row headerRow = sheet.createRow(rowIndex += 2);
            for (int i = 0; i < contentHeaders.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(contentHeaders.get(i));
                cell.setCellStyle(headerCellStyle);
            }

            ++rowIndex;

            // Add parent + child data
            addRecordData(workbook, fetchedList, sheet, rowIndex, stringDataCellStyleLeftAlign, numericDataCellStyleRightAlign);

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
                              List<ChurnedCustomer> resp,
                              XSSFSheet sheet,
                              int rowIndex,
                              CellStyle dataCellStyle,
                              CellStyle dataCellStyleRightAlign) {

        CellStyle parentRowStyle = workbook.createCellStyle();
        parentRowStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());

        Font parentFont = workbook.createFont();
        parentFont.setBold(true);
        parentRowStyle.setFont(parentFont);

        for (ChurnedCustomer churnedCustomer : resp) {

            XSSFRow parentRow = sheet.createRow(++rowIndex);
            int colIndex = -1;

            excelUtils.createStringDataCell(parentRow, ++colIndex, churnedCustomer.name() != null ? churnedCustomer.name() : "--", parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, churnedCustomer.phoneNumber() != null ? churnedCustomer.phoneNumber() : "--", parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, churnedCustomer.lastOrderDate() != null ? DateFormatter.formatDate(churnedCustomer.lastOrderDate()) : "--", parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, churnedCustomer.assignedSalesAgent() != null ? churnedCustomer.assignedSalesAgent() : "--", parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, churnedCustomer.lastContactDate() != null ? DateFormatter.formatDate(churnedCustomer.lastContactDate()) : "--", parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, churnedCustomer.district() != null ? churnedCustomer.district() : "--", parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, churnedCustomer.ward() != null ? churnedCustomer.ward() : "--", parentRowStyle);

            excelUtils.createStringDataCell(parentRow, ++colIndex, resolveCallStatusLabel(churnedCustomer.callStatus()), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, churnedCustomer.leadStatus() != null ? churnedCustomer.leadStatus().name() : "--", parentRowStyle);

            String notesSummary = "--";
            if (churnedCustomer.noteSummary() != null && !churnedCustomer.noteSummary().isEmpty()){
                notesSummary = churnedCustomer.noteSummary().get(0).code();
            }
            excelUtils.createStringDataCell(parentRow, ++colIndex, notesSummary, parentRowStyle);

        }
    }

    public ByteArrayOutputStream createPdf(List<ChurnedCustomer> content, String name, Boolean inActive) throws DocumentException, IOException {
        String title;
        if (Boolean.TRUE.equals(inActive)) {
            title = "In Active Customer Report";
        } else if (Boolean.FALSE.equals(inActive)) {
            title = "Non Churned Customer Report";
        } else {
            title = "Churned Customer Report";
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Document document = new Document(PageSize.A4, 10f, 10f, 54f, 54f);

        PdfWriter writer = PdfWriter.getInstance(document, outputStream);
        document.open();

        Image logo_2 = Image.getInstance(new ClassPathResource("static/images/bigger_log.png").getInputStream().readAllBytes());
        logo_2.scaleToFit(330, 180);  //630  and 200

        float y = document.getPageSize().getHeight()
                - document.topMargin()
                - logo_2.getScaledHeight()
                + 60f;  // whe you increase like 60+  you move it up

        logo_2.setAbsolutePosition(280, y);  // 270
        writer.getDirectContent().addImage(logo_2);

        addHeaderSection(document);
        addTitle(document, title);
        addItemsTable(document, content);
        document.close();

        return outputStream;
    }

    private void addHeaderSection(Document document) throws IOException, DocumentException {
        BaseColor receiptGreen = new BaseColor(84, 138, 70);

        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingAfter(10f);

        PdfPCell logoCell = new PdfPCell();
        Image logo = Image.getInstance(new ClassPathResource("static/images/logoImage_1.jpg").getInputStream().readAllBytes());
        logo.scaleToFit(170, 140);
        logo.setAlignment(Image.ALIGN_LEFT);
        logo.setIndentationLeft(-10f);
        document.add(logo);
        logoCell.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(logoCell);

        PdfPCell companyCell = new PdfPCell();
        companyCell.setBorder(Rectangle.NO_BORDER);

        com.itextpdf.text.Font companyNameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, receiptGreen);
        com.itextpdf.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, receiptGreen);

        Paragraph companyName = new Paragraph(company, normalFont);
        companyCell.addElement(companyName);

        Paragraph addressInfo = new Paragraph(companyAddress, normalFont);
        companyCell.addElement(addressInfo);

        Paragraph emailInfo = new Paragraph(companyEmail, normalFont);
        companyCell.addElement(emailInfo);

        headerTable.addCell(companyCell);

        document.add(headerTable);
    }

    public void addTitle(Document document, String titleString) throws DocumentException {
        com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new BaseColor(84, 138, 70));
        Paragraph title = new Paragraph(titleString, titleFont);

        PdfPTable titleTable = new PdfPTable(1);
        titleTable.setWidthPercentage(100);
        titleTable.setSpacingBefore(20f);
        titleTable.setSpacingAfter(20f);

        PdfPCell titleCell = new PdfPCell(title);
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPaddingTop(6);
        titleCell.setPaddingBottom(8);
        titleCell.setHorizontalAlignment(Element.ALIGN_LEFT);

        titleCell.setCellEvent(backgroundEvent);

        titleTable.addCell(titleCell);
        document.add(titleTable);
    }

    private void addItemsTable(Document document, List<ChurnedCustomer> content) throws DocumentException {
        BaseColor receiptGreen = new BaseColor(84, 138, 70);
        BaseColor receiptGreen_2 = BaseColor.LIGHT_GRAY;
        BaseColor receiptWhite = BaseColor.WHITE;
        LineSeparator line = new LineSeparator();
        line.setLineColor(BaseColor.LIGHT_GRAY);

        PdfPTable itemsTable = new PdfPTable(11);
        itemsTable.setWidthPercentage(100);
        itemsTable.setWidths(new float[]{1, 5, 3, 3, 3, 3, 3, 3, 3, 3, 3});
        itemsTable.setSpacingAfter(10);
        itemsTable.setSpacingAfter(1f);

        com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, receiptWhite);
        com.itextpdf.text.Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 7);

        PdfPCell listNo = new PdfPCell(new Phrase("No", headerFont));
        listNo.setPadding(5);
        listNo.setBorder(Rectangle.NO_BORDER);
        listNo.setHorizontalAlignment(Element.ALIGN_CENTER);
        listNo.setBackgroundColor(receiptGreen);
        itemsTable.addCell(listNo);

        PdfPCell descHeader = new PdfPCell(new Phrase("Customer Name", headerFont));
        descHeader.setPadding(5);
        descHeader.setBorder(Rectangle.NO_BORDER);
        descHeader.setBackgroundColor(receiptGreen);
        descHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        itemsTable.addCell(descHeader);


        PdfPCell qtyHeader = new PdfPCell(new Phrase("Telephone", headerFont));
        qtyHeader.setPadding(5);
        qtyHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        qtyHeader.setBorder(Rectangle.NO_BORDER);
        qtyHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(qtyHeader);


        PdfPCell unitPriceHeader = new PdfPCell(new Phrase("Last Order Date", headerFont));
        unitPriceHeader.setPadding(5);
        unitPriceHeader.setBorder(Rectangle.NO_BORDER);
        unitPriceHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        unitPriceHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(unitPriceHeader);

        PdfPCell totalExpectedHeader = new PdfPCell(new Phrase("Assigned Sales Agent", headerFont));
        totalExpectedHeader.setPadding(5);
        totalExpectedHeader.setBorder(Rectangle.NO_BORDER);
        totalExpectedHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        totalExpectedHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(totalExpectedHeader);

        PdfPCell contactHeader = new PdfPCell(new Phrase("Last Contact Date", headerFont));
        contactHeader.setPadding(5);
        contactHeader.setBorder(Rectangle.NO_BORDER);
        contactHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        contactHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(contactHeader);

        PdfPCell districtHeader = new PdfPCell(new Phrase("District", headerFont));
        districtHeader.setPadding(5);
        districtHeader.setBorder(Rectangle.NO_BORDER);
        districtHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        districtHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(districtHeader);

        PdfPCell wardHeader = new PdfPCell(new Phrase("Ward", headerFont));
        wardHeader.setPadding(5);
        wardHeader.setBorder(Rectangle.NO_BORDER);
        wardHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        wardHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(wardHeader);

        PdfPCell csHeader = new PdfPCell(new Phrase("Call Status", headerFont));
        csHeader.setPadding(5);
        csHeader.setBorder(Rectangle.NO_BORDER);
        csHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        csHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(csHeader);

        PdfPCell leadStatusHeader = new PdfPCell(new Phrase("Lead Status", headerFont));
        leadStatusHeader.setPadding(5);
        leadStatusHeader.setBorder(Rectangle.NO_BORDER);
        leadStatusHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        leadStatusHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(leadStatusHeader);

        PdfPCell nHeader = new PdfPCell(new Phrase("Notes", headerFont));
        nHeader.setPadding(5);
        nHeader.setBorder(Rectangle.NO_BORDER);
        nHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        nHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(nHeader);

        int rowIndex = 0;

        for (ChurnedCustomer churnedCustomer : content) {

            PdfPCell numberCell = new PdfPCell(new Phrase(String.valueOf(rowIndex + 1), cellFont));
            numberCell.setPadding(5);
            numberCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            numberCell.setBorderColor(receiptGreen);
            itemsTable.addCell(numberCell);

            PdfPCell descCell = new PdfPCell(new Phrase(churnedCustomer.name() != null ? churnedCustomer.name() : "--", cellFont));
            descCell.setPadding(5);
            numberCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            descCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(descCell);

            PdfPCell qtyCell = new PdfPCell(new Phrase(churnedCustomer.phoneNumber() != null ? churnedCustomer.phoneNumber() : "--", cellFont));
            qtyCell.setPadding(5);
            qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            qtyCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(qtyCell);

            PdfPCell orderDateCell = new PdfPCell(new Phrase(churnedCustomer.lastOrderDate() != null ? DateFormatter.formatDate(churnedCustomer.lastOrderDate()) : "--", cellFont));
            orderDateCell.setPadding(5);
            orderDateCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            orderDateCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(orderDateCell);

            PdfPCell agentCell = new PdfPCell(new Phrase(churnedCustomer.assignedSalesAgent() != null ? churnedCustomer.assignedSalesAgent() : "--", cellFont));
            agentCell.setPadding(5);
            agentCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            agentCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(agentCell);

            PdfPCell lastContactCell = new PdfPCell(new Phrase(churnedCustomer.lastContactDate() != null ? DateFormatter.formatDate(churnedCustomer.lastContactDate()) : "--", cellFont));
            lastContactCell.setPadding(5);
            lastContactCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            lastContactCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(lastContactCell);

            PdfPCell districtCell = new PdfPCell(new Phrase(churnedCustomer.district() != null ? churnedCustomer.district() : "--", cellFont));
            districtCell.setPadding(5);
            districtCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            districtCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(districtCell);

            PdfPCell wardCell = new PdfPCell(new Phrase(churnedCustomer.ward() != null ? churnedCustomer.ward() : "--", cellFont));
            wardCell.setPadding(5);
            wardCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            wardCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(wardCell);

            PdfPCell csCell = new PdfPCell(new Phrase(resolveCallStatusLabel(churnedCustomer.callStatus()), cellFont));
            csCell.setPadding(5);
            csCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            csCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(csCell);

            PdfPCell leadStatusCell = new PdfPCell(new Phrase(churnedCustomer.leadStatus() != null ? churnedCustomer.leadStatus().name() : "--", cellFont));
            leadStatusCell.setPadding(5);
            leadStatusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            leadStatusCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(leadStatusCell);

            String notesSummary = "--";
            if (churnedCustomer.noteSummary() != null && !churnedCustomer.noteSummary().isEmpty()){
                notesSummary = churnedCustomer.noteSummary().get(0).code();
            }


            PdfPCell nCell = new PdfPCell(new Phrase( notesSummary, cellFont));
            nCell.setPadding(5);
            nCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            nCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(nCell);

            rowIndex++;
        }

        document.add(itemsTable);
    }

    // Prefer DB description; else fromValue (Hang Up aliases); else raw code — never fail the export.
    static String resolveCallStatusLabel(CallStatusSummary callStatus) {
        if (callStatus == null) {
            return "--";
        }
        if (callStatus.description() != null && !callStatus.description().isBlank()) {
            return callStatus.description().trim();
        }
        String code = callStatus.code();
        if (code == null || code.isBlank()) {
            return "--";
        }
        try {
            CustomerOutboundCallStatus status = CustomerOutboundCallStatus.fromValue(code);
            if (status != null) {
                return status.getLabel();
            }
        } catch (IllegalArgumentException ignored) {
            // Catalog row outside the fixed PATCH enum set — still exportable.
        }
        return code.trim();
    }
}
