package com.app.itibari.utils.helpers;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.ExceptionConverter;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
// Removed unused BarcodeQRCode import
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfGState;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

@Component
public class PdfUtil extends PdfPageEventHelper {

    private PdfTemplate t;

    private Image total;

    private static final BaseColor DARK_GREEN = new BaseColor(61, 109, 56);

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

        addHeader(writer, document);
        addWaterMark(writer, document);
        addFooter(writer, document);
    }

    private void addHeader(PdfWriter writer, Document document) {

        PdfPTable header = new PdfPTable(3);
        header.setTotalWidth(document.getPageSize().getWidth() - 40);

        header.getDefaultCell().setFixedHeight(40);
        header.getDefaultCell().setBorder(Rectangle.BOTTOM);
        header.getDefaultCell().setBorderColor(BaseColor.GREEN);


        // add text
        PdfPCell text = new PdfPCell();
        text.setPaddingBottom(10);
        text.setPaddingLeft(10);
        text.setBorder(Rectangle.BOTTOM);
        text.setBorderColor(BaseColor.GREEN);
        text.addElement(new Phrase("", new Font(Font.FontFamily.HELVETICA, 12)));
        header.addCell(text);

        // write content
        header.writeSelectedRows(0, -1, (document.left() + 5), (document.top() + 40), writer.getDirectContent());
    }

    private void addFooter(PdfWriter writer, Document document) {

        PdfPTable footer = new PdfPTable(3);
        try {
            // set defaults
            footer.setWidths(new int[]{24, 2, 1});
            footer.setTotalWidth(document.getPageSize().getWidth() - 40);
            footer.setLockedWidth(true);
            footer.getDefaultCell().setFixedHeight(30);
            footer.getDefaultCell().setBorder(Rectangle.TOP);
            footer.getDefaultCell().setBorderColor(BaseColor.GREEN);

            // add copyright
            footer.addCell(new Phrase("  © ITIBARI TECHNOLOGIES LIMITED.          https://www.itibari.io   ",
                                      new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, DARK_GREEN)));

            // add current page count
            footer.getDefaultCell().setHorizontalAlignment(Element.ALIGN_RIGHT);
            footer.addCell(new Phrase(String.format("Page %d of", writer.getPageNumber()), new Font(Font.FontFamily.HELVETICA, 8)));

            // add placeholder for total page count
            PdfPCell totalPageCount = new PdfPCell(total);
            totalPageCount.setBorder(Rectangle.TOP);
            totalPageCount.setBorderColor(BaseColor.LIGHT_GRAY);
            footer.addCell(totalPageCount);

            // write page
            PdfContentByte canvas = writer.getDirectContent();
            canvas.beginMarkedContentSequence(PdfName.ARTIFACT);
            footer.writeSelectedRows(0, -1, (document.left() + 10), 30, canvas);
            canvas.endMarkedContentSequence();
        } catch (DocumentException de) {
            throw new ExceptionConverter(de);
        }
    }

    private void addWaterMark(PdfWriter writer, Document document) {

        try {
            // draw on top of the existing content to avoid being hidden by opaque page elements
            PdfContentByte canvas = writer.getDirectContent();


            PdfGState gs = new PdfGState();
            gs.setFillOpacity(0.12f);
            gs.setStrokeOpacity(0.12f);
            canvas.saveState();
            canvas.setGState(gs);


            Font bigFont = new Font(Font.FontFamily.HELVETICA, 90, Font.BOLD, new BaseColor(200, 200, 200));
            Phrase big = new Phrase("CONFIDENTIAL", bigFont);
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, big,
                                       document.getPageSize().getWidth() / 2,
                                       document.getPageSize().getHeight() / 2,
                                       45);
            canvas.restoreState();
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    public void onCloseDocument(PdfWriter writer, Document document) {

        int totalLength = String.valueOf(writer.getPageNumber()).length();
        int totalWidth = totalLength * 5;
        ColumnText.showTextAligned(t, Element.ALIGN_RIGHT,
                                   new Phrase(String.valueOf(writer.getPageNumber()), new Font(Font.FontFamily.HELVETICA, 8)),
                                   totalWidth, 6, 0);
    }

    public PdfPCell createCell(String text, Integer alignment, boolean bold) {

        Font font = new Font();
        font.setFamily(Font.FontFamily.TIMES_ROMAN.name());
        font.setSize(8);
        if (bold) {
            font.setStyle(Font.BOLD);
        }

        Paragraph paragraph = new Paragraph(text, font);
        if (alignment != null) {
            paragraph.setAlignment(alignment);
        }
        PdfPCell pdfPCell = new PdfPCell();
        pdfPCell.addElement(paragraph);
        pdfPCell.setBorder(Rectangle.NO_BORDER);
        return pdfPCell;
    }

    public PdfPCell createCell(String text, Integer alignment, boolean bold, Integer colspan, Integer rowspan) {

        PdfPCell pdfPCell = createCell(text, alignment, bold);
        if (colspan != null) {
            pdfPCell.setColspan(colspan);
        }
        if (rowspan != null) {
            pdfPCell.setRowspan(rowspan);
        }
        return pdfPCell;
    }

    public PdfPCell createCell(String text, Integer alignment, boolean bold, Integer colspan, Integer rowspan, boolean borders) {

        PdfPCell pdfPCell = createCell(text, alignment, bold, colspan, rowspan);
        if (borders) {
            pdfPCell.setBorder(Rectangle.BOX);
        } else {
            pdfPCell.setBorder(Rectangle.NO_BORDER);
        }
        return pdfPCell;
    }
}


