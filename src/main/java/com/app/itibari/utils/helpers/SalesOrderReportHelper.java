package com.app.itibari.utils.helpers;

import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.SalesOrders;
import com.app.itibari.models.response.SalesAgentOrderPageable;
import com.app.itibari.models.response.SalesOrderResponse;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesOrderReportHelper {

    private final PersonRepository personRepository;

    private final ExcelUtils excelUtils;

    private final PdfUtil pdfUtil;

    public ByteArrayOutputStream createSalesOrdersReportPDF(List<SalesOrders> content, String name) {

        try {
            Person downloadedBy = personRepository.findByEmailAndDeletedIsFalse(name)
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Person with name " + name + " not found")
                            .userMessage("Person not found"));

            String subtitle = "Sales Order Report";

            // Practically no margins
            Document document = new Document(PageSize.A3.rotate(), 10f, 10f, 50f, 40f);

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            PdfWriter writer = PdfWriter.getInstance(document, out);
            PdfUtil event = new PdfUtil();
            writer.setPageEvent(event);
            document.open();

            Font datePrinted = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.BLACK);
            Paragraph date_printed = new Paragraph(LocalDate.now().toString(), datePrinted);
            date_printed.setAlignment(Element.ALIGN_RIGHT);
            date_printed.setSpacingAfter(5);
            document.add(date_printed);

            Font companyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.BLACK);
            Paragraph companyHeader = new Paragraph(String.format("Printed By %s", name), companyFont);
            companyHeader.setAlignment(Element.ALIGN_RIGHT);
            companyHeader.setSpacingAfter(5);
            document.add(companyHeader);

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.GREEN);
            Paragraph title = new Paragraph(subtitle, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Divider line
            Paragraph divider = new Paragraph("_____________________________________________________________");
            divider.setAlignment(Element.ALIGN_CENTER);
            divider.setSpacingAfter(20);
            document.add(divider);

            // Document title section

            // Table headers
            document.add(new Paragraph(" "));

            PdfPTable contentTable = new PdfPTable(13);
            contentTable.setWidthPercentage(100);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            headerFont.setStyle(Font.BOLD);
            Font contentFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            String[] contentHeaders = {
                    "Order Date",
                    "Order No",
                    "Customer",
                    "Region",
                    "Zone",
                    "Sales Channel",
                    "No of Items",
                    "Order Total",
                    "Delivery Date",
                    "Booked By",
                    "Delivery Agent",
                    "Status",
                    "Due Days"
            };
            for (String header : contentHeaders) {
                PdfPCell headerCell = new PdfPCell(new Phrase(header, headerFont));
                headerCell.setBorder(PdfPCell.NO_BORDER);
                headerCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                contentTable.addCell(headerCell);
            }

            for (SalesOrders salesOrder : content) {

                String creationTime = salesOrder.getCreationTime() != null ? formatDate1(salesOrder.getCreationTime().toString())
                        : "--";
                String salesOrderNumber = salesOrder.getSalesOrderNumber() != null ?
                        salesOrder.getSalesOrderNumber() : "--";
                String customerName = salesOrder.getCustomer() != null && salesOrder.getCustomer().getName() != null
                        ? salesOrder.getCustomer().getName() : "--";
                String regionName = salesOrder.getOrganizationRegions() != null ?
                        salesOrder.getOrganizationRegions().getName() : "--";
                String zoneName = salesOrder.getCustomer() != null && salesOrder.getCustomer().getOrganizationLocation() != null
                        ? salesOrder.getCustomer().getOrganizationLocation().getName() : "--";
                String sourceChannel = salesOrder.getSourceChannel() != null ? salesOrder.getSourceChannel().getLabel() :
                        "--";
                int numberOfItems = salesOrder.getSalesOrderItems() != null ? salesOrder.getSalesOrderItems().size() : 0;
                String totalAmount = salesOrder.getTotalAmount() != null ? String.valueOf(salesOrder.getTotalAmount()) : "0.00";
                String updateDate = salesOrder.getExpectedDeliveryDate() != null ?
                        formatDate(salesOrder.getExpectedDeliveryDate().toString()) : "--";
                String bookedBy = salesOrder.getBookedBy() != null ? salesOrder.getBookedBy() : "--";
                String deliveryAgentName = salesOrder.getDeliveryAgent() != null ?
                        salesOrder.getDeliveryAgent().getFullName() : "--";
                String deliveryStatus = salesOrder.getDeliveryStatus() != null ?
                        salesOrder.getDeliveryStatus().name() : "--";

                PdfPCell statusCell = new PdfPCell(new Phrase(creationTime, contentFont));
                statusCell.setBorder(PdfPCell.NO_BORDER);
                statusCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                contentTable.addCell(statusCell);

                PdfPCell orderNoCell = new PdfPCell(new Phrase(salesOrderNumber, contentFont));
                orderNoCell.setBorder(PdfPCell.NO_BORDER);
                orderNoCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                contentTable.addCell(orderNoCell);

                PdfPCell customerCell = new PdfPCell(new Phrase(customerName, contentFont));
                customerCell.setBorder(PdfPCell.NO_BORDER);
                customerCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                contentTable.addCell(customerCell);

                PdfPCell regionCell = new PdfPCell(new Phrase(regionName, contentFont));
                regionCell.setBorder(PdfPCell.NO_BORDER);
                regionCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                contentTable.addCell(regionCell);

                PdfPCell zoneCell = new PdfPCell(new Phrase(zoneName, contentFont));
                zoneCell.setBorder(PdfPCell.NO_BORDER);
                zoneCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                contentTable.addCell(zoneCell);

                PdfPCell salesChannelCell = new PdfPCell(new Phrase(sourceChannel, contentFont));
                salesChannelCell.setBorder(PdfPCell.NO_BORDER);
                salesChannelCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                contentTable.addCell(salesChannelCell);

                PdfPCell noOfItemsCell = new PdfPCell(new Phrase(String.valueOf(numberOfItems), contentFont));
                noOfItemsCell.setBorder(PdfPCell.NO_BORDER);
                noOfItemsCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                contentTable.addCell(noOfItemsCell);

                PdfPCell orderTotalCell = new PdfPCell(new Phrase(totalAmount, contentFont));
                orderTotalCell.setBorder(PdfPCell.NO_BORDER);
                orderTotalCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                contentTable.addCell(orderTotalCell);

                PdfPCell deliveryDateCell = new PdfPCell(new Phrase(updateDate, contentFont));
                deliveryDateCell.setBorder(PdfPCell.NO_BORDER);
                deliveryDateCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                contentTable.addCell(deliveryDateCell);

                PdfPCell bookedByCell = new PdfPCell(new Phrase(bookedBy, contentFont));
                bookedByCell.setBorder(PdfPCell.NO_BORDER);
                bookedByCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                contentTable.addCell(bookedByCell);

                PdfPCell deliveryAgentCell = new PdfPCell(new Phrase(deliveryAgentName, contentFont));
                deliveryAgentCell.setBorder(PdfPCell.NO_BORDER);
                deliveryAgentCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                contentTable.addCell(deliveryAgentCell);

                PdfPCell statusCell2 = new PdfPCell(new Phrase(deliveryStatus, contentFont));
                statusCell2.setBorder(PdfPCell.NO_BORDER);
                statusCell2.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                contentTable.addCell(statusCell2);

                PdfPCell dueDaysCell2 = new PdfPCell(new Phrase(salesOrder.getDueDays(), contentFont));
                dueDaysCell2.setBorder(PdfPCell.NO_BORDER);
                dueDaysCell2.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                contentTable.addCell(dueDaysCell2);
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

    public ByteArrayOutputStream createSalesOrdersReportExcel(List<SalesOrders> content, String name) {

        try {

            String documentTitle = "Sales Order Report";

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
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 11));

            XSSFRow row3 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row3,
                    0,
                    "Printed By: \t \t     " + name,
                    stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 11));

            XSSFRow row4 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row4, 0, "Printed On: \t \t     " + LocalDateTime.now(), stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 11));

            // add stats

            XSSFRow row5 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row5, 0, "Total Orders: \t \t     " + content.size(), stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 11));

            // total order value

            XSSFRow row6 = sheet.createRow(++rowIndex);
            BigDecimal totalOrderValue = content.stream()
                    .map(SalesOrders::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            excelUtils.createStringDataCell(row6, 0, "Total Order Value: \t \t     " + totalOrderValue, stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 11));

            List<String> contentHeaders = Arrays.asList(
                    "Order Date",
                    "Order No",
                    "Customer",
                    "Region",
                    "Zone",
                    "Sales Channel",
                    "No of Items",
                    "Order Total",
                    "Delivery Date",
                    "Booked By",
                    "Delivery Agent",
                    "Status",
                    "Due Days"
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

    public void addRecordData(List<SalesOrders> orders,
                              XSSFSheet sheet,
                              int rowIndex,
                              CellStyle dataCellStyle,
                              CellStyle dataCellStyleRightAlign) {

        int colIndex;

        for (SalesOrders salesOrder : orders) {
            colIndex = -1;
            XSSFRow row = sheet.createRow(++rowIndex);

            String creationTime = salesOrder.getCreationTime() != null ?
                    formatDate1(salesOrder.getCreationTime().toString()) : "--";
            String salesOrderNumber = salesOrder.getSalesOrderNumber() != null ? salesOrder.getSalesOrderNumber() :
                    "--";
            String customerName = salesOrder.getCustomer() != null && salesOrder.getCustomer().getName() != null
                    ? salesOrder.getCustomer().getName() : "--";
            String regionName = salesOrder.getOrganizationRegions() != null ?
                    salesOrder.getOrganizationRegions().getName() : "--";
            String zoneName = salesOrder.getCustomer() != null && salesOrder.getCustomer().getOrganizationLocation() != null
                    ? salesOrder.getCustomer().getOrganizationLocation().getName() : "--";
            String sourceChannel = salesOrder.getSourceChannel() != null ? salesOrder.getSourceChannel().getLabel() : "--";
            int numberOfItems = salesOrder.getSalesOrderItems() != null ? salesOrder.getSalesOrderItems().size() : 0;
            String totalAmount = salesOrder.getTotalAmount() != null ? String.valueOf(salesOrder.getTotalAmount()) : "0.00";
            String updateDate = salesOrder.getExpectedDeliveryDate() != null ?
                    formatDate(salesOrder.getExpectedDeliveryDate().toString()) : "--";
            String bookedBy = salesOrder.getBookedBy() != null ? salesOrder.getBookedBy() : "--";
            String deliveryAgentName = salesOrder.getDeliveryAgent() != null ?
                    salesOrder.getDeliveryAgent().getFullName() : "--";
            String deliveryStatus = salesOrder.getDeliveryStatus() != null ? salesOrder.getDeliveryStatus().name() :
                    "--";
            excelUtils.createStringDataCell(row, ++colIndex, creationTime, dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, salesOrderNumber, dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, customerName, dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, regionName, dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, zoneName, dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, sourceChannel, dataCellStyle);
            excelUtils.createNumericDataCell(row, ++colIndex, String.valueOf(numberOfItems), dataCellStyle);
            excelUtils.createNumericDataCell(row, ++colIndex, totalAmount, dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, updateDate, dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, bookedBy, dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, deliveryAgentName, dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, deliveryStatus, dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, salesOrder.getDueDays(), dataCellStyle);
        }
    }

    public String formatDate1(String dateValue) {
        LocalDateTime dateTime = LocalDateTime.parse(dateValue);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm");
        return dateTime.format(formatter);
    }

    public String formatDate(String dateValue) {
        LocalDate date = LocalDate.parse(dateValue);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        return date.format(formatter);
    }

    public ByteArrayOutputStream generateSalesAgentPdf(SalesAgentOrderPageable salesAgentOrderPageable) {
        try {
            String documentTitle = "Sales Agent Order Summary";

            Document document = new Document(PageSize.A3.rotate(), 10f, 10f, 50f, 40f);
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // Calculate totals from the pageable object
            BigDecimal totalAmount = salesAgentOrderPageable.getTotalAmount() != null ?
                    salesAgentOrderPageable.getTotalAmount() : BigDecimal.ZERO;
            long totalOrders = salesAgentOrderPageable.getTotalElements();
            String agentName = salesAgentOrderPageable.getAgent() != null ?
                    salesAgentOrderPageable.getAgent() : "N/A";
            String fromDate = salesAgentOrderPageable.getFrom() != null ?
                    salesAgentOrderPageable.getFrom() : "N/A";
            String toDate = salesAgentOrderPageable.getTo() != null ?
                    salesAgentOrderPageable.getTo() : "N/A";

            // Create writer with footer event
            PdfWriter writer = PdfWriter.getInstance(document, out);

            // Custom page event for footer with totals
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    try {
                        drawSalesAgentFooter(writer, document, totalAmount, totalOrders, agentName);
                    } catch (Exception e) {
                        log.error("Error drawing footer: {}", e.getMessage());
                    }
                }
            });

            document.open();

            // Header - Printed By
            Font datePrinted = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.BLACK);
            Paragraph printedOn = new Paragraph("Printed On: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), datePrinted);
            printedOn.setAlignment(Element.ALIGN_RIGHT);
            printedOn.setSpacingAfter(5);
            document.add(printedOn);

            Font companyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.BLACK);
            Paragraph printedBy = new Paragraph("Printed By: " + agentName, companyFont);
            printedBy.setAlignment(Element.ALIGN_RIGHT);
            printedBy.setSpacingAfter(5);
            document.add(printedBy);

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new BaseColor(83, 112, 76));
            Paragraph title = new Paragraph(documentTitle, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            // Date Range
            Font rangeFont = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.DARK_GRAY);
            Paragraph dateRange = new Paragraph("Date Range: " + fromDate + " to " + toDate, rangeFont);
            dateRange.setAlignment(Element.ALIGN_CENTER);
            dateRange.setSpacingAfter(15);
            document.add(dateRange);

            // Divider line
            LineSeparator separator = new LineSeparator();
            separator.setLineColor(new BaseColor(190, 190, 190));
            document.add(new Chunk(separator));
            document.add(new Paragraph(" "));

            // Create table with 6 columns
            PdfPTable contentTable = new PdfPTable(6);
            contentTable.setWidthPercentage(100);
            contentTable.setWidths(new float[]{1.5f, 1.5f, 2.5f, 1.8f, 1.8f, 1.5f});
            contentTable.setSpacingBefore(10f);
            contentTable.setSpacingAfter(10f);

            // Table Headers
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
            BaseColor headerBg = new BaseColor(83, 112, 76);

            String[] contentHeaders = {
                    "#", "Order Date", "Order No", "Customer", "Total Value", "Status"
            };

            for (String header : contentHeaders) {
                PdfPCell headerCell = new PdfPCell(new Phrase(header, headerFont));
                headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                headerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                headerCell.setPadding(8f);
                headerCell.setBackgroundColor(headerBg);
                headerCell.setBorderColor(new BaseColor(200, 200, 200));
                headerCell.setBorderWidth(0.8f);
                contentTable.addCell(headerCell);
            }

            // Table Data
            Font contentFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            List<SalesOrderResponse> orders = salesAgentOrderPageable.getContent();

            if (orders != null && !orders.isEmpty()) {
                int counter = 1;
                boolean evenRow = false;

                for (SalesOrderResponse order : orders) {
                    BaseColor rowColor = evenRow ? new BaseColor(248, 250, 248) : BaseColor.WHITE;

                    // Serial Number
                    contentTable.addCell(createTableCell(String.valueOf(counter++), contentFont,
                            Element.ALIGN_CENTER, rowColor));

                    // Order Date
                    contentTable.addCell(createTableCell(
                            order.getCreationDate() != null ? order.getCreationDate() : "N/A",
                            contentFont, Element.ALIGN_CENTER, rowColor));

                    // Order Number
                    contentTable.addCell(createTableCell(
                            order.getOrderNumber() != null ? order.getOrderNumber() : "N/A",
                            contentFont, Element.ALIGN_CENTER, rowColor));

                    // Customer
                    contentTable.addCell(createTableCell(
                            order.getCustomer() != null ? order.getCustomer() : "N/A",
                            contentFont, Element.ALIGN_LEFT, rowColor));

                    // Total Value
                    String amount = order.getTotalAmount() != null ?
                            "TSh " + String.format("%,.2f", order.getTotalAmount()) : "TSh 0.00";
                    contentTable.addCell(createTableCell(amount, contentFont,
                            Element.ALIGN_RIGHT, rowColor));

                    // Status with color coding
                    PdfPCell statusCell = createTableCell(
                            order.getStatus() != null ? order.getStatus() : "N/A",
                            contentFont, Element.ALIGN_CENTER, rowColor);

                    // Color code status
                    if (order.getStatus() != null) {
                        String status = order.getStatus().toLowerCase();
                        if (status.contains("completed") || status.contains("delivered")) {
                            statusCell.setBackgroundColor(new BaseColor(200, 230, 200));
                        } else if (status.contains("pending") || status.contains("processing")) {
                            statusCell.setBackgroundColor(new BaseColor(255, 230, 200));
                        } else if (status.contains("cancelled") || status.contains("failed")) {
                            statusCell.setBackgroundColor(new BaseColor(255, 200, 200));
                        }else {
                            statusCell.setBackgroundColor(BaseColor.WHITE);
                        }
                    }
                    contentTable.addCell(statusCell);

                    evenRow = !evenRow;
                }
            } else {
                // No data row
                PdfPCell noDataCell = new PdfPCell(new Phrase("No orders found", contentFont));
                noDataCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                noDataCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                noDataCell.setPadding(20f);
                noDataCell.setColspan(6);
                noDataCell.setBorderColor(new BaseColor(230, 230, 230));
                contentTable.addCell(noDataCell);
            }

            document.add(contentTable);

            // Summary Section
            Paragraph summaryTitle = new Paragraph("SUMMARY",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new BaseColor(83, 112, 76)));
            summaryTitle.setAlignment(Element.ALIGN_RIGHT);
            summaryTitle.setSpacingBefore(15f);
            document.add(summaryTitle);

            // Summary Table (right aligned)
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(30);
            summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summaryTable.setSpacingBefore(5f);
            summaryTable.setSpacingAfter(15f);

            Font summaryLabelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.BLACK);
            Font summaryValueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);

            // Total Orders
            summaryTable.addCell(createSummaryCell("Total Orders:", summaryLabelFont, Element.ALIGN_LEFT));
            summaryTable.addCell(createSummaryCell(String.valueOf(totalOrders), summaryValueFont, Element.ALIGN_RIGHT));

            // Total Value
            summaryTable.addCell(createSummaryCell("Total Value:", summaryLabelFont, Element.ALIGN_LEFT));
            summaryTable.addCell(createSummaryCell("TSh " + String.format("%,.2f", totalAmount), summaryValueFont, Element.ALIGN_RIGHT));

            document.add(summaryTable);

            // Footer note
            Paragraph footerNote = new Paragraph(
                    "This is a system generated document. No signature is required. " +
                            "For any inquiries, please contact the company at commercial@itibari.io.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, BaseColor.DARK_GRAY));
            footerNote.setAlignment(Element.ALIGN_CENTER);
            footerNote.setSpacingBefore(10f);
            document.add(footerNote);

            document.close();
            return out;

        } catch (Exception e) {
            log.error("Error generating Sales Agent PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error generating Sales Agent PDF", e);
        } finally {
            // Ensure document is closed
        }
    }

    /**
     * Helper method to draw footer with totals on each page
     */
    private void drawSalesAgentFooter(PdfWriter writer, Document document,
                                      BigDecimal totalAmount, long totalOrders, String agentName) {
        try {
            PdfContentByte cb = writer.getDirectContent();
            BaseColor footerColor = new BaseColor(83, 112, 76);

            float pageWidth = document.getPageSize().getWidth();
            float footerY = document.bottomMargin() - 15f;

            // Draw separator line
            cb.setColorStroke(new BaseColor(190, 190, 190));
            cb.setLineWidth(0.8f);
            cb.moveTo(document.leftMargin(), footerY + 10f);
            cb.lineTo(pageWidth - document.rightMargin(), footerY + 10f);
            cb.stroke();

            // Page number on left
            String pageText = "Page " + writer.getPageNumber() + " of " + writer.getCurrentPageNumber();
            cb.beginText();
            cb.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED), 9);
            cb.setColorFill(BaseColor.BLACK);
            cb.showTextAligned(Element.ALIGN_LEFT, pageText,
                    document.leftMargin(), footerY, 0);
            cb.endText();

            // Agent name in center
            cb.beginText();
            cb.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED), 9);
            cb.setColorFill(BaseColor.DARK_GRAY);
            cb.showTextAligned(Element.ALIGN_CENTER, "Agent: " + agentName,
                    pageWidth / 2, footerY, 0);
            cb.endText();

            // Totals on right
            String totalText = String.format("Total Value: TSh %,.2f  |  Orders: %d",
                    totalAmount, totalOrders);
            cb.beginText();
            cb.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED), 9);
            cb.setColorFill(footerColor);
            cb.showTextAligned(Element.ALIGN_RIGHT, totalText,
                    pageWidth - document.rightMargin(), footerY, 0);
            cb.endText();

        } catch (Exception e) {
            log.error("Error in sales agent footer: {}", e.getMessage());
        }
    }

    /**
     * Helper method to create table cell
     */
    private PdfPCell createTableCell(String text, Font font, int alignment, BaseColor backgroundColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        cell.setMinimumHeight(20f);
        cell.setBackgroundColor(backgroundColor);
        cell.setBorderColor(new BaseColor(230, 230, 230));
        cell.setBorderWidth(0.5f);
        return cell;
    }

    /**
     * Helper method to create summary cell
     */
    private PdfPCell createSummaryCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5f);
        cell.setBorderColor(new BaseColor(230, 230, 230));
        cell.setBorderWidth(0.5f);
        return cell;
    }
}
