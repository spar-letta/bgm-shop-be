package com.app.itibari.utils.reportHelper;

import com.app.itibari.models.response.HelpDeskDTOResponse;
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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TicketReportHelper {
    private final ExcelUtils excelUtils;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String company = "Itibari Technologies Limited";

    private static final String companyAddress = "Plot 544/1, Mikocheni light industrial area, Dar es Salaam";

    private static final String companyEmail = "Email: commercial@itibari.io";

    private final RoundedBackgroundCellEvent backgroundEvent;

    public ByteArrayOutputStream createPdfExport(Page<HelpDeskDTOResponse> content, String name) throws DocumentException, IOException {
        ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");
        LocalDateTime nairobiDate = LocalDateTime.now(nairobiZone);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm a");
        String formattedDateToday = String.format("printed on : %s", nairobiDate.format(formatter));
        String printedBy = String.format("by: %s", name.toLowerCase());


        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Document document = new Document(PageSize.A4.rotate());

        PdfWriter writer = PdfWriter.getInstance(document, outputStream);
        document.open();

        Image logo_2 = Image.getInstance(new ClassPathResource("static/images/bigger_log.png").getInputStream().readAllBytes());
        logo_2.scaleToFit(630, 180);  //630  and 200

        float y = document.getPageSize().getHeight()
                - document.topMargin()
                - logo_2.getScaledHeight()
                + 70f;  // whe you increase like 60+  you move it up

        logo_2.setAbsolutePosition(y-90, y);
        writer.getDirectContent().addImage(logo_2);

        addHeaderSection(document);
        addTitle(document, "Ticket Report", formattedDateToday, printedBy);
        addItemsTable(document, content.getContent());

        document.close();

        return outputStream;
    }

    private void addItemsTable(Document document, List<HelpDeskDTOResponse> content) throws DocumentException {

        BaseColor receiptGreen = new BaseColor(84, 138, 70);
        BaseColor receiptGreen_2 = BaseColor.LIGHT_GRAY;
        BaseColor receiptWhite = BaseColor.WHITE;
        LineSeparator line = new LineSeparator();
        line.setLineColor(BaseColor.LIGHT_GRAY);

        PdfPTable itemsTable = new PdfPTable(12);
        itemsTable.setWidthPercentage(100);
        itemsTable.setWidths(new float[]{2, 4, 4, 3, 6, 4, 6, 6, 4, 4, 4, 4});
        itemsTable.setSpacingAfter(10);
        itemsTable.setSpacingAfter(1f);

        com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, receiptWhite);
        com.itextpdf.text.Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 6);

        //1
        PdfPCell listNo = new PdfPCell(new Phrase("No", headerFont));
//        listNo.setPadding(5);
        listNo.setBorder(Rectangle.NO_BORDER);
        listNo.setHorizontalAlignment(Element.ALIGN_CENTER);
        listNo.setBackgroundColor(receiptGreen);
        itemsTable.addCell(listNo);

        //2
        PdfPCell descHeader = new PdfPCell(new Phrase("Ticket No.", headerFont));
//        descHeader.setPadding(5);
        descHeader.setBorder(Rectangle.NO_BORDER);
        descHeader.setBackgroundColor(receiptGreen);
        descHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        itemsTable.addCell(descHeader);

        //3
        PdfPCell createdDateHeader = new PdfPCell(new Phrase("Date", headerFont));
//        createdDateHeader.setPadding(5);
        createdDateHeader.setBorder(Rectangle.NO_BORDER);
        createdDateHeader.setBackgroundColor(receiptGreen);
        createdDateHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        itemsTable.addCell(createdDateHeader);

        //4
        PdfPCell qtyHeader = new PdfPCell(new Phrase("Time", headerFont));
//        qtyHeader.setPadding(5);
        qtyHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        qtyHeader.setBorder(Rectangle.NO_BORDER);
        qtyHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(qtyHeader);

//        //5
//        PdfPCell createdByNameHeader = new PdfPCell(new Phrase("Created By", headerFont));
////        createdByNameHeader.setPadding(5);
//        createdByNameHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
//        createdByNameHeader.setBorder(Rectangle.NO_BORDER);
//        createdByNameHeader.setBackgroundColor(receiptGreen);
//        itemsTable.addCell(createdByNameHeader);

        //5
        PdfPCell entityUserNameHeader = new PdfPCell(new Phrase("Customer", headerFont));
