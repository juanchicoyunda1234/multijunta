package ec.uta.multijunta.service;

import ec.uta.multijunta.model.TipoReporte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ReporteExcel {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generar(ReporteService.Reporte reporte) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet(reporte.tipo() == TipoReporte.MOROSOS
                    ? "Socios morosos" : "Balance");
            Font fuenteNegrita = libro.createFont();
            fuenteNegrita.setBold(true);
            CellStyle negrita = libro.createCellStyle();
            negrita.setFont(fuenteNegrita);
            DataFormat formatos = libro.createDataFormat();
            CellStyle moneda = libro.createCellStyle();
            moneda.setDataFormat(formatos.getFormat("$#,##0.00;-$#,##0.00"));
            int numero = 0;
            fila(hoja, numero++, negrita, reporte.junta().getNombre());
            fila(hoja, numero++, null, "RUC: " + reporte.junta().getRuc());
            fila(hoja, numero++, negrita, reporte.tipo().getEtiqueta());
            fila(hoja, numero++, null, "Rango: " + reporte.inicio().format(FECHA) + " al " + reporte.fin().format(FECHA));
            numero++;
            Row encabezado = hoja.createRow(numero++);
            int columna = 0;
            for (String titulo : reporte.columnas()) {
                encabezado.createCell(columna).setCellValue(titulo);
                encabezado.getCell(columna++).setCellStyle(negrita);
            }
            encabezado.createCell(columna).setCellValue("Monto");
            encabezado.getCell(columna).setCellStyle(negrita);
            for (ReporteService.Fila fila : reporte.filas()) {
                Row registro = hoja.createRow(numero++);
                int celda = 0;
                for (String texto : fila.textos()) {
                    registro.createCell(celda++).setCellValue(texto);
                }
                registro.createCell(celda).setCellValue(fila.monto().doubleValue());
                registro.getCell(celda).setCellStyle(moneda);
            }
            numero++;
            for (ReporteService.Total total : reporte.totales()) {
                Row registro = hoja.createRow(numero++);
                registro.createCell(0).setCellValue(total.etiqueta());
                registro.getCell(0).setCellStyle(negrita);
                registro.createCell(reporte.columnas().size()).setCellValue(total.monto().doubleValue());
                registro.getCell(reporte.columnas().size()).setCellStyle(moneda);
            }
            for (int indice = 0; indice <= reporte.columnas().size(); indice++) {
                hoja.setColumnWidth(indice, indice == reporte.columnas().size() - 1 ? 60 * 256 : 18 * 256);
            }
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
    }

    private void fila(Sheet hoja, int numero, CellStyle estilo, String texto) {
        Row registro = hoja.createRow(numero);
        registro.createCell(0).setCellValue(texto);
        if (estilo != null) {
            registro.getCell(0).setCellStyle(estilo);
        }
    }
}
