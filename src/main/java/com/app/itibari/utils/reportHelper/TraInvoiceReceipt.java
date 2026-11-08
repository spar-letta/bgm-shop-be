package com.app.itibari.utils.reportHelper;

import com.app.itibari.dataTypes.OrderItemStatus;
import com.app.itibari.dataTypes.OrderType;
import com.app.itibari.dataTypes.ReceiptPaymentType;
import com.app.itibari.dataTypes.WalletItemStatus;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.InvoiceDetailDto;
import com.app.itibari.models.request.TRARequest;
import com.app.itibari.models.response.TRAResponse;
import com.app.itibari.repository.CreditLimitItemRepository;
import com.app.itibari.repository.ReceiptItemRepository;
import com.app.itibari.repository.ReceiptRepository;
import com.app.itibari.repository.SalesOrdersRepository;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TraInvoiceReceipt {

    private final ReceiptRepository receiptRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final SalesOrdersRepository salesOrdersRepository;

    private static final BaseColor GREEN = new BaseColor(83, 128, 74);
    private static final BaseColor DARK_GREEN = new BaseColor(61, 109, 56);
    private static final BaseColor MID_GREEN = new BaseColor(90, 180, 108);
    private static final BaseColor LIGHT_GREEN = new BaseColor(129, 209, 148);
    private static final BaseColor TABLE_BORDER = new BaseColor(196, 196, 196);
    private static final BaseColor PANEL_BG = new BaseColor(230, 233, 230);
    private static final BaseColor WHITE_BG = new BaseColor(255, 255, 255);
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");

    private final CreditLimitItemRepository creditLimitItemRepository;

    public ByteArrayOutputStream generateCustomerInvoiceReceipt(UUID orderPublicId, OrderType orderType,
                                                                String generatedBy, String orderNumber) {
        SalesOrders salesOrder = salesOrdersRepository.findByPublicId(orderPublicId);

        if (orderType == OrderType.CREDIT) {
            return generateCustomerCreditInvoiceReceipt(salesOrder, generatedBy, orderNumber);
        } else if (orderType == OrderType.CASH) {
            return generateCustomerCashInvoiceReceipt(salesOrder, generatedBy, orderNumber);
        } else {
            log.warn("Unsupported order type {} for invoice receipt generation for order {}", orderType, orderPublicId);
            return new ByteArrayOutputStream();
        }
    }

    private ByteArrayOutputStream generateCustomerCashInvoiceReceipt(SalesOrders salesOrder, String generatedBy, String orderNumber) {
        Optional<Receipt> receiptOptional = receiptRepository.findBySalesOrderPublicIdAndPaymentTypeAndDeletedIsFalse(salesOrder.getPublicId().toString(), ReceiptPaymentType.CASH);
        if (receiptOptional.isEmpty()) {
            return new ByteArrayOutputStream();
        }

        Receipt receipt = receiptOptional.get();

        List<ReceiptItem> receiptItems = receiptItemRepository.findByReceiptAndDeletedIsFalse(receipt);


        Document document = new Document(PageSize.A4, 10, 10, 12, 14);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            drawPageDecorations(writer, document);
            addHeader(document);
            addBillToAndInvoiceDetails(document, receipt, false, salesOrder.getCustomer(), orderNumber);
            addLineItems(document, receiptItems);
            addTotalsPanel(document, salesOrder);
            addQrAndVerification(document, receipt);
            addBankAndPayment(document, salesOrder, receipt);
            addTermsAndFooter(document);
        } catch (Exception e) {
            log.error("Failed to generate receipt PDF for order {}", salesOrder.getSalesOrderNumber(), e);
            throw new RuntimeException("Error generating receipt PDF", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return out;
    }

    private ByteArrayOutputStream generateCustomerCreditInvoiceReceipt(SalesOrders salesOrder, String generatedBy, String orderNumber) {

        Optional<Receipt> receiptOptional = receiptRepository.findBySalesOrderPublicIdAndPaymentTypeAndDeletedIsFalse(salesOrder.getPublicId().toString(), ReceiptPaymentType.CREDIT);
        if (receiptOptional.isEmpty()) {
            return new ByteArrayOutputStream();
        }

        Receipt receipt = receiptOptional.get();

        List<ReceiptItem> receiptItems = receiptItemRepository.findByReceiptAndDeletedIsFalse(receipt);

        Document document = new Document(PageSize.A4, 10, 10, 12, 14);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            drawPageDecorations(writer, document);
            addHeader(document);
            addBillToAndInvoiceDetails(document, receipt, true, salesOrder.getCustomer(), orderNumber);
            addLineItems(document, receiptItems);
            addTotalsPanel(document, salesOrder);
            addQrAndVerification(document, receipt);
            addBankAndPayment(document, salesOrder, receipt);
            addTermsAndFooter(document);
        } catch (Exception e) {
            log.error("Failed to generate receipt PDF for order {}", salesOrder.getSalesOrderNumber(), e);
            throw new RuntimeException("Error generating receipt PDF", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return out;
    }

    private void drawPageDecorations(PdfWriter writer, Document document) {
        float width = document.getPageSize().getWidth();
        float height = document.getPageSize().getHeight();

        PdfContentByte under = writer.getDirectContentUnder();
        under.saveState();
        under.setColorFill(WHITE_BG);
        under.rectangle(0, 0, width, height);
        under.fill();
        under.restoreState();

        PdfContentByte cb = writer.getDirectContent();
        cb.saveState();

        cb.setColorFill(LIGHT_GREEN);
        cb.moveTo(width * 0.46f, height);
        cb.lineTo(width * 0.82f, height);
        cb.lineTo(width * 0.64f, height - 70f);
        cb.closePath();
        cb.fill();

        cb.setColorFill(MID_GREEN);
        cb.moveTo(width * 0.82f, height);
        cb.lineTo(width, height);
        cb.lineTo(width * 0.86f, height - 58f);
        cb.closePath();
        cb.fill();

        cb.setColorFill(new BaseColor(47, 170, 74));
        cb.moveTo(width * 0.86f, height - 58f);
        cb.lineTo(width, height);
        cb.lineTo(width, height - 70f);
        cb.closePath();
        cb.fill();

        cb.restoreState();
    }

    private void addHeader(Document document) throws Exception {
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(90);

        Image logo = Image.getInstance(new ClassPathResource("static/images/logoImage_1.jpg").getInputStream().readAllBytes());
        logo.scaleToFit(170, 68);
        logo.setAlignment(Image.ALIGN_LEFT);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setPaddingTop(8f);
        logoCell.setPaddingBottom(6f);
        logoCell.addElement(logo);
        header.addCell(logoCell);

        Font detailsFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new BaseColor(44, 75, 43));
        addHeaderLine(header, "Itibari Technologies Limited", detailsFont);
        addHeaderLine(header, "Plot 544/1, Mikocheni light industrial area, Dar es Salaam", detailsFont);
        addHeaderLine(header, "Email: commercial@itibari.io", detailsFont);
        addHeaderLine(header, "Phone: +255 765 881 329", detailsFont);
        addHeaderLine(header, "TIN: 176-727-497", detailsFont);
        addHeaderLine(header, "VRN: 4032129M", detailsFont);
        addHeaderLine(header, " ", detailsFont);
        document.add(header);
    }

    private void addHeaderLine(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(1f);
        cell.setPaddingBottom(1f);
        table.addCell(cell);
    }

    private void addBillToAndInvoiceDetails(Document document, Receipt receipt, boolean isCredit, Customers customer,
                                            String orderNumber
    ) throws DocumentException {
        PdfPTable layout = new PdfPTable(2);
        layout.setWidthPercentage(90);
        layout.setWidths(new float[]{3f, 2f});
        layout.setSpacingBefore(10f);

        Font sectionTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.BLACK);
        Font rowFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new BaseColor(48, 48, 48));
        Font rowBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new BaseColor(48, 48, 48));
        Font paymentFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, DARK_GREEN);

        Paragraph billTo = new Paragraph("Bill To:", sectionTitle);
        billTo.setSpacingAfter(6f);
        billTo.add(Chunk.NEWLINE);
        addMetaLine(billTo, "Customer: ", safe(receipt.getCustomerName()), rowBold, rowFont);
        addMetaLine(billTo, "Address: ", safe(receipt.getCustomerAddress()) + " " + safe(receipt.getCustomerLandMark()), rowBold, rowFont);
        addMetaLine(billTo, "Phone: ", safe(receipt.getMobileNum()), rowBold, rowFont);
        addMetaLine(billTo, "TIN: ", safe(customer.getTin()), rowBold, rowFont);
        addMetaLine(billTo, "VRN: ", safe(""), rowBold, rowFont);
        addMetaLine(billTo, "Z No: ", safe(receipt.getZNumber()), rowBold, rowFont);

        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.setPadding(0f);
        left.addElement(billTo);
        layout.addCell(left);

        Paragraph details = new Paragraph("Sales Invoice", sectionTitle);
        details.setAlignment(Element.ALIGN_LEFT);
        details.setSpacingAfter(2f);
        details.add(Chunk.NEWLINE);

        String creditInvoiceNo = "";
        String receiptNo = "";

        if (isCredit) {
            String invoiceNumber = getInvoiceNumber(receipt.getCustInvoiceNo());
            String digits = extractSuffixAfterDash(invoiceNumber);
            creditInvoiceNo = String.format("SIP-%s", digits);
        } else {
            String customerInvoiceNo = receipt.getCustInvoiceNo();
            String digits = extractSuffixAfterDash(customerInvoiceNo);
            receiptNo = String.format("CSP-%s", digits);
        }

        addMetaLine(details, "Invoice No: ", isCredit ? creditInvoiceNo : receiptNo, rowBold, rowFont);
        addMetaLine(details, "Order No: ", orderNumber, rowBold, rowFont);
        addMetaLine(details, "Invoice Date: ", safe(receipt.getIDate()), rowBold, rowFont);
        if (isCredit) {
            LocalDate dueDate = receipt.getCreditLimitItem() != null ? receipt.getCreditLimitItem().getDueDate() : null;
            addMetaLine(details, "Due Date: ", safe(String.valueOf(dueDate)), rowBold, rowFont);
        }
        addMetaLine(details, "Payment Method: ", isCredit ? "CREDIT" : "CASH", rowBold, paymentFont);
        if (isCredit) {

            LocalDate dateTaken = receipt.getCreditLimitItem() != null ? receipt.getCreditLimitItem().getCreationTime().toLocalDate() : null;
            LocalDate dueDate = receipt.getCreditLimitItem() != null ? receipt.getCreditLimitItem().getDueDate() : null;

            String periodInDays;
            if (dueDate != null && dateTaken != null) {
                long days = ChronoUnit.DAYS.between(dateTaken, dueDate);

                periodInDays = String.valueOf(Math.max(0, days));
            } else {
                periodInDays = "0";
            }
            addMetaLine(details, "Payment Terms: ", safe(periodInDays), rowBold, paymentFont);
        }
        addMetaLine(details, "VFD No: ", safe(receipt.getVfdInvoiceNum()), rowBold, rowFont);
        addMetaLine(details, "Time: ", safe(receipt.getITime()), rowBold, rowFont);

        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setPaddingLeft(30f);
        right.addElement(details);
        layout.addCell(right);

        document.add(layout);
    }

    private String getInvoiceNumber(String invoiceNumber) {
        if (invoiceNumber == null) {
            return null;
        }

        String trimmed = invoiceNumber.trim();

        if (trimmed.startsWith("CINV-")) {
            return trimmed;
        }

        int dashIndex = trimmed.indexOf('-');
        if (dashIndex != -1) {
            return "CINV-" + trimmed.substring(dashIndex + 1);
        }

        return "CINV-" + trimmed;
    }

    private String extractSuffixAfterDash(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String trimmed = value.trim();
        int dashIndex = trimmed.indexOf('-');
        if (dashIndex != -1 && dashIndex < trimmed.length() - 1) {
            return trimmed.substring(dashIndex + 1);
        }
        return trimmed;
    }

    private void addMetaLine(Paragraph paragraph, String label, String value, Font labelFont, Font valueFont) {
        paragraph.add(new Chunk(label, labelFont));
        paragraph.add(new Chunk(value, valueFont));
        paragraph.add(Chunk.NEWLINE);
    }

    private void addLineItems(Document document, List<ReceiptItem> receipts) throws DocumentException {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new BaseColor(50, 50, 50));
        Font amountFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new BaseColor(50, 50, 50));

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(90);
        table.setSpacingBefore(12f);
        table.setWidths(new float[]{0.8f, 5.0f, 0.9f, 1.9f, 1.5f, 2.1f});

        table.addCell(headerCell("NO", headerFont));
        table.addCell(headerCell("ITEM", headerFont));
        table.addCell(headerCell("QTY", headerFont));
        table.addCell(headerCell("UNIT PRICE", headerFont));
