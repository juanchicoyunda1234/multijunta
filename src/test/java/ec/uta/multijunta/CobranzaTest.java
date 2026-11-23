package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.uta.multijunta.model.Cuota;
import ec.uta.multijunta.model.EstadoObligacion;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.MetodoPago;
import ec.uta.multijunta.model.Multa;
import ec.uta.multijunta.model.Pago;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.CuotaRepository;
import ec.uta.multijunta.repository.MultaRepository;
import ec.uta.multijunta.repository.PagoRepository;
import java.time.LocalDate;
import java.util.List;
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
@DisplayName("CU09 Registrar ingresos por pagos")
class CobranzaTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private PagoRepository pagos;

    @Autowired
    private CuotaRepository cuotas;

    @Autowired
    private MultaRepository multas;

    private Junta junta;
    private Usuario tesorero;
    private Socio deudor;
    private Socio alDia;
    private Cuota cuota;
    private Multa multa;

    @BeforeEach
    void preparar() {
        junta = fabrica.junta("Junta Cobranzas");
        Sector sector = fabrica.sector(junta);
        tesorero = fabrica.tesorero(junta);
        deudor = fabrica.socio(junta, sector);
        alDia = fabrica.socio(junta, sector);
        cuota = fabrica.cuota(deudor, "Canon de riego", "12.50", LocalDate.now().minusDays(10));
        multa = fabrica.multa(deudor, "5.00");
    }

    private ResultActions cobrar(Usuario usuario, Socio socio, String monto, boolean confirmar, String... claves)
            throws Exception {
        return mvc.perform(post("/pagos/cobrar/" + socio.getId()).with(fabrica.sesion(usuario)).with(csrf())
                .param("obligaciones", claves).param("monto", monto).param("metodo", "EFECTIVO")
                .param("confirmarParcial", String.valueOf(confirmar)));
    }

    private List<Pago> pagosDeJunta() {
        return pagos.buscar(junta.getId(), null);
    }

    @Test
    @DisplayName("HU2-1 cobranzas con deuda, 'Al día', cuotas vencidas, botón Cobrar en cada fila y búsqueda")
    void listaCobranzas() throws Exception {
        mvc.perform(get("/pagos").with(fabrica.sesion(tesorero)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(deudor.getNombreCompleto())))
                .andExpect(content().string(containsString("$17.50")))
                .andExpect(content().string(containsString("Al día")))
                .andExpect(content().string(containsString("href=\"/pagos/cobrar/" + deudor.getId() + "\"")))
                .andExpect(content().string(containsString("href=\"/pagos/cobrar/" + alDia.getId() + "\"")))
                .andExpect(content().string(containsString("Vencida")));
        mvc.perform(get("/pagos/cobrar/" + alDia.getId()).with(fabrica.sesion(tesorero)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El socio no tiene obligaciones pendientes")));
        mvc.perform(get("/pagos").param("q", deudor.getCedula()).with(fabrica.sesion(tesorero)))
                .andExpect(content().string(containsString(deudor.getNombreCompleto())))
                .andExpect(content().string(not(containsString(alDia.getNombreCompleto()))));
    }

    @Test
    @DisplayName("HU2-2 el cobro lista solo obligaciones pendientes o parciales")
    void formularioDeCobro() throws Exception {
        Cuota anulada = fabrica.cuota(deudor, "Cuota anulada", "3.00", LocalDate.now());
        anulada.anular("Error");
        cuotas.save(anulada);
        Cuota pagada = fabrica.cuota(deudor, "Cuota pagada", "3.00", LocalDate.now());
        pagada.aplicar(pagada.getMonto());
        cuotas.save(pagada);
        mvc.perform(get("/pagos/cobrar/" + deudor.getId()).with(fabrica.sesion(tesorero)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Canon de riego")))
                .andExpect(content().string(containsString("Inasistencia a Minga")))
                .andExpect(content().string(not(containsString("Cuota anulada"))))
                .andExpect(content().string(not(containsString("Cuota pagada"))))
                .andExpect(content().string(containsString("$17.50")))
                .andExpect(content().string(containsString("Vencida")))
                .andExpect(content().string(containsString("Transferencia")));
    }

    @Test
    @DisplayName("HU2-3 pago completo con número de transacción y estado 'Al día'")
    void pagoCompleto() throws Exception {
        cobrar(tesorero, deudor, "17.50", false, cuota.getClave(), multa.getClave())
                .andExpect(flash().attribute("exito", "pago.registrado"));
        Pago pago = pagosDeJunta().get(0);
        assertThat(pago.getNumero()).isEqualTo("TX-000001");
        assertThat(pago.getMonto()).isEqualByComparingTo("17.50");
        assertThat(pago.getMetodo()).isEqualTo(MetodoPago.EFECTIVO);
        assertThat(pago.getRegistradoPor().getId()).isEqualTo(tesorero.getId());
        assertThat(pago.getAplicaciones()).hasSize(2);
        assertThat(cuotas.findById(cuota.getId()).orElseThrow().getEstado()).isEqualTo(EstadoObligacion.PAGADA);
        assertThat(multas.findById(multa.getId()).orElseThrow().getSaldoPendiente()).isEqualByComparingTo("0");
        mvc.perform(get("/pagos/" + pago.getId()).with(fabrica.sesion(tesorero)))
                .andExpect(content().string(containsString("TX-000001")))
                .andExpect(content().string(containsString("Al día")))
                .andExpect(content().string(containsString("Imprimir Recibo")));
        Multa otra = fabrica.multa(deudor, "2.00");
        cobrar(tesorero, deudor, "2", false, otra.getClave());
        assertThat(pagosDeJunta()).extracting(Pago::getNumero).contains("TX-000002");
        Junta ajena = fabrica.junta("Junta Numeración Propia");
        Socio socioAjeno = fabrica.socio(ajena, fabrica.sector(ajena));
        Multa multaAjena = fabrica.multa(socioAjeno, "1.00");
        cobrar(fabrica.tesorero(ajena), socioAjeno, "1", false, multaAjena.getClave());
        assertThat(pagos.buscar(ajena.getId(), null)).extracting(Pago::getNumero).containsExactly("TX-000001");
    }

    @Test
    @DisplayName("HU2-4 abono parcial pide confirmación y se aplica de la más antigua")
    void abonoParcial() throws Exception {
        cobrar(tesorero, deudor, "14.00", false, cuota.getClave(), multa.getClave())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El monto es menor al total. ¿Registrar como abono parcial?")));
        assertThat(pagosDeJunta()).isEmpty();
        cobrar(tesorero, deudor, "14.00", true, cuota.getClave(), multa.getClave())
                .andExpect(flash().attribute("exito", "pago.registrado"));
        Cuota cuotaActual = cuotas.findById(cuota.getId()).orElseThrow();
        Multa multaActual = multas.findById(multa.getId()).orElseThrow();
        assertThat(cuotaActual.getEstado()).isEqualTo(EstadoObligacion.PAGADA);
        assertThat(multaActual.getEstado()).isEqualTo(EstadoObligacion.PARCIAL);
        assertThat(multaActual.getSaldoPendiente()).isEqualByComparingTo("3.50");
    }

    @Test
    @DisplayName("HU2-5 montos y selección inválidos")
    void montosInvalidos() throws Exception {
        for (String monto : new String[] {"0", "-1", "20.00"}) {
            cobrar(tesorero, deudor, monto, true, cuota.getClave())
                    .andExpect(content().string(containsString(
                            "El monto debe ser mayor que cero y no superar el total seleccionado")));
        }
        cobrar(tesorero, deudor, "12.505", true, cuota.getClave())
                .andExpect(content().string(containsString("El monto admite hasta dos decimales")));
        mvc.perform(post("/pagos/cobrar/" + deudor.getId()).with(fabrica.sesion(tesorero)).with(csrf())
                        .param("monto", "5").param("metodo", "EFECTIVO"))
                .andExpect(content().string(containsString("Seleccione al menos una obligación")));
        Multa deOtroSocio = fabrica.multa(alDia, "4.00");
        cobrar(tesorero, deudor, "4", true, deOtroSocio.getClave())
                .andExpect(content().string(containsString("Las obligaciones cambiaron; vuelva a cargar el cobro")));
        assertThat(pagosDeJunta()).isEmpty();
    }

    @Test
    @DisplayName("HU2-6 y HU2-7 recibo imprimible; los pagos no se editan ni se borran")
    void reciboYPagoInmutable() throws Exception {
        cobrar(tesorero, deudor, "12.50", false, cuota.getClave());
        Pago pago = pagosDeJunta().get(0);
        mvc.perform(get("/pagos/" + pago.getId() + "/recibo").with(fabrica.sesion(tesorero)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(junta.getNombre())))
                .andExpect(content().string(containsString(junta.getRuc())))
                .andExpect(content().string(containsString("TX-000001")))
                .andExpect(content().string(containsString("Canon de riego")))
                .andExpect(content().string(containsString("$12.50")));
        int borrar = mvc.perform(delete("/pagos/" + pago.getId()).with(fabrica.sesion(tesorero)).with(csrf()))
                .andReturn().getResponse().getStatus();
        assertThat(borrar).isIn(404, 405);
        mvc.perform(post("/pagos/" + pago.getId() + "/editar").with(fabrica.sesion(tesorero)).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(pagos.findById(pago.getId())).isPresent();
    }

    @Test
    @DisplayName("HU2-8 ADMIN_JUNTA solo lectura; otra junta responde 404")
    void permisosYAislamiento() throws Exception {
        cobrar(tesorero, deudor, "12.50", false, cuota.getClave());
        Pago pago = pagosDeJunta().get(0);
        Usuario admin = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        mvc.perform(get("/pagos").with(fabrica.sesion(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("href=\"/pagos/cobrar/"))));
        mvc.perform(get("/pagos/" + pago.getId()).with(fabrica.sesion(admin))).andExpect(status().isOk());
        cobrar(admin, deudor, "5", true, multa.getClave()).andExpect(status().isForbidden());
        Usuario secretario = fabrica.secretario(junta);
        mvc.perform(get("/pagos").with(fabrica.sesion(secretario))).andExpect(status().isForbidden());
        Junta otra = fabrica.junta("Junta Ajena Cobranzas");
        Usuario ajeno = fabrica.tesorero(otra);
        mvc.perform(get("/pagos/" + pago.getId()).with(fabrica.sesion(ajeno))).andExpect(status().isNotFound());
        mvc.perform(get("/pagos/cobrar/" + deudor.getId()).with(fabrica.sesion(ajeno))).andExpect(status().isNotFound());
        cobrar(ajeno, deudor, "5", true, multa.getClave()).andExpect(status().isNotFound());
        mvc.perform(get("/pagos").with(fabrica.sesion(ajeno)))
                .andExpect(content().string(not(containsString(deudor.getNombreCompleto()))));
    }
}
