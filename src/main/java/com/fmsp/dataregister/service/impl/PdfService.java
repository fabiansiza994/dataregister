package com.fmsp.dataregister.service.impl;

import com.fmsp.dataregister.entity.Reporte;
import com.fmsp.dataregister.entity.Trabajo;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PdfService {

    public static void generarFacturaTrabajo(Trabajo trabajo, Reporte reporte, ByteArrayOutputStream baos) throws Exception {
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document doc = new Document(pdfDoc, PageSize.A4);
        doc.setMargins(120, 30, 60, 30); // margen superior amplio para dejar espacio a la bineta

        doc.add(new Paragraph(""));

        PdfPage page = pdfDoc.getFirstPage();
        Rectangle pageSize = page.getPageSize();

        if (reporte.getBineta() != null && !reporte.getBineta().isEmpty()) {
            ImageData binetaImg = ImageDataFactory.create(reporte.getBineta());
            PdfCanvas canvas = new PdfCanvas(page);
            float width = pageSize.getWidth();
            float height = 100f;

            canvas.addImageFittedIntoRectangle(
                    binetaImg,
                    new Rectangle(0, pageSize.getTop() - height, width, height),
                    false
            );
        }

        if (reporte.getMarcaAguaUrl() != null && !reporte.getMarcaAguaUrl().isEmpty()) {
            ImageData watermarkImg = ImageDataFactory.create(reporte.getMarcaAguaUrl());
            PdfCanvas canvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdfDoc);
            PdfExtGState gs = new PdfExtGState().setFillOpacity(0.1f);
            canvas.saveState();
            canvas.setExtGState(gs);

            float imageWidth = 150;
            float imageHeight = 150;
            float x = pageSize.getRight() - imageWidth - 30;
            float y = pageSize.getBottom() + 30;

            canvas.addImageFittedIntoRectangle(
                    watermarkImg,
                    new Rectangle(x, y, imageWidth, imageHeight),
                    false
            );
            canvas.restoreState();
        }


        String fechaHoy = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        doc.add(new Paragraph("Date: " + fechaHoy)
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontSize(10));

        if (reporte.getLogoUrl() != null && !reporte.getLogoUrl().isEmpty()) {
            Image logo = new Image(ImageDataFactory.create(reporte.getLogoUrl()));
            logo.setHorizontalAlignment(HorizontalAlignment.CENTER);
            logo.scaleToFit(120, 120);
            doc.add(logo);
        }

        doc.add(new Paragraph(trabajo.getUsuario().getGrupo().getEmpresa().getNombre())
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER));

        doc.add(new Paragraph("")
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

        Table table = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .useAllAvailableWidth();

        table.addCell(celda("Work Date", true));
        table.addCell(celda(String.valueOf(trabajo.getFecha()), false));

        table.addCell(celda("Client", true));
        table.addCell(celda(trabajo.getCliente().getNombre() + " " + trabajo.getCliente().getApellido(), false));

        table.addCell(celda("Description", true));
        table.addCell(celda(trabajo.getDescripcionLabor(), false));

        table.addCell(celda("Labor Cost", true));
        table.addCell(celda("$" + trabajo.getValorLabor(), false));

        table.addCell(celda("Material Cost", true));
        table.addCell(celda("$" + trabajo.getValorMateriales(), false));

        table.addCell(celda("Total Cost", true));
        table.addCell(celda("$" + trabajo.getValorTotal(), false));

        doc.add(table);

        /*doc.add(new Paragraph("\nRealizado por: " + trabajo.getUsuario().getUsuario())
                .setFontSize(10)
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginTop(30));*/

        if (reporte.getPiePagina() != null && !reporte.getPiePagina().isEmpty()) {
            doc.add(new Paragraph(reporte.getPiePagina())
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY)
                    .setFontSize(9)
                    .setItalic()
                    .setMarginTop(20));
        }

        doc.close();
    }

    private static Cell celda(String texto, boolean esTitulo) {
        Cell cell = new Cell().add(new Paragraph(texto));
        cell.setPadding(5);
        cell.setBorder(new SolidBorder(0.5f));
        if (esTitulo) {
            cell.setBold();
            cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        }
        return cell;
    }
}