//        table.addCell(headerCell("DISCOUNT", headerFont));
        table.addCell(headerCell("VAT 18%", headerFont));
        table.addCell(headerCell("AMOUNT", headerFont));

            int i = 1;
            for (ReceiptItem item : receipts) {
                if (item == null) {
                    continue;
                }

                BigDecimal unitPrice = safeStringAmount(item.getPricePerUnit());
                BigDecimal quantity = safeStringAmount(item.getQuantity());
                BigDecimal totalAmount = unitPrice.multiply(quantity).setScale(4, RoundingMode.HALF_UP);
                BigDecimal discountAmount = safeStringAmount(item.getDiscountAmount());

                BigDecimal amountAfterDiscount = totalAmount.subtract(discountAmount);

                table.addCell(bodyCell(String.valueOf(i++), amountFont));
                table.addCell(bodyCell(safe(item.getDescription()), cellFont));
                table.addCell(bodyCell(safe(item.getQuantity()), amountFont));
                table.addCell(bodyCell(safe(item.getPricePerUnit()), amountFont));
//            table.addCell(bodyCell(formatAmount(discountAmount), amountFont));
                table.addCell(bodyCell(safe(item.getTaxValue()), amountFont));
                table.addCell(bodyCell(formatAmount(amountAfterDiscount), amountFont));
            }
        document.add(table);
    }

    private PdfPCell headerCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(GREEN);
        cell.setBorderColor(GREEN);
        cell.setPaddingTop(7f);
        cell.setPaddingBottom(7f);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }

    private PdfPCell bodyCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorderColor(TABLE_BORDER);
        cell.setPaddingTop(9f);
        cell.setPaddingBottom(9f);
        return cell;
    }

    private void addTotalsPanel(Document document, SalesOrders salesOrder) throws DocumentException {
        if (salesOrder == null) {
            return;
        }
        BigDecimal total = safeAmount(calculatePayableAmount(salesOrder));

        BigDecimal newVatAmount = BigDecimal.ZERO;

        BigDecimal subTotal = BigDecimal.ZERO;

        for (SalesOrderItems item : salesOrder.getSalesOrderItems()) {
            if (item.isTaxApplied()) {
                BigDecimal priceBeforeTax = Optional.ofNullable(item.getProductBeforePriceAfterTax())
                        .orElse(BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(Optional.of(item.getQuantityDelivered()).orElse(0)))
                        .subtract(Optional.ofNullable(item.getDiscount()).orElse(BigDecimal.ZERO));
                BigDecimal taxPercentage = Optional.ofNullable(item.getTaxPercentageApplied()).orElse(BigDecimal.ZERO);
                newVatAmount = newVatAmount.add(priceBeforeTax.multiply(taxPercentage));
            }

            BigDecimal productPrice = Optional.ofNullable(item.getProductBeforePriceAfterTax())
                    .orElse(BigDecimal.ZERO);

            BigDecimal deliveredQty = BigDecimal.valueOf(Optional.of(item.getQuantityDelivered()).orElse(0));

            BigDecimal discount = Optional.ofNullable(item.getDiscount())
                    .orElse(BigDecimal.ZERO);

            BigDecimal tt = productPrice.multiply(deliveredQty).subtract(discount);


            subTotal = subTotal.add(tt);
        }
        BigDecimal taxOnDiscount = BigDecimal.ZERO;

        for (SalesOrderItems item : salesOrder.getSalesOrderItems()) {
            if (item != null && item.isTaxApplied()) {
                Product product = item.getProduct();
                if (product != null && product.getTaxCode() != null) {
                    BigDecimal taxRate = product.getTaxCode().getCodeValue();
                    BigDecimal discount = item.getDiscount();

                    if (taxRate != null && discount != null) {
                        taxOnDiscount = taxOnDiscount.add(taxRate.multiply(discount));
                    }
                }
            }
        }

        BigDecimal grandVatAmount = newVatAmount.add(taxOnDiscount);

        // let newVat be of

        BigDecimal vat = grandVatAmount.setScale(2, RoundingMode.HALF_UP);


        BigDecimal subTotals = total.setScale(2, RoundingMode.HALF_UP);

        if (subTotals.compareTo(BigDecimal.ZERO) < 0) {
            subTotals = BigDecimal.ZERO;
        }

        BigDecimal totalInVat = subTotals.subtract(vat);

        PdfPTable wrap = new PdfPTable(2);
        wrap.setWidthPercentage(90);
        wrap.setSpacingBefore(2f);
        wrap.setWidths(new float[]{59f, 31f});

        PdfPTable totals = new PdfPTable(2);
        totals.setWidths(new float[]{3.1f, 1.9f});
        totals.setWidthPercentage(100f);
        totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.setSpacingBefore(0f);

        Font label = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.BLACK);
        Font value = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, GREEN);
        Font totalLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.WHITE);
        Font creditLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.WHITE);
        Font totalValue = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
        Font greenValue = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, GREEN);

        totals.addCell(totalsLabelCell("Total TZS Excl VAT:", label, PANEL_BG));
        totals.addCell(totalsValueCell(formatAmount(subTotals), value, PANEL_BG));

        totals.addCell(totalsLabelCell("VAT:", label, PANEL_BG));
        totals.addCell(totalsValueCell(formatAmount(vat), value, PANEL_BG));

        BigDecimal subTotalAndVatAmount = subTotals.add(vat);
        BigDecimal interestAmount = BigDecimal.ZERO;

        BigDecimal additionVatSubTotal = subTotalAndVatAmount.add(interestAmount).setScale(0, RoundingMode.HALF_UP);
        totals.addCell(totalsLabelCell("TOTAL Incl VAT:", totalLabel, GREEN));
        totals.addCell(totalsValueCell(formatAmount(additionVatSubTotal), totalValue, GREEN));
        if (salesOrder.isCreditApplied()) {
            totals.addCell(totalsLabelCell("Credit Interest(" + salesOrder.getInterestRate() + " %):", totalLabel, GREEN));

            interestAmount = subTotalAndVatAmount
                    .multiply(salesOrder.getInterestRate())
                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);

            totals.addCell(totalsValueCell(formatAmount(interestAmount), totalValue, GREEN));


            // payable amount = total + interest

            BigDecimal payableAmount = subTotalAndVatAmount.add(interestAmount).setScale(0, RoundingMode.HALF_UP);
            totals.addCell(totalsLabelCell("Payable Amount(Credit):", totalLabel, GREEN));
            totals.addCell(totalsValueCell(formatAmount(payableAmount), totalValue, GREEN));
        }


        PdfPCell spacer = new PdfPCell(new Phrase(""));
        spacer.setBorder(Rectangle.NO_BORDER);
        wrap.addCell(spacer);

        PdfPCell cell = new PdfPCell(totals);
        cell.setBorder(Rectangle.NO_BORDER);
        wrap.addCell(cell);

        document.add(wrap);
    }

    private BigDecimal calculatePayableAmount(SalesOrders salesOrder) {
        return salesOrder.getSalesOrderItems()
                .stream()
                .filter(item -> item.getStatus() != OrderItemStatus.NotDelivered)
                .map(item -> Optional.ofNullable(item.getTotalPayableForReceipt())
                        .orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

    }

    private PdfPCell totalsLabelCell(String text, Font font, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(bg);
        cell.setPaddingTop(8f);
        cell.setPaddingBottom(8f);
        cell.setPaddingLeft(6f);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    private PdfPCell totalsValueCell(String text, Font font, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(bg);
        cell.setPaddingTop(8f);
        cell.setPaddingBottom(8f);
        cell.setPaddingRight(9f);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    private void addQrAndVerification(Document document, Receipt receipt) throws DocumentException {
        PdfPTable qrTable = new PdfPTable(2);
        qrTable.setWidthPercentage(60);
        qrTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        qrTable.setSpacingBefore(16f);
        qrTable.setWidths(new float[]{1.2f, 2.2f});

        PdfPCell qrCell = new PdfPCell();
        qrCell.setBorder(Rectangle.NO_BORDER);
        qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        qrCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        String qrCodeUri = receipt.getQrCodeUri();
        if (qrCodeUri != null && qrCodeUri.contains("base64,")) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(qrCodeUri.substring(qrCodeUri.indexOf(',') + 1).trim());
                Image qrImage = Image.getInstance(imageBytes);
                qrImage.scaleAbsolute(80f, 80f);
                qrCell.addElement(qrImage);
            } catch (Exception e) {
                log.warn("Could not add QR code image for sales order {}", receipt.getSalesOrderPublicId(), e);
            }
        }
        qrTable.addCell(qrCell);

        Font textFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.BLACK);
        PdfPCell codeCell = new PdfPCell(new Phrase("Invoice Verification Code: " + safe(receipt.getRctvCode()), textFont));
        codeCell.setBorder(Rectangle.NO_BORDER);
        codeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        codeCell.setPaddingLeft(8f);
        codeCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        qrTable.addCell(codeCell);

        document.add(qrTable);
    }

    private void addBankAndPayment(Document document, SalesOrders salesOrder, Receipt receipt) throws DocumentException {
        BigDecimal totalAmount = safeAmount(salesOrder != null ? calculatePayableAmount(salesOrder) : null);
        BigDecimal balanceDue = safeAmount(salesOrder != null ? salesOrder.getCustomerAmountToPay() : null);
        if (balanceDue.compareTo(totalAmount) > 0) {
            balanceDue = totalAmount;
        }
        BigDecimal amountPaid = totalAmount.subtract(balanceDue);
        if (amountPaid.compareTo(BigDecimal.ZERO) < 0) {
            amountPaid = BigDecimal.ZERO;
        }

        PdfPTable bank = new PdfPTable(3);
        bank.setWidthPercentage(90);
        bank.setSpacingBefore(14f);
        bank.setWidths(new float[]{2.3f, 2.3f, 1.2f});

        BigDecimal newVatAmount = BigDecimal.ZERO;


        assert salesOrder != null;

        for (SalesOrderItems item : salesOrder.getSalesOrderItems()) {
            if (item.isTaxApplied()) {
                BigDecimal priceBeforeTax = Optional.ofNullable(item.getProductBeforePriceAfterTax())
                        .orElse(BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(Optional.of(item.getQuantityDelivered()).orElse(0)))
                        .subtract(Optional.ofNullable(item.getDiscount()).orElse(BigDecimal.ZERO));
                BigDecimal taxPercentage = Optional.ofNullable(item.getTaxPercentageApplied()).orElse(BigDecimal.ZERO);
                newVatAmount = newVatAmount.add(priceBeforeTax.multiply(taxPercentage));
            }
        }

        // let newVat be of

        BigDecimal taxOnDiscount = BigDecimal.ZERO;

        for (SalesOrderItems item : salesOrder.getSalesOrderItems()) {
            if (item != null && item.isTaxApplied()) {
                Product product = item.getProduct();
                if (product != null && product.getTaxCode() != null) {
                    BigDecimal taxRate = product.getTaxCode().getCodeValue();
                    BigDecimal discount1 = item.getDiscount();

                    if (taxRate != null && discount1 != null) {
                        taxOnDiscount = taxOnDiscount.add(taxRate.multiply(discount1));
                    }
                }
            }
        }
        BigDecimal grandVatAmount = newVatAmount.add(taxOnDiscount);

        BigDecimal vat = grandVatAmount.setScale(2, RoundingMode.HALF_UP);

        bank.addCell(bankCell(buildBankPanel(
                "Payment Bank Details",
                "Bank Name: CRDB BANK",
                "Bank Ac: 0150958172600",
                "Acc Name: ITIBARI TECHNOLOGIES LTD",
                "Swift Code: CORUTZTZ",
                "Branch: Tower Branch"
        )));

        if (salesOrder.isCreditApplied()) {
            bank.addCell(bankCell(buildBankPanel("", ""
            )));
        } else {
            bank.addCell(bankCell(buildBankPanel(
                    "Payment Bank Details",
                    "Bank Name: NMB BANK",
                    "Bank Ac: 23910007355",
                    "Acc Name: ITIBARI TECHNOLOGIES LTD",
                    "Swift Code: NMBTZTZ",
                    "Branch: Oyster Plaza"
            )));
        }

        PdfPTable amountPanel = new PdfPTable(1);
        amountPanel.setWidthPercentage(100);

        Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new BaseColor(51, 86, 49));
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10, new BaseColor(45, 45, 45));
        Font balance = FontFactory.getFont(FontFactory.HELVETICA, 10, GREEN);

        Font balanceFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, GREEN);

        boolean credit = salesOrder.isCreditApplied();
        BigDecimal amountPlusVat = amountPaid.add(vat).setScale(0, RoundingMode.HALF_UP);

        PdfPCell paid = new PdfPCell(new Phrase("Amount Paid: " + formatAmount(credit ? safeAmount(receipt.getCreditLimitItem() != null ? receipt.getCreditLimitItem().getPaidAmount() : null) : amountPlusVat), balanceFont));
        paid.setBorder(Rectangle.NO_BORDER);
        paid.setBackgroundColor(PANEL_BG);
        paid.setPaddingTop(12f);
        paid.setPaddingBottom(12f);
        paid.setPaddingLeft(8f);


        BigDecimal amountDue = credit
                ? safeAmount(receipt.getCreditLimitItem() != null ? receipt.getCreditLimitItem().getBalanceAmount() : null)
                : BigDecimal.ZERO;
        PdfPCell due = new PdfPCell(new Phrase("Balance Due:  " + formatAmount(credit ? amountDue : balanceDue), balanceFont));
        due.setBorder(Rectangle.NO_BORDER);
        due.setBackgroundColor(new BaseColor(197, 228, 190));
        due.setPaddingTop(12f);
        due.setPaddingBottom(12f);
        due.setPaddingLeft(8f);


        PdfPCell spacerTitle = new PdfPCell(new Phrase(" ", title));
        spacerTitle.setBorder(Rectangle.NO_BORDER);
        spacerTitle.setBackgroundColor(PANEL_BG);
        spacerTitle.setPaddingTop(8f);
        spacerTitle.setPaddingBottom(8f);

        amountPanel.addCell(spacerTitle);
        amountPanel.addCell(paid);
        amountPanel.addCell(due);

        bank.addCell(bankCell(amountPanel));

        document.add(bank);
    }

    private PdfPCell bankCell(PdfPTable inner) {
        PdfPCell cell = new PdfPCell(inner);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(PANEL_BG);
        cell.setPadding(9f);
        return cell;
    }

    private PdfPTable buildBankPanel(String title, String... lines) {
        PdfPTable panel = new PdfPTable(1);
        panel.setWidthPercentage(90);

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new BaseColor(51, 86, 49));
        Font lineFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new BaseColor(62, 62, 62));

        PdfPCell tCell = new PdfPCell(new Phrase(title, titleFont));
        tCell.setBorder(Rectangle.NO_BORDER);
        tCell.setBackgroundColor(PANEL_BG);
        tCell.setPaddingBottom(8f);
        panel.addCell(tCell);

        for (String line : lines) {
            PdfPCell lineCell = new PdfPCell(new Phrase(line, lineFont));
            lineCell.setBorder(Rectangle.NO_BORDER);
            lineCell.setBackgroundColor(PANEL_BG);
            lineCell.setPaddingBottom(6f);
            panel.addCell(lineCell);
        }
        return panel;
    }

    private void addTermsAndFooter(Document document) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, DARK_GREEN);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 8, new BaseColor(74, 124, 70));
        Font thankYouFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, GREEN);

        Paragraph termsTitle = new Paragraph("Terms & Conditions", titleFont);
        termsTitle.setAlignment(Element.ALIGN_CENTER);
        termsTitle.setSpacingBefore(14f);
        document.add(termsTitle);

        Paragraph terms = new Paragraph(
                "Goods once sold will not be taken back or exchanged.\n" +
                        "Please check the goods at the time of purchase for any defects.\n" +
                        "Warranty claims should be directed to the manufacturer.\n" +
                        "This receipt serves as a valid proof of purchase.\n" +
                        "Prices are inclusive of all applicable taxes.\n" +
                        "For any inquiries, contact us at commercial@itibari.io or +255 765 881 329 ",
                bodyFont
        );
        terms.setAlignment(Element.ALIGN_CENTER);
        terms.setLeading(13f);
        terms.setSpacingBefore(4f);
        document.add(terms);

        Paragraph thankYou = new Paragraph("Thank you for your business!", thankYouFont);
        thankYou.setAlignment(Element.ALIGN_CENTER);
        thankYou.setSpacingBefore(16f);
        document.add(thankYou);
    }


    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String formatAmount(BigDecimal amount) {
        return MONEY_FORMAT.format(safeAmount(amount));
    }

    private String formatStringAmount(String amount) {
        try {
            if (amount == null || amount.trim().isEmpty()) {
                return "0";
            }
            return MONEY_FORMAT.format(new BigDecimal(amount.trim()));
        } catch (Exception ignored) {
            return safe(amount);
        }
    }

    private BigDecimal safeStringAmount(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (Exception ignored) {
            return BigDecimal.ZERO;
        }
    }

    public boolean saveReceipt(TRARequest request, TRAResponse response, SalesOrders salesOrder) {
        log.info("Saving TRA receipt for sales order: {}", salesOrder.getSalesOrderNumber());


        CreditLimitItem creditLimitItem = creditLimitItemRepository.findByOrderNumberAndStatusAndDeletedIsFalse(salesOrder.getSalesOrderNumber(), WalletItemStatus.ACTIVE)
                .orElse(null);
        ReceiptPaymentType receiptPaymentType = creditLimitItem != null ? ReceiptPaymentType.CREDIT : ReceiptPaymentType.CASH;

        Receipt receipt = new Receipt();
        receipt.setSalesOrderPublicId(salesOrder.getPublicId().toString());
        receipt.setInvoiceNumber(request.getCustInvoiceNo());
        receipt.setCustomerName(salesOrder.getCustomer().getName());
        receipt.setRctvNum(response.getRctvNum());
        receipt.setRctvCode(response.getRctvCode());
        receipt.setZNumber(response.getZNumber());
        receipt.setVfdInvoiceNum(response.getVfdInvoiceNum());
        receipt.setIDate(request.getIDate());
        receipt.setITime(request.getITime());

        receipt.setCustInvoiceNo(request.getCustInvoiceNo());

        receipt.setCustIdType(request.getCustIdType());
        receipt.setCustId(request.getCustId());
        receipt.setMobileNum(request.getMobileNum());
        receipt.setUserName(request.getUserName());
        receipt.setPayType(request.getPayType());
        receipt.setCreationTime(LocalDateTime.now());
        receipt.setStatus(response.getStatus());
        receipt.setQrPath(response.getQrPath());
        receipt.setQrCodeUri(response.getQrCodeUri());
        receipt.setPaymentType(receiptPaymentType);
        receipt.setCustomerAddress(salesOrder.getCustomer().getAddress());
        receipt.setCustomerLandMark(salesOrder.getCustomer().getOrganizationLocation() != null ?
                salesOrder.getCustomer().getOrganizationLocation().getName() : "");
        receipt.setCreditLimitItem(creditLimitItem);

        Receipt savedReceipt = receiptRepository.save(receipt);
        List<ReceiptItem> receiptItems = new ArrayList<>();

        for (InvoiceDetailDto detailDto : request.getOldInvoiceDetails()) {
            ReceiptItem item = new ReceiptItem();

            BigDecimal percentage = safeStringAmount(detailDto.getDiscountPercentage());

            item.setDescription(detailDto.getProductName());
            item.setQuantity(detailDto.getQty());
            item.setTaxCode(detailDto.getTaxCode());
            item.setAmount(Optional.ofNullable(detailDto.getAmt()).orElse(BigDecimal.ZERO));
            item.setTaxValue(detailDto.getTaxValue());
            item.setPricePerUnit(detailDto.getPricePerUnit());
            item.setReceipt(savedReceipt);
            item.setCreationTime(LocalDateTime.now());
            item.setProductPublicId(detailDto.getProductPublicId());
            item.setDiscountAmount(detailDto.getDiscountAmount());
            item.setDiscountPercentage(String.valueOf(percentage.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)));
            item.setSalesOrderItemId(detailDto.getSalesOrderItemId());
            ReceiptItem receiptItem = receiptItemRepository.save(item);
            receiptItems.add(receiptItem);
        }

        savedReceipt.setReceiptItems(receiptItems);
        Receipt receipt1 = receiptRepository.save(savedReceipt);

        log.info("TRA receipt saved successfully for sales order: {}", salesOrder.getSalesOrderNumber());
        return receipt1.getId() != null;
    }
}
