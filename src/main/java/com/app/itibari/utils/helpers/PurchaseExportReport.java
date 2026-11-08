package com.app.itibari.utils.helpers;

import com.app.itibari.models.entities.Suppliers;
import com.app.itibari.models.response.CustomerOrderDTORapper;
import com.app.itibari.models.response.CustomerOrderSummary;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PurchaseExportReport {

    private final ExcelUtils excelUtils;

    private final ReportRecordConverter reportRecordConverter;

    final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final DecimalFormat moneyFormatter = new DecimalFormat("#,##0.00;(#,##0.00)");
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0");

    public PurchaseExportReport(ExcelUtils excelUtils, ReportRecordConverter reportRecordConverter) {

        this.excelUtils = excelUtils;
        this.reportRecordConverter = reportRecordConverter;
    }

    public ByteArrayOutputStream generatePurchaseExcel(Suppliers supplier, String name, List<CustomerOrderDTORapper> content) {

        try {
            String documentTitle = String.format("PURCHASE REPORT FOR %s", supplier.getSupplierName().toUpperCase());

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
            excelUtils.createStringDataCell(row4, 0, "Printed On: " + formatter.format(LocalDateTime.now()), stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 6));

            // Headers
            List<String> contentHeaders = Arrays.asList(
                    "Customer",
                    "District",
                    "Ward",
                    "Purchase Date",
                    "Product",
                    "Sku Code",
                    "Quantity Purchased",
                    "Order No"
            );

            Row headerRow = sheet.createRow(rowIndex += 2);
            for (int i = 0; i < contentHeaders.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(contentHeaders.get(i));
                cell.setCellStyle(headerCellStyle);
            }

            ++rowIndex;

            // Data
            addPurchaseData(content, sheet, rowIndex, stringDataCellStyleLeftAlign, numericDataCellStyleRightAlign);

            for (int i = 0; i < contentHeaders.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            bos.close();

            return bos;
        } catch (IOException e) {
            throw new BadRequestRestApiException()
                    .developerMessage("Error creating Excel file")
                    .userMessage("Sorry. Error creating file");
        }
    }

    public ByteArrayOutputStream generatePurchasePdf(Suppliers supplier, String fullName, List<CustomerOrderDTORapper> content) {

        Document document = new Document(PageSize.A4.rotate(), 18f, 18f, 24f, 24f);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BaseColor receiptGreen = new BaseColor(83, 112, 76);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PdfUtil());
            document.open();


            Image logo_2 = Image.getInstance(new ClassPathResource("static/images/logo_2.png").getInputStream().readAllBytes());
            logo_2.scaleToFit(630, 200);

            float y = document.getPageSize().getHeight()
                    - document.topMargin()
                    - logo_2.getScaledHeight()
                    + 50f;

            logo_2.setAbsolutePosition(-22, y);
            writer.getDirectContent().addImage(logo_2);

            document.add(new Paragraph("\n\n\n"));

            Image logo = Image.getInstance(new ClassPathResource("static/images/logoImage_1.jpg").getInputStream().readAllBytes());
            logo.scaleToFit(120, 120);
            logo.setAlignment(Image.ALIGN_LEFT);
            logo.setIndentationLeft(-10f);
            document.add(logo);


            // title

            String titleName = String.format("PURCHASE REPORT FOR %s", supplier.getSupplierName().toUpperCase());
            Paragraph title = new Paragraph(titleName,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, receiptGreen));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(6f);
            document.add(title);


            Paragraph companyName = new Paragraph("ITIBARI TECHNOLOGIES LTD",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, receiptGreen));
            companyName.setAlignment(Element.ALIGN_LEFT);
            companyName.setSpacingBefore(8f);
            companyName.setSpacingAfter(2f);
            document.add(companyName);

            Paragraph companyAddress = new Paragraph("Mikocheni light industrial area, Dar es Salaam",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, receiptGreen));
            companyAddress.setAlignment(Element.ALIGN_LEFT);
            companyAddress.setSpacingAfter(2f);
            document.add(companyAddress);

            Paragraph companyEmail = new Paragraph("Email: commercial@itibari.io",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, receiptGreen));
            companyEmail.setAlignment(Element.ALIGN_LEFT);
            companyEmail.setSpacingAfter(8f);
            document.add(companyEmail);

            LineSeparator separator = new LineSeparator();
            separator.setLineColor(new BaseColor(190, 190, 190));
            document.add(new Chunk(separator));

            addItems(document, content);

            Paragraph printedBy = new Paragraph("Printed By: " + fullName,
                    FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK));
            printedBy.setAlignment(Element.ALIGN_LEFT);
            printedBy.setIndentationLeft(0f);
            printedBy.setSpacingBefore(8f);
            document.add(printedBy);

            Paragraph printedOn = new Paragraph("Printed On: " + formatter.format(LocalDateTime.now()),
                    FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK));
            printedOn.setAlignment(Element.ALIGN_LEFT);
            printedOn.setIndentationLeft(0f);
            document.add(printedOn);


            Paragraph sytemGenerated = new Paragraph(
                    """
                            This is a system generated document. No signature is required." 
                            "For any inquiries, please contact the company at commercial@itibari.io.
                            """,
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, BaseColor.DARK_GRAY));
            sytemGenerated.setAlignment(Element.ALIGN_CENTER);
            document.add(sytemGenerated);


            document.add(new Paragraph("\n"));


            document.close();

        } catch (Exception e) {   // catch broader to log everything
            log.error("Error generating receipt PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error generating receipt PDF", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }

        return out;
    }


    private void addItems(Document document, List<CustomerOrderDTORapper> content) throws DocumentException {

        com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.WHITE);
        com.itextpdf.text.Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

        PdfPTable lineItems = new PdfPTable(8);
        lineItems.setWidthPercentage(100);
        lineItems.setWidths(new float[]{1.8f, 1.8f, 1.8f, 1.5f, 1.8f, 1.8f, 1.8f, 1.8f});
        lineItems.setSpacingBefore(10f);
        lineItems.setSpacingAfter(6f);


        lineItems.addCell(createHeaderCell("Customer", headerFont));
        lineItems.addCell(createHeaderCell("District", headerFont));
        lineItems.addCell(createHeaderCell("Ward", headerFont));
        lineItems.addCell(createHeaderCell("Purchase Date", headerFont));
        lineItems.addCell(createHeaderCell("Product", headerFont));
        lineItems.addCell(createHeaderCell("Sku Code", headerFont));
        lineItems.addCell(createHeaderCell("Quantity Purchased", headerFont));
        lineItems.addCell(createHeaderCell("Order No", headerFont));


        List<CustomerOrderSummary> customerOrderSummaries = this.convertCustomerOrderSummary(content);

        for (CustomerOrderSummary item : customerOrderSummaries) {
            for (CustomerOrderDTORapper.InnerCustomerDetails innerItem : item.sortedOrders()) {
                // Parent row with customer info
                lineItems.addCell(makeCell(item.customerName(), cellFont));
                lineItems.addCell(makeCell(item.region(), cellFont));
                lineItems.addCell(makeCell(item.location(), cellFont));
                lineItems.addCell(makeCell(dateFormatter.format(innerItem.purchaseDate()), cellFont));
                lineItems.addCell(makeCell(innerItem.productName(), cellFont));
                lineItems.addCell(makeCell(innerItem.skuCode(), cellFont));
                lineItems.addCell(makeCell(moneyFormatter.format(innerItem.quantityPurchased()), cellFont));
                lineItems.addCell(makeCell(item.routeId(), cellFont));
            }
        }
        document.add(Chunk.NEWLINE);
        document.add(lineItems);


    }

    private PdfPCell createHeaderCell(String text, com.itextpdf.text.Font font) {
        BaseColor receiptGreen = new BaseColor(83, 112, 76);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        cell.setBackgroundColor(receiptGreen);
        cell.setBorderColor(new BaseColor(230, 230, 230));
        cell.setBorderWidth(0.8f);
        return cell;
    }

    public void addPurchaseData(List<CustomerOrderDTORapper> itemDTOList,
                                XSSFSheet sheet,
                                int rowIndex,
                                CellStyle dataCellStyle,
                                CellStyle dataCellStyleRightAlign) {

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        List<CustomerOrderSummary> customerOrderSummaries = this.convertCustomerOrderSummary(itemDTOList);

        for (CustomerOrderSummary customerOrderDTO : customerOrderSummaries) {
            for (CustomerOrderDTORapper.InnerCustomerDetails inner : customerOrderDTO.sortedOrders()) {
                int colIndex = -1;
                XSSFRow row = sheet.createRow(++rowIndex);
                excelUtils.createStringDataCell(row, ++colIndex, customerOrderDTO.customerName(), dataCellStyle);
                excelUtils.createStringDataCell(row, ++colIndex, customerOrderDTO.region(), dataCellStyle);
                excelUtils.createStringDataCell(row, ++colIndex, customerOrderDTO.location(), dataCellStyle);
                excelUtils.createStringDataCell(row, ++colIndex, dateFormatter.format(inner.purchaseDate()), dataCellStyle);
                excelUtils.createStringDataCell(row, ++colIndex, inner.productName(), dataCellStyle);
                excelUtils.createStringDataCell(row, ++colIndex, inner.skuCode(), dataCellStyle);
                excelUtils.createStringDataCell(row, ++colIndex, String.valueOf(inner.quantityPurchased()), dataCellStyleRightAlign);
                excelUtils.createStringDataCell(row, ++colIndex, customerOrderDTO.routeId(), dataCellStyle);
            }
        }
    }

    private List<CustomerOrderSummary> convertCustomerOrderSummary(List<CustomerOrderDTORapper> orders) {

        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }

        return orders.stream()
                .filter(Objects::nonNull) // Filter out null orders
                .filter(order -> order.customerName() != null) // CRITICAL: Filter out null customer names
                .filter(order -> order.innerCustomerDetailsList() != null && !order.innerCustomerDetailsList().isEmpty()) // Optional: filter out orders with no details
                .collect(Collectors.groupingBy(
                        CustomerOrderDTORapper::customerName,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    if (list.isEmpty()) return null;

                                    // Get the first occurrence for customer details
                                    CustomerOrderDTORapper first = list.get(0);

                                    // Collect and sort all inner details with null safety
                                    List<CustomerOrderDTORapper.InnerCustomerDetails> allOrders = list.stream()
                                            .filter(order -> order.innerCustomerDetailsList() != null)
                                            .flatMap(order -> order.innerCustomerDetailsList().stream())
                                            .filter(Objects::nonNull) // Filter out null inner details
                                            .sorted(Comparator.comparing(
                                                    CustomerOrderDTORapper.InnerCustomerDetails::purchaseDate,
                                                    Comparator.nullsLast(Comparator.naturalOrder()) // Handle null dates
                                            ).reversed()) // DESC order
                                            .collect(Collectors.toList());

                                    return new CustomerOrderSummary(
                                            first.customerName(),
                                            first.routeId() != null ? first.routeId() : "",
                                            first.region() != null ? first.region() : "",
                                            first.location() != null ? first.location() : "",
                                            first.address() != null ? first.address() : "",
                                            allOrders
                                    );
                                }
                        )
                ))
                .values()
                .stream()
                .filter(Objects::nonNull) // Filter out any null summaries
                .filter(summary -> !summary.sortedOrders().isEmpty()) // Optional: only include customers with orders
                .collect(Collectors.toList());
    }

    // Helper methods
    private PdfPCell makeCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(5);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private PdfPCell emptyCell(Font font) {
        PdfPCell cell = new PdfPCell(new Phrase("", font));
        cell.setPadding(5);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
}