//        entityUserNameHeader.setPadding(5);
        entityUserNameHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        entityUserNameHeader.setBorder(Rectangle.NO_BORDER);
        entityUserNameHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(entityUserNameHeader);

        //6
        PdfPCell titleHeader = new PdfPCell(new Phrase("Issue", headerFont));
//        titleHeader.setPadding(5);
        titleHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleHeader.setBorder(Rectangle.NO_BORDER);
        titleHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(titleHeader);

        //8
//        PdfPCell contentHeader = new PdfPCell(new Phrase("Description", headerFont));
////        contentHeader.setPadding(5);
//        contentHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
//        contentHeader.setBorder(Rectangle.NO_BORDER);
//        contentHeader.setBackgroundColor(receiptGreen);
//        itemsTable.addCell(contentHeader);

        //7
        PdfPCell departmentHeader = new PdfPCell(new Phrase("Primary Department", headerFont));
//        departmentHeader.setPadding(5);
        departmentHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        departmentHeader.setBorder(Rectangle.NO_BORDER);
        departmentHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(departmentHeader);

        //8
        PdfPCell workflowStatusHeader = new PdfPCell(new Phrase("Current Department", headerFont));
//        workflowStatusHeader.setPadding(5);
        workflowStatusHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        workflowStatusHeader.setBorder(Rectangle.NO_BORDER);
        workflowStatusHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(workflowStatusHeader);

        //9
        PdfPCell statusHeader = new PdfPCell(new Phrase("Status", headerFont));
//        statusHeader.setPadding(5);
        statusHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        statusHeader.setBorder(Rectangle.NO_BORDER);
        statusHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(statusHeader);

        //12
//        PdfPCell ticketPriorityHeader = new PdfPCell(new Phrase("Priority", headerFont));
////        ticketPriorityHeader.setPadding(5);
//        ticketPriorityHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
//        ticketPriorityHeader.setBorder(Rectangle.NO_BORDER);
//        ticketPriorityHeader.setBackgroundColor(receiptGreen);
//        itemsTable.addCell(ticketPriorityHeader);

        //10
        PdfPCell entityPhoneHeader = new PdfPCell(new Phrase("Phone", headerFont));
//        entityPhoneHeader.setPadding(5);
        entityPhoneHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        entityPhoneHeader.setBorder(Rectangle.NO_BORDER);
        entityPhoneHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(entityPhoneHeader);

        //
//        PdfPCell regionHeader = new PdfPCell(new Phrase("District", headerFont));
////        regionHeader.setPadding(5);
//        regionHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
//        regionHeader.setBorder(Rectangle.NO_BORDER);
//        regionHeader.setBackgroundColor(receiptGreen);
//        itemsTable.addCell(regionHeader);

        //15
//        PdfPCell wardHeader = new PdfPCell(new Phrase("Ward", headerFont));
////        wardHeader.setPadding(5);
//        wardHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
//        wardHeader.setBorder(Rectangle.NO_BORDER);
//        wardHeader.setBackgroundColor(receiptGreen);
//        itemsTable.addCell(wardHeader);

        //11
        PdfPCell assignedToNameHeader = new PdfPCell(new Phrase("Assigned To", headerFont));
//        assignedToNameHeader.setPadding(5);
        assignedToNameHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        assignedToNameHeader.setBorder(Rectangle.NO_BORDER);
        assignedToNameHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(assignedToNameHeader);

        //12
        PdfPCell customerTypeHeader = new PdfPCell(new Phrase("Customer Type", headerFont));
//        customerTypeHeader.setPadding(5);
        customerTypeHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        customerTypeHeader.setBorder(Rectangle.NO_BORDER);
        customerTypeHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(customerTypeHeader);

        //18
//        PdfPCell pocEmailHeader = new PdfPCell(new Phrase("POC Email", headerFont));
////        pocEmailHeader.setPadding(5);
//        pocEmailHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
//        pocEmailHeader.setBorder(Rectangle.NO_BORDER);
//        pocEmailHeader.setBackgroundColor(receiptGreen);
//        itemsTable.addCell(pocEmailHeader);

        //19
