package com.app.itibari.utils.helpers;

import com.app.itibari.models.entities.GoodsReturnDeliveryNote;
import com.app.itibari.models.entities.ReturnedProductItem;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Component
public class GrtnPdf {
    private final ExcelUtils excelUtils;

    private static final String company = "Itibari Technologies Limited";

    private static final String companyAddress = "Plot 544/1, Mikocheni light industrial area, Dar es Salaam";

    private static final String companyTin = "TIN: 176-727-497";

    private static final String companyVrn = "VRN: 40-321297-M";

    private static final String companyPhone = "Phone: +255 123 456 789";

    private static final String companyEmail = "Email: commercial@itibari.io";

    final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm");

    private final RoundedBackgroundCellEvent backgroundEvent;

    public GrtnPdf(ExcelUtils excelUtils, RoundedBackgroundCellEvent backgroundEvent) {
        this.excelUtils = excelUtils;
        this.backgroundEvent = backgroundEvent;
    }

    public ByteArrayOutputStream createGoodsReturnedPdf(GoodsReturnDeliveryNote goodsReturnNote) throws DocumentException, IOException {
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
        addTitle(document);
        addSupplierAndPoDetails(document, goodsReturnNote);
        addItemsTable(document, goodsReturnNote);
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

        Font companyNameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, receiptGreen);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, receiptGreen);

        Paragraph companyName = new Paragraph(company, normalFont);
        companyCell.addElement(companyName);

        Paragraph addressInfo = new Paragraph(companyAddress, normalFont);
        companyCell.addElement(addressInfo);

        Paragraph emailInfo = new Paragraph(companyEmail, normalFont);
        companyCell.addElement(emailInfo);

        Paragraph tinInfo = new Paragraph(companyTin, normalFont);
        companyCell.addElement(tinInfo);

        Paragraph vrnInfo = new Paragraph(companyVrn, normalFont);
        companyCell.addElement(vrnInfo);

        headerTable.addCell(companyCell);

        document.add(headerTable);
    }

    public void addTitle(Document document) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new BaseColor(84, 138, 70));
        Paragraph title = new Paragraph("Goods Returned Note", titleFont);

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

    private void addSupplierAndPoDetails(Document document, GoodsReturnDeliveryNote goodsReturnDeliveryNote) throws DocumentException {

        PdfPTable detailsTable = new PdfPTable(2);
        detailsTable.setWidthPercentage(100);
        detailsTable.setSpacingAfter(20);
        detailsTable.setWidths(new float[]{4f, 1.1f});
        BaseColor receiptGreen = new BaseColor(84, 138, 70);

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, receiptGreen);
        Font normalFont_2 = FontFactory.getFont(FontFactory.HELVETICA, 9, receiptGreen);

        // Supplier details
        PdfPCell supplierCell = new PdfPCell();
        supplierCell.setBorder(Rectangle.NO_BORDER);

        Paragraph supplierHeader = new Paragraph("Supplier Details:", headerFont);
        supplierCell.addElement(supplierHeader);

        Chunk label = new Chunk("Supplier Name: ", normalFont);
        Chunk value = new Chunk(goodsReturnDeliveryNote.getSupplier().getSupplierName() != null ? goodsReturnDeliveryNote.getSupplier().getSupplierName().toUpperCase() : "-", normalFont_2);
        Paragraph supplierName = new Paragraph();
        supplierName.add(label);
        supplierName.add(value);
        supplierCell.addElement(supplierName);

        Chunk label_Address = new Chunk("Address: ", normalFont);
        Chunk value_Address = new Chunk(goodsReturnDeliveryNote.getSupplier().getPostalAddress() != null ? goodsReturnDeliveryNote.getSupplier().getPostalAddress() : "-", normalFont_2);
        Paragraph address = new Paragraph();
        address.add(label_Address);
        address.add(value_Address);
        supplierCell.addElement(address);

        Chunk label_Phone = new Chunk("Phone: ", normalFont);
        Chunk value_Phone = new Chunk(goodsReturnDeliveryNote.getSupplier().getPhoneNumber() != null ? goodsReturnDeliveryNote.getSupplier().getPhoneNumber() : "-", normalFont_2);
        Paragraph phone = new Paragraph();
        phone.add(label_Phone);
        phone.add(value_Phone);
        supplierCell.addElement(phone);

        Chunk label_Email = new Chunk("Email: ", normalFont);
        Chunk value_Email = new Chunk(goodsReturnDeliveryNote.getSupplier().getEmail() != null ? goodsReturnDeliveryNote.getSupplier().getEmail() : "-", normalFont_2);
        Paragraph email = new Paragraph();
        email.add(label_Email);
        email.add(value_Email);
        supplierCell.addElement(email);

        detailsTable.addCell(supplierCell);

        // PO details
        PdfPCell poCell = new PdfPCell();
        poCell.setBorder(Rectangle.NO_BORDER);

        poCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Paragraph poHeader = new Paragraph("Goods Returned Details", headerFont);
        poHeader.setAlignment(Element.ALIGN_LEFT);
        poCell.addElement(poHeader);

        Chunk label_po_no = new Chunk("Grtn No: ", normalFont);
        Chunk label_po_no_value = new Chunk(goodsReturnDeliveryNote.getGoodsReturnNoteNumber() != null ? goodsReturnDeliveryNote.getGoodsReturnNoteNumber() : "-", normalFont_2);
        Paragraph po_no = new Paragraph();
        po_no.setAlignment(Element.ALIGN_LEFT);
        po_no.add(label_po_no);
        po_no.add(label_po_no_value);
        poCell.addElement(po_no);

        Chunk label_date = new Chunk("Date: ", normalFont);
        Chunk label_date_value = new Chunk(goodsReturnDeliveryNote.getCreationTime() != null ? goodsReturnDeliveryNote.getCreationTime().toLocalDate().format(DATE_FORMATTER) : "-", normalFont_2);
        Paragraph po_no_date = new Paragraph();
        po_no_date.setAlignment(Element.ALIGN_LEFT);
        po_no_date.add(label_date);
        po_no_date.add(label_date_value);
        poCell.addElement(po_no_date);

        Chunk label_time = new Chunk("Time : ", normalFont);
        Chunk label_time_value = new Chunk(goodsReturnDeliveryNote.getCreationTime() != null ? goodsReturnDeliveryNote.getCreationTime().format(TIME_FORMATTER) : "-", normalFont_2);
        Paragraph po_no_time = new Paragraph();
        po_no_time.setAlignment(Element.ALIGN_LEFT);
        po_no_time.add(label_time);
        po_no_time.add(label_time_value);
        poCell.addElement(po_no_time);

        detailsTable.addCell(poCell);

        document.add(detailsTable);
    }

    private void addItemsTable(Document document, GoodsReturnDeliveryNote goodsReturnDeliveryNote) throws DocumentException {
        BaseColor receiptGreen = new BaseColor(84, 138, 70);
        BaseColor receiptGreen_2 = BaseColor.LIGHT_GRAY;
        BaseColor receiptWhite = BaseColor.WHITE;
        LineSeparator line = new LineSeparator();
        line.setLineColor(BaseColor.LIGHT_GRAY);

        PdfPTable itemsTable = new PdfPTable(5);
        itemsTable.setWidthPercentage(100);
        itemsTable.setWidths(new float[]{1, 5, 5, 5, 5});
        itemsTable.setSpacingAfter(10);
        itemsTable.setSpacingAfter(1f);

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA, 10, receiptWhite);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

        PdfPCell listNo = new PdfPCell(new Phrase("No", headerFont));
        listNo.setPadding(5);
        listNo.setBorder(Rectangle.NO_BORDER);
        listNo.setHorizontalAlignment(Element.ALIGN_CENTER);
        listNo.setBackgroundColor(receiptGreen);
        itemsTable.addCell(listNo);

        PdfPCell descHeader = new PdfPCell(new Phrase("Product Name", headerFont));
        descHeader.setPadding(5);
        descHeader.setBorder(Rectangle.NO_BORDER);
        descHeader.setBackgroundColor(receiptGreen);
        descHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        itemsTable.addCell(descHeader);


        PdfPCell qtyHeader = new PdfPCell(new Phrase("Product Code", headerFont));
        qtyHeader.setPadding(5);
        qtyHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        qtyHeader.setBorder(Rectangle.NO_BORDER);
        qtyHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(qtyHeader);


        PdfPCell unitPriceHeader = new PdfPCell(new Phrase("Quantity Returned", headerFont));
        unitPriceHeader.setPadding(5);
        unitPriceHeader.setBorder(Rectangle.NO_BORDER);
        unitPriceHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        unitPriceHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(unitPriceHeader);

        PdfPCell totalExpectedHeader = new PdfPCell(new Phrase("Quantity Requested", headerFont));
        totalExpectedHeader.setPadding(5);
        totalExpectedHeader.setBorder(Rectangle.NO_BORDER);
        totalExpectedHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        totalExpectedHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(totalExpectedHeader);

        int rowIndex = 0;

        for (ReturnedProductItem productItem : goodsReturnDeliveryNote.getReturnedProductItems()) {

            PdfPCell numberCell = new PdfPCell(new Phrase(String.valueOf(rowIndex + 1), cellFont));
            numberCell.setPadding(5);
            numberCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            numberCell.setBorderColor(receiptGreen);
            itemsTable.addCell(numberCell);

            PdfPCell descCell = new PdfPCell(new Phrase(productItem.getProductName(), cellFont));
            descCell.setPadding(5);
            numberCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            descCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(descCell);

            PdfPCell qtyCell = new PdfPCell(new Phrase(productItem.getProductCode(), cellFont));
            qtyCell.setPadding(5);
            qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            qtyCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(qtyCell);

            PdfPCell unitPriceCell = new PdfPCell(new Phrase(String.format("%,.2f", (double) productItem.getQuantityReturned()), cellFont));
            unitPriceCell.setPadding(5);
            unitPriceCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            unitPriceCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(unitPriceCell);

            PdfPCell totalExpectedCell = new PdfPCell(new Phrase(String.format("%,.2f", (double) productItem.getQuantityRequested()), cellFont));
            totalExpectedCell.setPadding(5);
            totalExpectedCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            totalExpectedCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(totalExpectedCell);

            rowIndex++;
        }

        document.add(itemsTable);
    }

    public ByteArrayOutputStream createGoodsReturnedExcel(GoodsReturnDeliveryNote goodsReturnNote, String name) {
        try {

            String documentTitle = "Goods Returned Note";

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
                    "Printed By: \t \t     " + name,
                    stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 3));

            XSSFRow row4 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row4, 0, "Printed On: \t \t     " + LocalDateTime.now(), stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 3));

            List<String> contentHeaders = Arrays.asList(
                    "Product Name",
                    "Product Code",
                    "Quantity Returned",
                    "Quantity Requested"
            );

            int colIndex;
            Row headerRow = sheet.createRow(rowIndex += 2);

            for (int i = 0; i < contentHeaders.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(contentHeaders.get(i));
                cell.setCellStyle(headerCellStyle);
            }

            ++rowIndex;

            addRecordData(workbook, goodsReturnNote.getReturnedProductItems(), sheet, rowIndex);

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

    public void addRecordData(
            XSSFWorkbook workbook,
            List<ReturnedProductItem> returnedProductItems,
            XSSFSheet sheet,
            int rowIndex) {
        CellStyle parentRowStyle = workbook.createCellStyle();

        for (ReturnedProductItem returnedProductItem : returnedProductItems) {

            XSSFRow parentRow = sheet.createRow(++rowIndex);
            int colIndex = -1;

            excelUtils.createStringDataCell(parentRow, ++colIndex, returnedProductItem.getProductName(), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, returnedProductItem.getProductCode(), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, String.valueOf(returnedProductItem.getQuantityReturned()), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, String.valueOf(returnedProductItem.getQuantityRequested()), parentRowStyle);

        }
    }
}
