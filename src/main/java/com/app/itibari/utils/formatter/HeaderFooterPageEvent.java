package com.app.itibari.utils.formatter;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

public class HeaderFooterPageEvent extends PdfPageEventHelper {

    private PdfTemplate t;
    private Image total;

    public void onOpenDocument(PdfWriter writer, Document document) {
        t = writer.getDirectContent().createTemplate(30, 16);
        try {
            total = Image.getInstance(t);
            total.setRole(PdfName.ARTIFACT);
        } catch (DocumentException de) {
            throw new ExceptionConverter(de);
        }
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        addFooter(writer, document);
    }

    private void addFooter(PdfWriter writer, Document document) {
        PdfPTable footer = new PdfPTable(1); // single cell for the line
        BaseColor receiptGreen = new BaseColor(22, 199, 55);

        // configure table
        footer.setTotalWidth(document.getPageSize().getWidth()); // full page width
        footer.setLockedWidth(true);
        footer.getDefaultCell().setFixedHeight(0); // no height, just the border
//        footer.getDefaultCell().setBorder(Rectangle.TOP);
//        footer.getDefaultCell().setBorderColor(receiptGreen);
        footer.getDefaultCell().setBorderWidthTop(1f);

        // add an empty cell (the border will be visible as the line)
        footer.addCell(" ");

        // calculate Y position: bottom margin line
        float y = document.bottomMargin();

        // draw the footer line at the bottom margin
        PdfContentByte canvas = writer.getDirectContent();
        canvas.beginMarkedContentSequence(PdfName.ARTIFACT);
        footer.writeSelectedRows(0, -1, 0, y, canvas);
        canvas.endMarkedContentSequence();
    }



    public void onCloseDocument(PdfWriter writer, Document document) {
        int totalLength = String.valueOf(writer.getPageNumber()).length();
        int totalWidth = totalLength * 5;
        ColumnText.showTextAligned(t, Element.ALIGN_RIGHT,
                new Phrase(String.valueOf(writer.getPageNumber()), new Font(Font.FontFamily.HELVETICA, 8)),
                totalWidth, 6, 0);
    }
}
