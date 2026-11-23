package ec.uta.multijunta.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PdfSimple {

    public byte[] generar(String encabezado, String titulo, List<String> parrafos) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 56, 56, 56, 56);
        PdfWriter.getInstance(documento, salida);
        documento.open();
        Font pequena = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 11);
        documento.add(new Paragraph(encabezado, pequena));
        Paragraph tituloParrafo = new Paragraph(titulo, fuenteTitulo);
        tituloParrafo.setSpacingBefore(12);
        tituloParrafo.setSpacingAfter(12);
        documento.add(tituloParrafo);
        for (String texto : parrafos) {
            Paragraph parrafo = new Paragraph(texto, normal);
            parrafo.setSpacingAfter(8);
            documento.add(parrafo);
        }
        documento.close();
        return salida.toByteArray();
    }
}
