package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import ec.uta.multijunta.model.CategoriaEgreso;
import ec.uta.multijunta.model.Cuota;
import ec.uta.multijunta.model.Egreso;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Multa;
import ec.uta.multijunta.model.Pago;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.CuotaRepository;
import ec.uta.multijunta.repository.EgresoRepository;
import ec.uta.multijunta.repository.PagoRepository;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CU12 Gestionar comprobantes y emitir reportes")
class ReporteTest {

    private static final DateTimeFormatter COMPACTA = DateTimeFormatter.BASIC_ISO_DATE;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private PagoRepository pagos;

    @Autowired
    private EgresoRepository egresos;

    @Autowired
    private CuotaRepository cuotas;

    private Junta junta;
    private Usuario tesorero;
    private Socio socio;
    private final LocalDate hoy = LocalDate.now();

    @BeforeEach
    void preparar() throws Exception {
        junta = fabrica.junta("Junta Reportes");
        tesorero = fabrica.tesorero(junta);
        socio = fabrica.socio(junta, fabrica.sector(junta));
        Cuota vencida = fabrica.cuota(socio, "Tarifa anual vencida", "20.00", hoy.minusDays(10));
        fabrica.cuota(socio, "Consumo por vencer", "7.00", hoy.plusDays(10));
        Cuota anulada = fabrica.cuota(socio, "Cuota anulada", "9.00", hoy.minusDays(5));
        anulada.anular("Error");
        cuotas.save(anulada);
        Multa multa = fabrica.multa(socio, "5.00");
        cobrar(multa.getClave(), "5.00");
        cobrar(vencida.getClave(), "8.00");
        Pago parcial = pagos.buscar(junta.getId(), "TX-000002").get(0);
        mvc.perform(post("/pagos/" + parcial.getId() + "/reversar").with(fabrica.sesion(tesorero)).with(csrf())
                .param("motivo", "Cheque sin fondos"));
        mvc.perform(multipart("/egresos").with(fabrica.sesion(tesorero)).with(csrf()).param("categoria", "MATERIALES")
                .param("concepto", "Tubería PVC").param("monto", "3.00").param("fecha", hoy.toString()));
        Junta otra = fabrica.junta("Junta Ajena Reportes");
        Socio ajeno = fabrica.socio(otra, fabrica.sector(otra));
        Multa multaAjena = fabrica.multa(ajeno, "99.00");
        mvc.perform(post("/pagos/cobrar/" + ajeno.getId()).with(fabrica.sesion(fabrica.tesorero(otra))).with(csrf())
                .param("obligaciones", multaAjena.getClave()).param("monto", "99").param("metodo", "EFECTIVO"));
    }

    private void cobrar(String clave, String monto) throws Exception {
        mvc.perform(post("/pagos/cobrar/" + socio.getId()).with(fabrica.sesion(tesorero)).with(csrf())
                .param("obligaciones", clave).param("monto", monto).param("metodo", "EFECTIVO")
                .param("confirmarParcial", "true"));
    }

    private ResultActions generar(Usuario usuario, String tipo, LocalDate inicio, LocalDate fin, String formato)
            throws Exception {
        return mvc.perform(post("/reportes").with(fabrica.sesion(usuario)).with(csrf()).param("tipo", tipo)
                .param("inicio", inicio.toString()).param("fin", fin.toString()).param("formato", formato));
    }

