package com.app.itibari.utils.helpers;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPCellEvent;
import com.itextpdf.text.pdf.PdfPTable;
import org.springframework.stereotype.Component;

@Component
public class RoundedBackgroundCellEvent implements PdfPCellEvent {
    private final BaseColor backgroundColor = new BaseColor(245, 245, 245); // yellow
    private final float radius = 5f;

    @Override
    public void cellLayout(PdfPCell cell, Rectangle rect, PdfContentByte[] canvases) {
        PdfContentByte canvas = canvases[PdfPTable.BACKGROUNDCANVAS];
        canvas.setColorFill(backgroundColor);

        float x = rect.getLeft();
        float y = rect.getBottom();
        float w = rect.getWidth();
        float h = rect.getHeight();

        // Draw rounded rectangle
        canvas.roundRectangle(x, y, w, h, radius);
        canvas.fill();
    }
}

