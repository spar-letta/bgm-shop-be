package com.app.itibari.utils.reportHelper;

import com.app.itibari.models.entities.*;
import com.app.itibari.utils.helpers.PdfUtil;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceReceipt {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ByteArrayOutputStream createReceipt(SalesOrders salesOrder, Principal principal) {
        Document document = new Document(PageSize.A4);
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
            logo_2.setIndentationLeft(-71f);
            document.add(logo_2);

            document.add(new Paragraph("\n\n\n"));
            Image logo = Image.getInstance(new ClassPathResource("static/images/logoImage_1.jpg").getInputStream().readAllBytes());
            logo.scaleToFit(120, 120);
            logo.setAlignment(Image.ALIGN_LEFT);
            logo.setIndentationLeft(-10f);
            document.add(logo);

            Paragraph companyDetails = new Paragraph("""
                    ITIBARI TECHNOLOGIES LTD
                    Mikocheni light industrial area, Dar es Salaam \
                    Salaam\
                    
                    Plot \
                    Email: commercial@itibari.io""",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, receiptGreen));
            companyDetails.setSpacingAfter(18);
            document.add(companyDetails);

            addInvoiceMetadata(document, salesOrder, principal);

            addLineItems(document, salesOrder);

            addTotals(document, salesOrder);


            Font thankYouFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, receiptGreen);
            Paragraph thankYou = new Paragraph("Thank you for your business!", thankYouFont);

            PdfContentByte canvas = writer.getDirectContent();

            float centerX = document.getPageSize().getWidth() / 2;
            float thankYouY = document.bottomMargin() + 100;

            ColumnText.showTextAligned(
                    canvas,
                    Element.ALIGN_CENTER,
                    new Phrase(thankYou),
                    centerX,
                    thankYouY,
                    0);

            BarcodeQRCode my_code = new BarcodeQRCode(String.format("Order Number: %s",
                    salesOrder.getSalesOrderNumber()), 1, 1, null);
            Image qr_image = my_code.getImage();
            qr_image.scaleAbsolute(75f, 75f);

            float qrY = thankYouY - 90;
            float qrX = centerX - (qr_image.getScaledWidth() / 2);
            qr_image.setAbsolutePosition(qrX, qrY);
            canvas.addImage(qr_image);

            Font codeFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
            Phrase codePhrase = new Phrase(String.format("Invoice Verification Code: %s",
                    salesOrder.getSalesOrderNumber()), codeFont);

            float textX = qrX + qr_image.getScaledWidth() + 10;
            float textY = qrY + (qr_image.getScaledHeight() / 2);

            ColumnText.showTextAligned(
                    canvas,
                    Element.ALIGN_LEFT,
                    codePhrase,
                    textX,
                    textY,
                    0
            );

            canvas.addImage(qr_image);

            document.add(qr_image);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Error generating invoice PDF", e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    private void addInvoiceMetadata(Document document, SalesOrders salesOrder, Principal principal) throws DocumentException {
        com.itextpdf.text.Font titleFonts = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        com.itextpdf.text.Font cellFont_1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        com.itextpdf.text.Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        LocalDateTime now = salesOrder.getSoldAt() != null ? salesOrder.getSoldAt() : LocalDateTime.now();
        String formattedDateTime = now.format(formatter);

        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(100);

        Customers customer = salesOrder.getCustomer();

        String address = customer.getAddress();
        String landMark = customer.getLandmark();
        String phoneNumber = customer.getPhoneNumber();

        metaTable.addCell(cell("Customer Details", titleFonts));
        metaTable.addCell(cell("", titleFonts));

        metaTable.addCell(cell(customer.getName(), cellFont));
        metaTable.addCell(cell("Invoice Date: " + LocalDate.now(), cellFont_1));

        metaTable.addCell(cell("P.O BOX "+address, cellFont));
        metaTable.addCell(cell("Invoice No.: " + salesOrder.getSalesOrderNumber(), cellFont));

        metaTable.addCell(cell(landMark, cellFont));
        metaTable.addCell(cell("Terms of Payment: CASH  \nCurrency : TZS", cellFont));

        metaTable.addCell(cell(phoneNumber, cellFont));
        metaTable.addCell(cell("VFD No. 157", cellFont));

        metaTable.addCell(cell("Tin No.", cellFont));
        metaTable.addCell(cell("ZNumber: 9/293939", cellFont));

        metaTable.addCell(cell("VRN No.", cellFont));
        metaTable.addCell(cell("Time Stamp: " + formattedDateTime, cellFont));

        for (int i = 1; i < metaTable.getRows().size(); i++) {
            PdfPCell secondColCell = metaTable.getRow(i).getCells()[1];
            if (secondColCell != null) {
                secondColCell.setPaddingLeft(80f);
            }
        }

        document.add(metaTable);
    }

    private PdfPCell cell(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(5f);
        return c;
    }

    private void addLineItems(Document document, SalesOrders salesOrder) throws DocumentException {
        com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        com.itextpdf.text.Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        PdfPTable lineItems = new PdfPTable(6);
        lineItems.setWidthPercentage(100);
        lineItems.setWidths(new int[]{1, 4, 1, 2, 2, 2});

        lineItems.addCell(createHeaderCell("S No.", headerFont));
        lineItems.addCell(createHeaderCell("Item", headerFont));
        lineItems.addCell(createHeaderCell("Qty", headerFont));
        lineItems.addCell(createHeaderCell("VAT %", headerFont));
        lineItems.addCell(createHeaderCell("Unit Price", headerFont));
        lineItems.addCell(createHeaderCell("Amount", headerFont));

        Set<SalesOrderItems> items = salesOrder.getSalesOrderItems();

        int i = 1;
        for (SalesOrderItems item : items) {
            if (item == null) continue;

            Product product = item.getProduct();
            String productName = (product != null && product.getProductName() != null)
                    ? product.getProductName()
                    : "N/A";

            TaxCode taxCode = (product != null) ? product.getTaxCode() : null;
            String value;

            if (taxCode != null && taxCode.getCodeName() != null &&
                    (taxCode.getCodeName().equalsIgnoreCase("Exempt") ||
                            taxCode.getCodeName().equalsIgnoreCase("Zero Rate"))) {
                value = productName;
            } else if (taxCode != null && taxCode.getCodeValue() != null) {
                value = taxCode.getCodeValue().toString();
            } else {
                value = "N/A";
            }

            Integer qty = item.getQuantity();
            String quantity = (qty != null) ? String.valueOf(qty) : "0";

            String pricePerUnit = (item.getPricePerUnit() != null)
                    ? item.getPricePerUnit().toString()
                    : "0.00";

            lineItems.addCell(createCell(String.valueOf(i), cellFont));
            lineItems.addCell(createCell(productName, cellFont));
            lineItems.addCell(createCell(quantity, cellFont));
            lineItems.addCell(createCell(value, cellFont));
            lineItems.addCell(createCell(pricePerUnit, cellFont));
            lineItems.addCell(createCell(String.valueOf(item.getTotalAmount() != null ? item.getTotalAmount() : BigDecimal.ZERO), cellFont));

            i++;
        }

        document.add(Chunk.NEWLINE);
        document.add(lineItems);
    }

    private PdfPCell createHeaderCell(String text, Font font) {
        BaseColor receiptGreen = new BaseColor(0, 153, 0);

        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(receiptGreen);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(5f);
        return cell;
    }

    private PdfPCell createCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(5f);
        return cell;
    }

    private void addTotals(Document document, SalesOrders salesOrder) throws DocumentException {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);

        BaseColor receiptGreen = new BaseColor(84, 138, 70);

        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 12, receiptGreen);


        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingBefore(15);
        document.add(spacer);

        LineSeparator line = new LineSeparator();
        line.setLineColor(BaseColor.BLACK);
        line.setLineWidth(0.2f);
        line.setPercentage(35);
        line.setAlignment(Element.ALIGN_RIGHT);

        BigDecimal grandTotal = salesOrder.getTotalAmount() != null ? salesOrder.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal vatAmount = salesOrder.getTaxAmountValue() != null ? salesOrder.getTaxAmountValue() : BigDecimal.ZERO;
        BigDecimal subTotal = grandTotal.subtract(vatAmount);

        Paragraph exclusiveAmount = new Paragraph();
        exclusiveAmount.add(new Chunk("  Sub Total : ", labelFont));
        exclusiveAmount.add(new Chunk("   TZS " + String.format("%,.2f", subTotal), valueFont));
        exclusiveAmount.setAlignment(Element.ALIGN_RIGHT);

        Paragraph VATAmount = new Paragraph();
        VATAmount.add(new Chunk("VAT Amount : ", labelFont));
        VATAmount.add(new Chunk("          TZS " + String.format("%,.2f", vatAmount), valueFont));
        VATAmount.setAlignment(Element.ALIGN_RIGHT);

        Paragraph total = new Paragraph();
        total.add(new Chunk("Total Amount : ", labelFont));
        total.add(new Chunk("   TZS " + String.format("%,.2f", grandTotal), valueFont));
        total.setAlignment(Element.ALIGN_RIGHT);

        document.add(exclusiveAmount);
        document.add(VATAmount);
        document.add(new Chunk(line));
        document.add(total);
    }
}
