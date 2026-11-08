package com.app.itibari.utils.reportHelper;

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
public class QuarantineDisposedProductExport {
    private static final String company = "Itibari Technologies Limited";

    private static final String companyAddress = "Plot 544/1, Mikocheni light industrial area, Dar es Salaam";

    private static final String companyEmail = "Email: commercial@itibari.io";

    private final RoundedBackgroundCellEvent backgroundEvent;
    private final ExcelUtils excelUtils;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0");

    public ByteArrayOutputStream createExcel(List<DisposedQuarantineProducts> content, Principal principal) {
        try {
            String documentTitle = "Disposed Quarantine Product Report";

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
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 9));

            // Printed By
            XSSFRow row3 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row3, 0, "Printed By: " + principal.getName(), stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 9));

            // Printed On
            XSSFRow row4 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row4, 0, "Printed On: " + formatter.format(LocalDateTime.now()), stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 9));
            // Headers
            List<String> contentHeaders = Arrays.asList(
                    "disposal date",
                    "sku",
                    "product name",
                    "disposed",
                    "disposal value",
                    "disposal qty",
                    "reason code",
                    "comment",
                    "initiator",
                    "approver"
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

    private void addRecordData(XSSFWorkbook workbook, List<DisposedQuarantineProducts> content, XSSFSheet sheet, int rowIndex, CellStyle stringDataCellStyleLeftAlign, CellStyle numericDataCellStyleRightAlign) {
        CellStyle parentRowStyle = workbook.createCellStyle();
        parentRowStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());

        Font parentFont = workbook.createFont();
        parentFont.setBold(true);
        parentRowStyle.setFont(parentFont);

        for (DisposedQuarantineProducts disposedProduct : content) {

            XSSFRow parentRow = sheet.createRow(++rowIndex);
            int colIndex = -1;

            excelUtils.createStringDataCell(parentRow, ++colIndex, disposedProduct.disposalDate(), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, disposedProduct.sku(), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, disposedProduct.productName(), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, disposedProduct.disposed() == true ? "Disposed": "--", parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, disposedProduct.disposalValue(), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, disposedProduct.quantity(), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, disposedProduct.reasonCode(), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, disposedProduct.comment(), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, disposedProduct.initiatedBy(), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, disposedProduct.approvedBy(), parentRowStyle);
        }
    }

    public ByteArrayOutputStream generateLeadPdf(List<DisposedQuarantineProducts> content, Principal principal) throws DocumentException, IOException {
        String title = "Disposed Quarantine Product Report";


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

    private void addItemsTable(Document document, List<DisposedQuarantineProducts> content) throws DocumentException {
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

        PdfPCell ddHeader = new PdfPCell(new Phrase("disposal date", headerFont));
        ddHeader.setPadding(5);
        ddHeader.setBorder(Rectangle.NO_BORDER);
        ddHeader.setBackgroundColor(receiptGreen);
        ddHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        itemsTable.addCell(ddHeader);


        PdfPCell skuHeader = new PdfPCell(new Phrase("sku", headerFont));
        skuHeader.setPadding(5);
        skuHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        skuHeader.setBorder(Rectangle.NO_BORDER);
        skuHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(skuHeader);


        PdfPCell pnPriceHeader = new PdfPCell(new Phrase("product name", headerFont));
        pnPriceHeader.setPadding(5);
        pnPriceHeader.setBorder(Rectangle.NO_BORDER);
        pnPriceHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        pnPriceHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(pnPriceHeader);

        PdfPCell disposedHeader = new PdfPCell(new Phrase("disposed", headerFont));
        disposedHeader.setPadding(5);
        disposedHeader.setBorder(Rectangle.NO_BORDER);
        disposedHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        disposedHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(disposedHeader);

        PdfPCell dvHeader = new PdfPCell(new Phrase("disposed value", headerFont));
        dvHeader.setPadding(5);
        dvHeader.setBorder(Rectangle.NO_BORDER);
        dvHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        dvHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(dvHeader);

        PdfPCell qtyHeader = new PdfPCell(new Phrase("disposed qty", headerFont));
        qtyHeader.setPadding(5);
        qtyHeader.setBorder(Rectangle.NO_BORDER);
        qtyHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        qtyHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(qtyHeader);

        PdfPCell rcHeader = new PdfPCell(new Phrase("reason code", headerFont));
        rcHeader.setPadding(5);
        rcHeader.setBorder(Rectangle.NO_BORDER);
        rcHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        rcHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(rcHeader);

        PdfPCell cHeader = new PdfPCell(new Phrase("comment", headerFont));
        cHeader.setPadding(5);
        cHeader.setBorder(Rectangle.NO_BORDER);
        cHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        cHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(cHeader);

        PdfPCell iHeader = new PdfPCell(new Phrase("initiator", headerFont));
        iHeader.setPadding(5);
        iHeader.setBorder(Rectangle.NO_BORDER);
        iHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        iHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(iHeader);

        PdfPCell aHeader = new PdfPCell(new Phrase("approver", headerFont));
        aHeader.setPadding(5);
        aHeader.setBorder(Rectangle.NO_BORDER);
        aHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        aHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(aHeader);

        int rowIndex = 0;

        for (DisposedQuarantineProducts disposedQuarantineProduct : content) {

            PdfPCell numberCell = new PdfPCell(new Phrase(String.valueOf(rowIndex + 1), cellFont));
            numberCell.setPadding(5);
            numberCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            numberCell.setBorderColor(receiptGreen);
            itemsTable.addCell(numberCell);

            PdfPCell descCell = new PdfPCell(new Phrase(disposedQuarantineProduct.disposalDate(), cellFont));
            descCell.setPadding(5);
            descCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            descCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(descCell);

            PdfPCell qtyCell = new PdfPCell(new Phrase(disposedQuarantineProduct.sku(), cellFont));
            qtyCell.setPadding(5);
            qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            qtyCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(qtyCell);

            PdfPCell orderDateCell = new PdfPCell(new Phrase(disposedQuarantineProduct.productName(), cellFont));
            orderDateCell.setPadding(5);
            orderDateCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            orderDateCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(orderDateCell);

            PdfPCell agentCell = new PdfPCell(new Phrase(disposedQuarantineProduct.disposed() == true ? "Disposed" : "--", cellFont));
            agentCell.setPadding(5);
            agentCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            agentCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(agentCell);

            PdfPCell lastContactCell = new PdfPCell(new Phrase(disposedQuarantineProduct.disposalValue().toString(), cellFont));
            lastContactCell.setPadding(5);
            lastContactCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            lastContactCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(lastContactCell);

            PdfPCell qtyContactCell = new PdfPCell(new Phrase(disposedQuarantineProduct.quantity(), cellFont));
            qtyContactCell.setPadding(5);
            qtyContactCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            qtyContactCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(qtyContactCell);

            PdfPCell districtCell = new PdfPCell(new Phrase(disposedQuarantineProduct.reasonCode(), cellFont));
            districtCell.setPadding(5);
            districtCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            districtCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(districtCell);

            PdfPCell wardCell = new PdfPCell(new Phrase(disposedQuarantineProduct.comment(), cellFont));
            wardCell.setPadding(5);
            wardCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            wardCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(wardCell);

            PdfPCell iCell = new PdfPCell(new Phrase(disposedQuarantineProduct.initiatedBy(), cellFont));
            iCell.setPadding(5);
            iCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            iCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(iCell);

            PdfPCell aCell = new PdfPCell(new Phrase(disposedQuarantineProduct.approvedBy(), cellFont));
            aCell.setPadding(5);
            aCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            aCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(aCell);

            rowIndex++;
        }

        document.add(itemsTable);
    }
}
