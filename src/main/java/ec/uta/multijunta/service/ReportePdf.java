package ec.uta.multijunta.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

@Service
public class ReportePdf {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ArchivoService archivos;

    public ReportePdf(ArchivoService archivos) {
        this.archivos = archivos;
    }

    public static String moneda(BigDecimal monto) {
        return (monto.signum() < 0 ? "-$" : "$") + Montos.texto(monto.abs());
    }

    public byte[] generar(ReporteService.Reporte reporte) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(documento, salida);
        documento.open();
        Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font negrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        agregarLogo(documento, reporte);
        documento.add(new Paragraph(reporte.junta().getNombre(), titulo));
        documento.add(new Paragraph("RUC: " + reporte.junta().getRuc(), normal));
        documento.add(new Paragraph(reporte.tipo().getEtiqueta(), negrita));
        documento.add(new Paragraph("Rango: " + reporte.inicio().format(FECHA) + " al " + reporte.fin().format(FECHA)
                + " · Generado: " + reporte.generado().format(FECHA_HORA), normal));
        documento.add(new Paragraph(" ", normal));
        PdfPTable tabla = new PdfPTable(reporte.columnas().size() + 1);
        tabla.setWidthPercentage(100);
        for (String columna : reporte.columnas()) {
            tabla.addCell(new PdfPCell(new Phrase(columna, negrita)));
        }
        tabla.addCell(celdaMonto("Monto", negrita));
        for (ReporteService.Fila fila : reporte.filas()) {
            for (String texto : fila.textos()) {
                tabla.addCell(new PdfPCell(new Phrase(texto, normal)));
            }
            tabla.addCell(celdaMonto(moneda(fila.monto()), normal));
        }
        documento.add(tabla);
        documento.add(new Paragraph(" ", normal));
        PdfPTable resumen = new PdfPTable(2);
        resumen.setWidthPercentage(60);
        resumen.setHorizontalAlignment(Element.ALIGN_RIGHT);
        for (ReporteService.Total total : reporte.totales()) {
            resumen.addCell(new PdfPCell(new Phrase(total.etiqueta(), negrita)));
            resumen.addCell(celdaMonto(moneda(total.monto()), negrita));
        }
        documento.add(resumen);
        documento.close();
        return salida.toByteArray();
    }

    private PdfPCell celdaMonto(String texto, Font fuente) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return celda;
    }

    private void agregarLogo(Document documento, ReporteService.Reporte reporte) {
        archivos.logo(reporte.junta().getLogoUrl()).ifPresent(ruta -> {
            try {
                Image logo = Image.getInstance(Files.readAllBytes(ruta));
                logo.scaleToFit(80, 80);
                documento.add(logo);
            } catch (IOException excepcion) {
                throw new UncheckedIOException(excepcion);
            }
        });
    }
}
