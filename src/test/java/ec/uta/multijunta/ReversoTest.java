package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.uta.multijunta.model.Cuota;
import ec.uta.multijunta.model.EstadoObligacion;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Multa;
import ec.uta.multijunta.model.Pago;
import ec.uta.multijunta.model.ReversoPago;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.CuotaRepository;
import ec.uta.multijunta.repository.MultaRepository;
import ec.uta.multijunta.repository.PagoRepository;
import ec.uta.multijunta.repository.ReversoPagoRepository;
import java.time.LocalDate;
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
@DisplayName("CU31 Reversar un pago")
class ReversoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private PagoRepository pagos;

    @Autowired
    private ReversoPagoRepository reversos;

    @Autowired
    private CuotaRepository cuotas;

    @Autowired
    private MultaRepository multas;

    private Junta junta;
    private Usuario tesorero;
    private Cuota cuota;
    private Multa multa;
    private Pago primero;
    private Pago segundo;

    @BeforeEach
    void preparar() throws Exception {
        junta = fabrica.junta("Junta Reversos");
        tesorero = fabrica.tesorero(junta);
        Socio socio = fabrica.socio(junta, fabrica.sector(junta));
        cuota = fabrica.cuota(socio, "Canon de riego", "12.50", LocalDate.now().minusDays(10));
        multa = fabrica.multa(socio, "5.00");
        cobrar(socio, "14.00", cuota.getClave(), multa.getClave());
        cobrar(socio, "3.50", multa.getClave());
        primero = pagos.buscar(junta.getId(), "TX-000001").get(0);
        segundo = pagos.buscar(junta.getId(), "TX-000002").get(0);
    }

    private void cobrar(Socio socio, String monto, String... claves) throws Exception {
        mvc.perform(post("/pagos/cobrar/" + socio.getId()).with(fabrica.sesion(tesorero)).with(csrf())
                .param("obligaciones", claves).param("monto", monto).param("metodo", "TRANSFERENCIA")
                .param("confirmarParcial", "true"));
    }

    private ResultActions reversar(Usuario usuario, Pago pago, String motivo) throws Exception {
        return mvc.perform(post("/pagos/" + pago.getId() + "/reversar").with(fabrica.sesion(usuario)).with(csrf())
                .param("motivo", motivo).param("confirmar", "true"));
    }

    @Test
    @DisplayName("HU5-1 y HU5-2 reverso con número propio que devuelve exactamente lo aplicado")
    void reversoExacto() throws Exception {
        assertThat(multas.findById(multa.getId()).orElseThrow().getEstado()).isEqualTo(EstadoObligacion.PAGADA);
        reversar(tesorero, primero, "Pago registrado al socio equivocado")
                .andExpect(flash().attribute("exito", "pago.reversado"));
        ReversoPago reverso = reversos.findByPagoId(primero.getId()).orElseThrow();
        assertThat(reverso.getNumero()).isEqualTo("TX-000003");
        assertThat(reverso.getMonto()).isEqualByComparingTo("14.00");
        assertThat(reverso.getRegistradoPor().getId()).isEqualTo(tesorero.getId());
        assertThat(pagos.findById(primero.getId()).orElseThrow().isReversado()).isTrue();
        Cuota cuotaActual = cuotas.findById(cuota.getId()).orElseThrow();
        assertThat(cuotaActual.getEstado()).isEqualTo(EstadoObligacion.PENDIENTE);
        assertThat(cuotaActual.getSaldoPendiente()).isEqualByComparingTo("12.50");
        Multa multaActual = multas.findById(multa.getId()).orElseThrow();
        assertThat(multaActual.getEstado()).isEqualTo(EstadoObligacion.PARCIAL);
        assertThat(multaActual.getSaldoPendiente()).isEqualByComparingTo("1.50");
        assertThat(pagos.ingresosVigentes(junta.getId())).isEqualByComparingTo("3.50");
        assertThat(pagos.findById(segundo.getId()).orElseThrow().isReversado()).isFalse();
    }

    @Test
    @DisplayName("HU5-3 y HU5-4 motivo obligatorio y sin doble reverso")
    void validaciones() throws Exception {
        reversar(tesorero, primero, " ").andExpect(flash().attribute("error", "motivo.obligatorio"));
        assertThat(pagos.findById(primero.getId()).orElseThrow().isReversado()).isFalse();
        reversar(tesorero, primero, "Error de digitación").andExpect(flash().attribute("exito", "pago.reversado"));
        reversar(tesorero, primero, "Otra vez").andExpect(flash().attribute("error", "pago.yaReversado"));
        assertThat(reversos.countByJuntaId(junta.getId())).isEqualTo(1);
        mvc.perform(get("/pagos/" + primero.getId()).with(fabrica.sesion(tesorero)))
                .andExpect(content().string(not(containsString("data-bs-target=\"#modalReversar\""))));
    }

    @Test
    @DisplayName("HU5-5 el pago reversado sigue visible con su reverso y la cuota vuelve a poder anularse")
    void pagoVisible() throws Exception {
        reversar(tesorero, primero, "Error de digitación");
        mvc.perform(get("/pagos/historial").with(fabrica.sesion(tesorero)))
                .andExpect(content().string(containsString("TX-000001")))
                .andExpect(content().string(containsString("Reversado (TX-000003)")));
        mvc.perform(get("/pagos/" + primero.getId()).with(fabrica.sesion(tesorero)))
                .andExpect(content().string(containsString("Reversado")))
                .andExpect(content().string(containsString("Error de digitación")))
                .andExpect(content().string(containsString("TX-000003")));
        mvc.perform(post("/cuotas/" + cuota.getId() + "/anular").with(fabrica.sesion(tesorero)).with(csrf())
                .param("motivo", "Emitida por error")).andExpect(flash().attribute("exito", "cuota.anulada"));
    }

    @Test
    @DisplayName("HU5-6 solo el tesorero reversa; otra junta responde 404")
    void permisos() throws Exception {
        Usuario admin = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        mvc.perform(get("/pagos/" + primero.getId()).with(fabrica.sesion(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("data-bs-target=\"#modalReversar\""))));
        reversar(admin, primero, "x").andExpect(status().isForbidden());
        Junta otra = fabrica.junta("Junta Ajena Reversos");
        reversar(fabrica.tesorero(otra), primero, "x").andExpect(status().isNotFound());
        assertThat(pagos.findById(primero.getId()).orElseThrow().isReversado()).isFalse();
    }
}
