package ec.uta.multijunta.rest;

import ec.uta.multijunta.model.FormatoReporte;
import ec.uta.multijunta.model.TipoReporte;
import ec.uta.multijunta.rest.formulario.ReporteFormulario;
import ec.uta.multijunta.service.DatosReporte;
import ec.uta.multijunta.service.ReglaNegocioException;
import ec.uta.multijunta.service.ReporteExcel;
import ec.uta.multijunta.service.ReportePdf;
import ec.uta.multijunta.service.ReporteService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private static final DateTimeFormatter COMPACTA = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ReporteService reportes;
    private final ReportePdf pdf;
    private final ReporteExcel excel;

    public ReporteController(ReporteService reportes, ReportePdf pdf, ReporteExcel excel) {
        this.reportes = reportes;
        this.pdf = pdf;
        this.excel = excel;
    }

    @GetMapping
    public String formulario(@ModelAttribute("reporteFormulario") ReporteFormulario formulario,
            BindingResult resultado, Model modelo) {
        if (!resultado.hasErrors() && formulario.getTipo() != null && formulario.getInicio() != null
                && formulario.getFin() != null) {
            try {
                ReporteService.Reporte reporte = reportes.generar(formulario.getTipo(), formulario.getInicio(),
                        formulario.getFin());
                modelo.addAttribute("previa", reporte);
                modelo.addAttribute("previaTexto", (reporte.tipo() == TipoReporte.MOROSOS ? "Deuda en mora: "
                        : "Saldo del rango: ") + ReportePdf.moneda(reporte.resultado()));
            } catch (ReglaNegocioException excepcion) {
                modelo.addAttribute("previaVacia", excepcion.getClave());
            }
        }
        return mostrar(modelo);
    }

    @PostMapping
    public String generar(@Valid @ModelAttribute("reporteFormulario") ReporteFormulario formulario,
            BindingResult resultado, Model modelo, HttpServletResponse respuesta) throws IOException {
        if (!resultado.hasErrors()) {
            DatosReporte datos = formulario.datos();
            try {
                ReporteService.Reporte reporte = reportes.generar(datos.tipo(), datos.inicio(), datos.fin());
                boolean esPdf = datos.formato() == FormatoReporte.PDF;
                byte[] contenido = esPdf ? pdf.generar(reporte) : excel.generar(reporte);
                String nombre = (datos.tipo() == TipoReporte.MOROSOS ? "morosos-" : "balance-")
                        + datos.inicio().format(COMPACTA) + "-" + datos.fin().format(COMPACTA)
                        + (esPdf ? ".pdf" : ".xlsx");
                respuesta.setContentType(esPdf ? "application/pdf" : EXCEL);
                respuesta.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombre).build().toString());
                respuesta.setContentLength(contenido.length);
                respuesta.getOutputStream().write(contenido);
                respuesta.flushBuffer();
                return null;
            } catch (ReglaNegocioException excepcion) {
                if (excepcion.getCampo() != null) {
                    resultado.rejectValue(excepcion.getCampo(), excepcion.getClave());
                } else {
                    modelo.addAttribute("error", excepcion.getClave());
                }
            }
        }
        return mostrar(modelo);
    }

    private String mostrar(Model modelo) {
        modelo.addAttribute("tipos", TipoReporte.values());
        modelo.addAttribute("formatos", FormatoReporte.values());
        return "reportes/formulario";
    }
}