//        PdfPCell emailInCopyHeader = new PdfPCell(new Phrase("Emails In Copy", headerFont));
////        emailInCopyHeader.setPadding(5);
//        emailInCopyHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
//        emailInCopyHeader.setBorder(Rectangle.NO_BORDER);
//        emailInCopyHeader.setBackgroundColor(receiptGreen);
//        itemsTable.addCell(emailInCopyHeader);

        //20
//        PdfPCell productListHeader = new PdfPCell(new Phrase("Products", headerFont));
////        productListHeader.setPadding(5);
//        productListHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
//        productListHeader.setBorder(Rectangle.NO_BORDER);
//        productListHeader.setBackgroundColor(receiptGreen);
//        itemsTable.addCell(productListHeader);

        //21
//        PdfPCell customerListHeader = new PdfPCell(new Phrase("Customers", headerFont));
////        customerListHeader.setPadding(5);
//        customerListHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
//        customerListHeader.setBorder(Rectangle.NO_BORDER);
//        customerListHeader.setBackgroundColor(receiptGreen);
//        itemsTable.addCell(customerListHeader);

        //22
//        PdfPCell departmentInCopyHeader = new PdfPCell(new Phrase("Departments In Copy", headerFont));
////        departmentInCopyHeader.setPadding(5);
//        departmentInCopyHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
//        departmentInCopyHeader.setBorder(Rectangle.NO_BORDER);
//        departmentInCopyHeader.setBackgroundColor(receiptGreen);
//        itemsTable.addCell(departmentInCopyHeader);


        int rowIndex = 0;

        for (HelpDeskDTOResponse helpDeskDTOResponse : content) {

            PdfPCell numberCell = new PdfPCell(new Phrase(String.valueOf(rowIndex + 1), cellFont));
//            numberCell.setPadding(5);
            numberCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            numberCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(numberCell);

            PdfPCell descCell = new PdfPCell(new Phrase(helpDeskDTOResponse.ticketNumber(), cellFont));
//            descCell.setPadding(5);
            numberCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            descCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(descCell);

            PdfPCell qtyCell = new PdfPCell(new Phrase(helpDeskDTOResponse.createdDate(), cellFont));
//            qtyCell.setPadding(5);
            qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            qtyCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(qtyCell);

            PdfPCell orderDateCell = new PdfPCell(new Phrase(helpDeskDTOResponse.createdTime(), cellFont));
//            orderDateCell.setPadding(5);
            orderDateCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            orderDateCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(orderDateCell);

//            PdfPCell createdByNameCell = new PdfPCell(new Phrase(helpDeskDTOResponse.createdByName(), cellFont));
////            createdByNameCell.setPadding(5);
//            createdByNameCell.setHorizontalAlignment(Element.ALIGN_CENTER);
//            createdByNameCell.setBorderColor(receiptGreen_2);
//            itemsTable.addCell(createdByNameCell);

            PdfPCell entityUserNameCell = new PdfPCell(new Phrase(helpDeskDTOResponse.entityUserName(), cellFont));
//            entityUserNameCell.setPadding(5);
            entityUserNameCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            entityUserNameCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(entityUserNameCell);

            PdfPCell titleCell = new PdfPCell(new Phrase(helpDeskDTOResponse.title(), cellFont));
//            titleCell.setPadding(5);
            titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            titleCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(titleCell);

//            PdfPCell contentCell = new PdfPCell(new Phrase(helpDeskDTOResponse.content(), cellFont));
////            contentCell.setPadding(5);
//            contentCell.setHorizontalAlignment(Element.ALIGN_CENTER);
//            contentCell.setBorderColor(receiptGreen_2);
//            itemsTable.addCell(contentCell);

            PdfPCell departmentCell = new PdfPCell(new Phrase(helpDeskDTOResponse.department(), cellFont));
//            departmentCell.setPadding(5);
            departmentCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            departmentCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(departmentCell);

            PdfPCell workflowStatusCell = new PdfPCell(new Phrase(helpDeskDTOResponse.workflowStatus(), cellFont));
//            workflowStatusCell.setPadding(5);
            workflowStatusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            workflowStatusCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(workflowStatusCell);

            PdfPCell statusCell = new PdfPCell(new Phrase(helpDeskDTOResponse.status(), cellFont));
//            statusCell.setPadding(5);
            statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            statusCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(statusCell);

//            PdfPCell ticketPriorityCell = new PdfPCell(new Phrase(helpDeskDTOResponse.ticketPriority(), cellFont));
////            ticketPriorityCell.setPadding(5);
//            ticketPriorityCell.setHorizontalAlignment(Element.ALIGN_CENTER);
//            ticketPriorityCell.setBorderColor(receiptGreen_2);
//            itemsTable.addCell(ticketPriorityCell);

            PdfPCell entityPhoneCell = new PdfPCell(new Phrase(helpDeskDTOResponse.entityPhone(), cellFont));
//            entityPhoneCell.setPadding(5);
            entityPhoneCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            entityPhoneCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(entityPhoneCell);

//            PdfPCell regionCell = new PdfPCell(new Phrase(helpDeskDTOResponse.region() != null ? helpDeskDTOResponse.region().name() : "--", cellFont));
////            regionCell.setPadding(5);
//            regionCell.setHorizontalAlignment(Element.ALIGN_CENTER);
//            regionCell.setBorderColor(receiptGreen_2);
//            itemsTable.addCell(regionCell);

//            PdfPCell locationCell = new PdfPCell(new Phrase(helpDeskDTOResponse.location() != null ? helpDeskDTOResponse.location().name() : "--", cellFont));
////            locationCell.setPadding(5);
//            locationCell.setHorizontalAlignment(Element.ALIGN_CENTER);
//            locationCell.setBorderColor(receiptGreen_2);
//            itemsTable.addCell(locationCell);

            PdfPCell assignedToNameCell = new PdfPCell(new Phrase(helpDeskDTOResponse.assignedToName(), cellFont));
//            assignedToNameCell.setPadding(5);
            assignedToNameCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            assignedToNameCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(assignedToNameCell);

            PdfPCell customerTypeCell = new PdfPCell(new Phrase(helpDeskDTOResponse.customerType() != null ? (helpDeskDTOResponse.customerType().equalsIgnoreCase("Existing_Customer") ? "Existing" : "New") : "--", cellFont));
//            customerTypeCell.setPadding(5);
            customerTypeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            customerTypeCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(customerTypeCell);

//            PdfPCell pocEmailCell = new PdfPCell(new Phrase(helpDeskDTOResponse.pocEmail(), cellFont));
////            pocEmailCell.setPadding(5);
//            pocEmailCell.setHorizontalAlignment(Element.ALIGN_CENTER);
//            pocEmailCell.setBorderColor(receiptGreen_2);
//            itemsTable.addCell(pocEmailCell);

//            String emailsInCopy = "--";
//            if (helpDeskDTOResponse.emailInCopy() != null && !helpDeskDTOResponse.emailInCopy().isEmpty()) {
//                emailsInCopy = IntStream.range(0, helpDeskDTOResponse.emailInCopy().size())
//                        .mapToObj(i -> (i + 1) + "." + helpDeskDTOResponse.emailInCopy().get(i))
//                        .collect(Collectors.joining("\n"));
//
//            }
//
//
//            PdfPCell emailInCopyCell = new PdfPCell(new Phrase(emailsInCopy, cellFont));
////            emailInCopyCell.setPadding(5);
//            emailInCopyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
//            emailInCopyCell.setBorderColor(receiptGreen_2);
//            itemsTable.addCell(emailInCopyCell);

//            String productList = "--";
//            if (helpDeskDTOResponse.productList() != null && !helpDeskDTOResponse.productList().isEmpty()) {
//                productList = IntStream.range(0, helpDeskDTOResponse.productList().size())
//                        .mapToObj(i -> (i + 1) + "." + helpDeskDTOResponse.productList().get(i).name())
//                        .collect(Collectors.joining("\n"));
//            }
//
//            PdfPCell productListCell = new PdfPCell(new Phrase(productList, cellFont));
////            productListCell.setPadding(5);
//            productListCell.setHorizontalAlignment(Element.ALIGN_CENTER);
//            productListCell.setBorderColor(receiptGreen_2);
//            itemsTable.addCell(productListCell);

//            String customerList = "--";
//            if (helpDeskDTOResponse.customerList() != null && !helpDeskDTOResponse.customerList().isEmpty()) {
//                customerList = IntStream.range(0, helpDeskDTOResponse.customerList().size())
//                        .mapToObj(i -> (i + 1) + "." + helpDeskDTOResponse.customerList().get(i).name())
//                        .collect(Collectors.joining("\n"));
//            }
//
//            PdfPCell customerListCell = new PdfPCell(new Phrase(customerList, cellFont));
////            customerListCell.setPadding(5);
//            customerListCell.setHorizontalAlignment(Element.ALIGN_CENTER);
//            customerListCell.setBorderColor(receiptGreen_2);
//            itemsTable.addCell(customerListCell);

//            String departmentInCopy = "--";
//            if (helpDeskDTOResponse.departmentInCopy() != null && !helpDeskDTOResponse.departmentInCopy().isEmpty()) {
//                departmentInCopy = IntStream.range(0, helpDeskDTOResponse.departmentInCopy().size())
//                        .mapToObj(i -> (i + 1) + "." + helpDeskDTOResponse.departmentInCopy().get(i).name())
//                        .collect(Collectors.joining("\n"));
//            }
//
//            PdfPCell departmentInCopyCell = new PdfPCell(new Phrase(departmentInCopy, cellFont));
////            departmentInCopyCell.setPadding(5);
//            departmentInCopyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
//            departmentInCopyCell.setBorderColor(receiptGreen_2);
//            itemsTable.addCell(departmentInCopyCell);

            rowIndex++;
        }

        document.add(itemsTable);
    }

    private void addHeaderSection(Document document) throws DocumentException, IOException {
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

    public void addTitle(Document document, String titleString, String formattedDateToday, String printedBy) throws DocumentException {
        com.itextpdf.text.Font dateFont = FontFactory.getFont(FontFactory.TIMES_ITALIC, 5, new BaseColor(84, 138, 70));
        com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new BaseColor(84, 138, 70));
        Paragraph title = new Paragraph(titleString, titleFont);

        PdfPTable titleTable = new PdfPTable(1);
        titleTable.setWidthPercentage(100);
        titleTable.setSpacingBefore(20f);
        titleTable.setSpacingAfter(20f);

        PdfPCell dateCell = new PdfPCell(new Paragraph(formattedDateToday, dateFont));
        dateCell.setBorder(Rectangle.NO_BORDER);
        dateCell.setPaddingTop(6);
        dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        PdfPCell byNameCell = new PdfPCell(new Paragraph(printedBy, dateFont));
        byNameCell.setBorder(Rectangle.NO_BORDER);
        byNameCell.setPaddingBottom(8);
        byNameCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        PdfPCell titleCell = new PdfPCell(title);
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPaddingTop(6);
        titleCell.setPaddingBottom(8);
        titleCell.setHorizontalAlignment(Element.ALIGN_LEFT);

        titleCell.setCellEvent(backgroundEvent);

        titleTable.addCell(dateCell);
        titleTable.addCell(byNameCell);
        titleTable.addCell(titleCell);
        document.add(titleTable);
    }

    public ByteArrayOutputStream createExcelExport(List<HelpDeskDTOResponse> content, String name) {
        try {

            String documentTitle = "Ticket Report";

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
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 10));

            XSSFRow row4 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row4, 0, "Printed On: \t \t     " + DateFormatter.formatDate(LocalDateTime.now()), stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 10));

            XSSFRow row5 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row5, 0, "Total Items: \t \t     " + content.size(), stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 10));

            List<String> contentHeaders = Arrays.asList(
                    "ticket number",
                    "date",
                    "time",
                    "customer",
                    "issue",
                    "primary department",
                    "current department",
                    "status",
                    "phone",
                    "assigned to",
                    "customer type"
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

    public void addRecordData(List<HelpDeskDTOResponse> helpDeskDTOResponses,
                              XSSFSheet sheet,
                              int rowIndex,
                              CellStyle dataCellStyle,
                              CellStyle dataCellStyleRightAlign) {

        int colIndex;

        for (HelpDeskDTOResponse response : helpDeskDTOResponses) {
            colIndex = -1;
            XSSFRow row = sheet.createRow(++rowIndex);


            excelUtils.createStringDataCell(row, ++colIndex, response.ticketNumber(), dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, response.createdDate(), dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, response.createdTime(), dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, response.entityUserName(), dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, response.title(), dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, response.department(), dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, response.workflowStatus(), dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, response.status(), dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, response.entityPhone(), dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, response.assignedToName(), dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, response.customerType(), dataCellStyle);
        }
    }
}
