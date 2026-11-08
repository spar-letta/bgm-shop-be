package com.app.itibari.utils.reportHelper;

import com.app.itibari.models.entities.OrderAmendment;
import com.app.itibari.models.response.DisposedQuarantineProducts;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.ExcelUtils;
import com.app.itibari.utils.helpers.RoundedBackgroundCellEvent;
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
import java.security.Principal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderAmendmentExport {

    private static final String company = "Itibari Technologies Limited";

    private static final String companyAddress = "Plot 544/1, Mikocheni light industrial area, Dar es Salaam";

    private static final String companyEmail = "Email: commercial@itibari.io";

    private final RoundedBackgroundCellEvent backgroundEvent;
    private final ExcelUtils excelUtils;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0");


    public ByteArrayOutputStream createExcel(List<OrderAmendment> content, Principal principal) {
        try {
            String documentTitle = "Order Amendment Report";

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
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 7));

            // Printed By
            XSSFRow row3 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row3, 0, "Printed By: " + principal.getName(), stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 7));

            // Printed On
            XSSFRow row4 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row4, 0, "Printed On: " + formatter.format(LocalDateTime.now()), stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 7));
            // Headers
            List<String> contentHeaders = Arrays.asList(
                    "ROUTE ID",
                    "ORDER NUMBER",
                    "CUSTOMER NAME",
                    "DELIVERY AGENT",
                    "AMENDMENT REASON",
                    "APPROVAL REASON",
                    "ORDER TOTAL"
            );

            Row headerRow = sheet.createRow(rowIndex += 2);
            for (int i = 0; i < contentHeaders.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(contentHeaders.get(i));
                cell.setCellStyle(headerCellStyle);
            }

            ++rowIndex;

            // Add parent + child data
            addRecordData(workbook, content, sheet, rowIndex, stringDataCellStyleLeftAlign, numericDataCellStyleRightAlign);

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

    private void addRecordData(XSSFWorkbook workbook, List<OrderAmendment> content, XSSFSheet sheet, int rowIndex, CellStyle stringDataCellStyleLeftAlign, CellStyle numericDataCellStyleRightAlign) {
        CellStyle parentRowStyle = workbook.createCellStyle();
        parentRowStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());

        Font parentFont = workbook.createFont();
        parentFont.setBold(true);
        parentRowStyle.setFont(parentFont);

        for (OrderAmendment orderAmendment : content) {

            XSSFRow parentRow = sheet.createRow(++rowIndex);
            int colIndex = -1;

            String routeId = orderAmendment.getRouteId();
            String orderNumber = orderAmendment.getSalesOrder().getSalesOrderNumber();
            String customerName = orderAmendment.getSalesOrder().getCustomer().getName();
            String daName = orderAmendment.getSalesOrder().getDeliveryAgent() != null ? orderAmendment.getSalesOrder().getDeliveryAgent().getFullName():"--";
            String reasonCode = orderAmendment.getRejectionsCode() != null ? orderAmendment.getRejectionsCode().getCode() : "--";
            String description = orderAmendment.getDescription();
            String orderTotal = orderAmendment.getAmendedTotal() != null ? orderAmendment.getAmendedTotal().toString() : "0";

            excelUtils.createStringDataCell(parentRow, ++colIndex, routeId, parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, orderNumber, parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, customerName, parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, daName, parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, reasonCode, parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, description, parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, orderTotal, parentRowStyle);
        }
    }

    public ByteArrayOutputStream generateLeadPdf(List<OrderAmendment> content, Principal principal) throws DocumentException, IOException {
        String title = "Order Amendment Report";


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

    private void addItemsTable(Document document, List<OrderAmendment> content) throws DocumentException {
        BaseColor receiptGreen = new BaseColor(84, 138, 70);
        BaseColor receiptGreen_2 = BaseColor.LIGHT_GRAY;
        BaseColor receiptWhite = BaseColor.WHITE;
        LineSeparator line = new LineSeparator();
        line.setLineColor(BaseColor.LIGHT_GRAY);

        PdfPTable itemsTable = new PdfPTable(8);
        itemsTable.setWidthPercentage(100);
        itemsTable.setWidths(new float[]{1, 5, 3, 3, 3, 3, 3, 3});
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

        PdfPCell header_1 = new PdfPCell(new Phrase("Route Id", headerFont));
        header_1.setPadding(5);
        header_1.setBorder(Rectangle.NO_BORDER);
        header_1.setBackgroundColor(receiptGreen);
        header_1.setHorizontalAlignment(Element.ALIGN_CENTER);
        itemsTable.addCell(header_1);

        PdfPCell header_2 = new PdfPCell(new Phrase("Order No", headerFont));
        header_2.setPadding(5);
        header_2.setBorder(Rectangle.NO_BORDER);
        header_2.setBackgroundColor(receiptGreen);
        header_2.setHorizontalAlignment(Element.ALIGN_CENTER);
        itemsTable.addCell(header_2);

        PdfPCell header_3 = new PdfPCell(new Phrase("Customer Name", headerFont));
        header_3.setPadding(5);
        header_3.setBorder(Rectangle.NO_BORDER);
        header_3.setBackgroundColor(receiptGreen);
        header_3.setHorizontalAlignment(Element.ALIGN_CENTER);
        itemsTable.addCell(header_3);

        PdfPCell header_4 = new PdfPCell(new Phrase("Delivery Agent", headerFont));
        header_4.setPadding(5);
        header_4.setBorder(Rectangle.NO_BORDER);
        header_4.setBackgroundColor(receiptGreen);
        header_4.setHorizontalAlignment(Element.ALIGN_CENTER);
        itemsTable.addCell(header_4);

        PdfPCell header_5 = new PdfPCell(new Phrase("Amendment Reason", headerFont));
        header_5.setPadding(5);
        header_5.setBorder(Rectangle.NO_BORDER);
        header_5.setBackgroundColor(receiptGreen);
        header_5.setHorizontalAlignment(Element.ALIGN_CENTER);
        itemsTable.addCell(header_5);

        PdfPCell header_6 = new PdfPCell(new Phrase("Approval Reason", headerFont));
        header_6.setPadding(5);
        header_6.setBorder(Rectangle.NO_BORDER);
        header_6.setBackgroundColor(receiptGreen);
        header_6.setHorizontalAlignment(Element.ALIGN_CENTER);
        itemsTable.addCell(header_6);

        PdfPCell header_7 = new PdfPCell(new Phrase("Total", headerFont));
        header_7.setPadding(5);
        header_7.setBorder(Rectangle.NO_BORDER);
        header_7.setBackgroundColor(receiptGreen);
        header_7.setHorizontalAlignment(Element.ALIGN_CENTER);
        itemsTable.addCell(header_7);


        int rowIndex = 0;

        for (OrderAmendment orderAmendment : content) {
            String routeId = orderAmendment.getRouteId();
            String orderNumber = orderAmendment.getSalesOrder().getSalesOrderNumber();
            String customerName = orderAmendment.getSalesOrder().getCustomer().getName();
            String daName = orderAmendment.getSalesOrder().getDeliveryAgent() != null ? orderAmendment.getSalesOrder().getDeliveryAgent().getFullName():"--";
            String reasonCode = orderAmendment.getRejectionsCode() != null ? orderAmendment.getRejectionsCode().getCode() : "--";
            String description = orderAmendment.getDescription();
            String orderTotal = orderAmendment.getAmendedTotal() != null ? orderAmendment.getAmendedTotal().toString() : "0";

            PdfPCell numberCell = new PdfPCell(new Phrase(String.valueOf(rowIndex + 1), cellFont));
            numberCell.setPadding(5);
            numberCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            numberCell.setBorderColor(receiptGreen);
            itemsTable.addCell(numberCell);

            PdfPCell Cell_1 = new PdfPCell(new Phrase(routeId, cellFont));
            Cell_1.setPadding(5);
            Cell_1.setHorizontalAlignment(Element.ALIGN_CENTER);
            Cell_1.setBorderColor(receiptGreen_2);
            itemsTable.addCell(Cell_1);

            PdfPCell Cell_2 = new PdfPCell(new Phrase(orderNumber, cellFont));
            Cell_2.setPadding(5);
            Cell_2.setHorizontalAlignment(Element.ALIGN_CENTER);
            Cell_2.setBorderColor(receiptGreen_2);
            itemsTable.addCell(Cell_2);

            PdfPCell Cell_3 = new PdfPCell(new Phrase(customerName, cellFont));
            Cell_3.setPadding(5);
            Cell_3.setHorizontalAlignment(Element.ALIGN_CENTER);
            Cell_3.setBorderColor(receiptGreen_2);
            itemsTable.addCell(Cell_3);

            PdfPCell Cell_4 = new PdfPCell(new Phrase(daName, cellFont));
            Cell_4.setPadding(5);
            Cell_4.setHorizontalAlignment(Element.ALIGN_CENTER);
            Cell_4.setBorderColor(receiptGreen_2);
            itemsTable.addCell(Cell_4);

            PdfPCell Cell_5 = new PdfPCell(new Phrase(reasonCode, cellFont));
            Cell_5.setPadding(5);
            Cell_5.setHorizontalAlignment(Element.ALIGN_CENTER);
            Cell_5.setBorderColor(receiptGreen_2);
            itemsTable.addCell(Cell_5);

            PdfPCell Cell_6 = new PdfPCell(new Phrase(description, cellFont));
            Cell_6.setPadding(5);
            Cell_6.setHorizontalAlignment(Element.ALIGN_CENTER);
            Cell_6.setBorderColor(receiptGreen_2);
            itemsTable.addCell(Cell_6);

            PdfPCell Cell_7 = new PdfPCell(new Phrase(orderTotal, cellFont));
            Cell_7.setPadding(5);
            Cell_7.setHorizontalAlignment(Element.ALIGN_CENTER);
            Cell_7.setBorderColor(receiptGreen_2);
            itemsTable.addCell(Cell_7);

            rowIndex++;
        }

        document.add(itemsTable);
    }
}