    private String textoPdf(byte[] bytes) throws Exception {
        PdfReader lector = new PdfReader(bytes);
        StringBuilder texto = new StringBuilder();
        PdfTextExtractor extractor = new PdfTextExtractor(lector);
        for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
            texto.append(extractor.getTextFromPage(pagina)).append('\n');
        }
        lector.close();
        return texto.toString().replaceAll("\\s+", " ");
    }

    private String textoExcel(byte[] bytes) throws Exception {
        StringBuilder texto = new StringBuilder();
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet hoja = libro.getSheetAt(0);
            for (Row fila : hoja) {
                for (Cell celda : fila) {
                    texto.append(celda.toString()).append(" | ");
                }
                texto.append('\n');
            }
        }
        return texto.toString();
    }

    @Test
    @DisplayName("HU8-1 formulario y vista previa con el saldo del rango")
    void formularioYVistaPrevia() throws Exception {
        mvc.perform(get("/reportes").with(fabrica.sesion(tesorero)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Balance General de Ingresos y Egresos")))
                .andExpect(content().string(containsString("Socios Morosos")))
                .andExpect(content().string(containsString("Generar Reporte")));
        mvc.perform(get("/reportes").param("tipo", "BALANCE").param("inicio", hoy.toString()).param("fin", hoy.toString())
                        .with(fabrica.sesion(tesorero)))
                .andExpect(content().string(containsString("Saldo del rango: $2.00")));
    }

    @Test
    @DisplayName("HU8-2 y HU8-5 Balance en PDF con nombre, RUC, pago, reverso negativo, egresos y saldo")
    void balancePdf() throws Exception {
        byte[] pdf = generar(tesorero, "BALANCE", hoy, hoy, "PDF")
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString(
                        "balance-" + hoy.format(COMPACTA) + "-" + hoy.format(COMPACTA) + ".pdf")))
                .andExpect(content().contentType("application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();
        String texto = textoPdf(pdf);
        assertThat(texto).contains("Junta Reportes", junta.getRuc(), "Balance General de Ingresos y Egresos",
                "TX-000001", "TX-000002", "TX-000003", "-$8.00", "Materiales", "-$3.00", "Saldo del rango", "$2.00");
        assertThat(texto).doesNotContain("$99.00");
    }

    @Test
    @DisplayName("HU8-2 Balance en Excel con las mismas filas")
    void balanceExcel() throws Exception {
        byte[] excel = generar(tesorero, "BALANCE", hoy, hoy, "EXCEL")
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString(".xlsx")))
                .andReturn().getResponse().getContentAsByteArray();
        String texto = textoExcel(excel);
        assertThat(texto).contains("Junta Reportes", "TX-000003", "Tubería PVC", "Saldo del rango", "-8.0", "2.0");
    }

    @Test
    @DisplayName("HU8-3 Socios Morosos con deuda vencida acumulada hasta la fecha de fin")
    void morosos() throws Exception {
        byte[] pdf = generar(tesorero, "MOROSOS", hoy, hoy, "PDF").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        String texto = textoPdf(pdf);
        assertThat(texto).contains("Socios Morosos", socio.getNombreCompleto(), "Tarifa anual vencida", "$20.00");
        assertThat(texto).doesNotContain("Consumo por vencer").doesNotContain("Cuota anulada")
                .doesNotContain("Inasistencia a Minga");
    }

    @Test
    @DisplayName("HU8-4 y HU8-6 rango sin registros y rango inválido")
    void rangos() throws Exception {
        LocalDate antiguo = LocalDate.of(2020, 1, 1);
        generar(tesorero, "BALANCE", antiguo, antiguo.plusDays(30), "PDF")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No existen registros en el rango seleccionado")));
        generar(tesorero, "MOROSOS", antiguo, antiguo.plusDays(30), "EXCEL")
                .andExpect(content().string(containsString("No existen registros en el rango seleccionado")));
        generar(tesorero, "BALANCE", hoy, hoy.minusDays(1), "PDF")
                .andExpect(content().string(containsString("La fecha de fin debe ser igual o posterior a la de inicio")));
    }

    @Test
    @DisplayName("HU8-7 ADMIN_JUNTA descarga; SECRETARIO recibe 403")
    void permisos() throws Exception {
        generar(fabrica.usuario(Rol.ADMIN_JUNTA, junta), "BALANCE", hoy, hoy, "EXCEL").andExpect(status().isOk());
        Usuario secretario = fabrica.secretario(junta);
        mvc.perform(get("/reportes").with(fabrica.sesion(secretario))).andExpect(status().isForbidden());
        generar(secretario, "BALANCE", hoy, hoy, "PDF").andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SC-004 el saldo del Balance desde el inicio coincide con el saldo actual")
    void saldoCoincide() throws Exception {
        BigDecimal saldoActual = pagos.ingresosVigentes(junta.getId()).subtract(egresos.totalDeJunta(junta.getId()));
        assertThat(saldoActual).isEqualByComparingTo("2.00");
        mvc.perform(get("/reportes").param("tipo", "BALANCE").param("inicio", "2000-01-01")
                        .param("fin", hoy.toString()).with(fabrica.sesion(tesorero)))
                .andExpect(content().string(containsString("Saldo del rango: $2.00")));
    }

    @Test
    @DisplayName("SC-006 reporte mensual de 500 movimientos en menos de 10 segundos")
    void rendimientoQuinientos() throws Exception {
        Usuario registrador = fabrica.usuario(Rol.TESORERO, junta);
        for (int indice = 0; indice < 500; indice++) {
            Egreso egreso = new Egreso();
            egreso.setJunta(junta);
            egreso.setCategoria(CategoriaEgreso.OTROS);
            egreso.setConcepto("Gasto " + indice);
            egreso.setMonto(new BigDecimal("1.00"));
            egreso.setFecha(hoy.withDayOfMonth(1));
            egreso.setRegistradoPor(registrador);
            egresos.save(egreso);
        }
        long inicio = System.nanoTime();
        byte[] pdf = generar(tesorero, "BALANCE", hoy.withDayOfMonth(1), hoy, "PDF").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        byte[] excel = generar(tesorero, "BALANCE", hoy.withDayOfMonth(1), hoy, "EXCEL").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        long milisegundos = (System.nanoTime() - inicio) / 1_000_000;
        assertThat(milisegundos).isLessThan(10000);
        assertThat(textoPdf(pdf)).contains("Gasto 499");
        assertThat(textoExcel(excel)).contains("Gasto 499");
    }
}
