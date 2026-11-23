package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.uta.multijunta.model.CategoriaEgreso;
import ec.uta.multijunta.model.Egreso;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Multa;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.EgresoRepository;
import ec.uta.multijunta.repository.PagoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CU11 Registrar egresos de la junta")
class EgresoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private EgresoRepository egresos;

    @Autowired
    private PagoRepository pagos;

    private Junta junta;
    private Usuario tesorero;

    @BeforeEach
    void preparar() throws Exception {
        junta = fabrica.junta("Junta Egresos");
        tesorero = fabrica.tesorero(junta);
        Socio socio = fabrica.socio(junta, fabrica.sector(junta));
        Multa multa = fabrica.multa(socio, "100.00");
        mvc.perform(post("/pagos/cobrar/" + socio.getId()).with(fabrica.sesion(tesorero)).with(csrf())
                .param("obligaciones", multa.getClave()).param("monto", "100").param("metodo", "EFECTIVO"));
    }

    private MockMultipartHttpServletRequestBuilder formulario(Usuario usuario, String monto) {
        MockMultipartHttpServletRequestBuilder solicitud = multipart("/egresos");
        solicitud.with(fabrica.sesion(usuario)).with(csrf());
        solicitud.param("categoria", "MATERIALES").param("concepto", "Tubería PVC").param("monto", monto)
                .param("fecha", LocalDate.now().toString()).param("proveedor", "Ferretería Central");
        return solicitud;
    }

    private ResultActions registrar(String monto, MockMultipartFile archivo, boolean confirmar) throws Exception {
        MockMultipartHttpServletRequestBuilder solicitud = formulario(tesorero, monto);
        if (archivo != null) {
            solicitud.file(archivo);
        }
        return mvc.perform(solicitud.param("confirmarNegativo", String.valueOf(confirmar)));
    }

    private List<Egreso> egresosDeJunta() {
        return egresos.findByJuntaIdOrderByFechaDescIdDesc(junta.getId());
    }

    private BigDecimal saldo() {
        return pagos.ingresosVigentes(junta.getId()).subtract(egresos.totalDeJunta(junta.getId()));
    }

    private MockMultipartFile pdf() {
        return new MockMultipartFile("comprobante", "factura.pdf", "application/pdf", Fabrica.PDF_MINIMO);
    }

    @Test
    @DisplayName("HU4-1 y HU4-2 registrar con factura y recalcular el saldo actual")
    void registrarConFactura() throws Exception {
        mvc.perform(get("/egresos").with(fabrica.sesion(tesorero)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Saldo Actual")))
                .andExpect(content().string(containsString("$100.00")))
                .andExpect(content().string(containsString("Nuevo Gasto")))
                .andExpect(content().string(containsString("Servicios básicos")));
        registrar("30.00", pdf(), false).andExpect(flash().attribute("exito", "egreso.registrado"));
        Egreso egreso = egresosDeJunta().get(0);
        assertThat(egreso.getCategoria()).isEqualTo(CategoriaEgreso.MATERIALES);
        assertThat(egreso.getProveedor()).isEqualTo("Ferretería Central");
        assertThat(egreso.isPendienteSoporte()).isFalse();
        assertThat(egreso.getRegistradoPor().getId()).isEqualTo(tesorero.getId());
        assertThat(saldo()).isEqualByComparingTo("70.00");
        mvc.perform(get("/egresos").with(fabrica.sesion(tesorero)))
                .andExpect(content().string(containsString("$70.00")))
                .andExpect(content().string(containsString("Tubería PVC")));
        mvc.perform(get("/egresos/" + egreso.getId() + "/comprobante").with(fabrica.sesion(tesorero)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    @DisplayName("HU4-3 sin factura queda 'Pendiente de soporte' y se adjunta después")
    void pendienteDeSoporte() throws Exception {
        registrar("10.00", null, false).andExpect(flash().attribute("exito", "egreso.registrado"));
        Egreso egreso = egresosDeJunta().get(0);
        assertThat(egreso.isPendienteSoporte()).isTrue();
        mvc.perform(get("/egresos").with(fabrica.sesion(tesorero)))
                .andExpect(content().string(containsString("Pendiente de soporte")));
        mvc.perform(get("/egresos/" + egreso.getId() + "/comprobante").with(fabrica.sesion(tesorero)))
                .andExpect(status().isNotFound());
        mvc.perform(multipart("/egresos/" + egreso.getId() + "/comprobante").file(pdf())
                        .with(fabrica.sesion(tesorero)).with(csrf()))
                .andExpect(flash().attribute("exito", "egreso.soporteAdjuntado"));
        assertThat(egresos.findById(egreso.getId()).orElseThrow().isPendienteSoporte()).isFalse();
        mvc.perform(get("/egresos").with(fabrica.sesion(tesorero)))
                .andExpect(content().string(not(containsString("Pendiente de soporte"))));
    }

    @Test
    @DisplayName("HU4-4 fondos insuficientes pide confirmar y permite saldo negativo")
    void fondosInsuficientes() throws Exception {
        registrar("130.00", null, false)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Fondos insuficientes. ¿Forzar registro negativo?")));
        assertThat(egresosDeJunta()).isEmpty();
        registrar("130.00", null, true).andExpect(flash().attribute("exito", "egreso.registrado"));
        assertThat(saldo()).isEqualByComparingTo("-30.00");
        mvc.perform(get("/egresos").with(fabrica.sesion(tesorero)))
                .andExpect(content().string(containsString("-$30.00")));
    }

    @Test
    @DisplayName("HU4-5 y HU4-6 validaciones de categoría, monto, campos y archivo")
    void validaciones() throws Exception {
        mvc.perform(multipart("/egresos").with(fabrica.sesion(tesorero)).with(csrf()).param("categoria", "VIAJES")
                        .param("concepto", "Viaje").param("monto", "10").param("fecha", LocalDate.now().toString()))
                .andExpect(content().string(containsString("El valor no es válido")));
        registrar("0", null, false).andExpect(content().string(containsString("El monto debe ser mayor que cero")));
        registrar("1.234", null, false).andExpect(content().string(containsString("El monto admite hasta dos decimales")));
        mvc.perform(multipart("/egresos").with(fabrica.sesion(tesorero)).with(csrf())
                        .param("categoria", "OTROS").param("monto", "5"))
                .andExpect(content().string(containsString("Este campo es obligatorio")));
        registrar("5", new MockMultipartFile("comprobante", "nota.txt", "text/plain", "hola".getBytes()), false)
                .andExpect(content().string(containsString("Formato no permitido. Use PDF, JPG o PNG")));
        assertThat(egresosDeJunta()).isEmpty();
    }

    @Test
    @DisplayName("HU4-7 ADMIN_JUNTA solo lectura; otra junta responde 404")
    void permisosYAislamiento() throws Exception {
        registrar("10.00", pdf(), false);
        Egreso egreso = egresosDeJunta().get(0);
        Usuario admin = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        mvc.perform(get("/egresos").with(fabrica.sesion(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tubería PVC")))
                .andExpect(content().string(not(containsString("Nuevo Gasto"))));
        mvc.perform(get("/egresos/" + egreso.getId() + "/comprobante").with(fabrica.sesion(admin)))
                .andExpect(status().isOk());
        mvc.perform(formulario(admin, "5")).andExpect(status().isForbidden());
        mvc.perform(multipart("/egresos/" + egreso.getId() + "/comprobante").file(pdf())
                .with(fabrica.sesion(admin)).with(csrf())).andExpect(status().isForbidden());
        Junta otra = fabrica.junta("Junta Ajena Egresos");
        Usuario ajeno = fabrica.tesorero(otra);
        mvc.perform(get("/egresos/" + egreso.getId() + "/comprobante").with(fabrica.sesion(ajeno)))
                .andExpect(status().isNotFound());
        mvc.perform(multipart("/egresos/" + egreso.getId() + "/comprobante").file(pdf())
                .with(fabrica.sesion(ajeno)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/egresos").with(fabrica.sesion(ajeno)))
                .andExpect(content().string(not(containsString("Tubería PVC"))));
    }
}
