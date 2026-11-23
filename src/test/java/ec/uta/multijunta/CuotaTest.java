package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.uta.multijunta.model.Cuota;
import ec.uta.multijunta.model.EstadoObligacion;
import ec.uta.multijunta.model.EstadoSocio;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.CuotaRepository;
import java.math.BigDecimal;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CU30 Registrar cuotas y obligaciones")
class CuotaTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private CuotaRepository cuotas;

    private Junta junta;
    private Sector norte;
    private Sector sur;
    private Usuario tesorero;
    private Socio ana;
    private Socio beto;
    private Socio carla;

    @BeforeEach
    void preparar() {
        junta = fabrica.junta("Junta Cuotas");
        norte = fabrica.sector(junta);
        sur = fabrica.sector(junta);
        tesorero = fabrica.tesorero(junta);
        ana = fabrica.socio(junta, norte);
        beto = fabrica.socio(junta, norte);
        carla = fabrica.socio(junta, sur);
        fabrica.socioSinCuenta(junta, norte, EstadoSocio.INACTIVO);
    }

    private MockHttpServletRequestBuilder formulario(String ruta, String destino, String concepto, String monto) {
        return post(ruta).with(fabrica.sesion(tesorero)).with(csrf())
                .param("destino", destino).param("concepto", concepto).param("monto", monto)
                .param("fechaVencimiento", LocalDate.now().plusDays(15).toString())
                .param("sectorId", norte.getId().toString()).param("socioId", ana.getId().toString());
    }

    private ResultActions emitir(String destino, String concepto, String monto) throws Exception {
        return mvc.perform(formulario("/cuotas", destino, concepto, monto));
    }

    private List<Cuota> todas() {
        return cuotas.buscar(junta.getId(), null, null);
    }

    @Test
    @DisplayName("HU1-1 y HU1-2 formulario con tres destinos y vista previa sin guardar")
    void formularioYVistaPrevia() throws Exception {
        mvc.perform(get("/cuotas/emitir").with(fabrica.sesion(tesorero)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Todos los socios activos")))
                .andExpect(content().string(containsString("Un sector")))
                .andExpect(content().string(containsString("Un socio")));
        mvc.perform(formulario("/cuotas/previa", "TODOS", "Tarifa anual 2026", "12.50"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Se crearán 3 cuotas por un total de $37.50")));
        assertThat(todas()).isEmpty();
    }

    @Test
    @DisplayName("HU1-3 emitir a todos los activos, a un sector y a un socio")
    void emitirPorDestino() throws Exception {
        emitir("TODOS", "Tarifa anual 2026", "12.50").andExpect(flash().attribute("exito", "cuota.emitidas"));
        assertThat(todas()).hasSize(3).allSatisfy(cuota -> {
            assertThat(cuota.getEstado()).isEqualTo(EstadoObligacion.PENDIENTE);
            assertThat(cuota.getSaldoPendiente()).isEqualByComparingTo("12.50");
            assertThat(cuota.getFecha()).isEqualTo(LocalDate.now());
            assertThat(cuota.getSocio().getEstado()).isEqualTo(EstadoSocio.ACTIVO);
        });
        emitir("SECTOR", "Limpieza canal norte", "4").andExpect(flash().attribute("exito", "cuota.emitidas"));
        assertThat(todas()).filteredOn(c -> c.getConcepto().equals("Limpieza canal norte"))
                .extracting(c -> c.getSocio().getId()).containsExactlyInAnyOrder(ana.getId(), beto.getId());
        emitir("SOCIO", "Consumo de medidor", "3.50").andExpect(flash().attribute("exito", "cuota.emitidas"));
        assertThat(todas()).filteredOn(c -> c.getConcepto().equals("Consumo de medidor"))
                .extracting(c -> c.getSocio().getId()).containsExactly(ana.getId());
    }

    @Test
    @DisplayName("HU1-4 monto cero, negativo o con más de dos decimales")
    void montoInvalido() throws Exception {
        emitir("TODOS", "Tarifa", "0").andExpect(model().attributeHasFieldErrorCode("cuotaFormulario", "monto", "DecimalMin"));
        emitir("TODOS", "Tarifa", "-5").andExpect(content().string(containsString("El monto debe ser mayor que cero")));
        emitir("TODOS", "Tarifa", "1.255").andExpect(content().string(containsString("El monto admite hasta dos decimales")));
        assertThat(todas()).isEmpty();
    }

    @Test
    @DisplayName("HU1-5 un concepto no se emite dos veces al mismo socio")
    void conceptoRepetido() throws Exception {
        emitir("SOCIO", "Tarifa anual 2026", "10");
        mvc.perform(formulario("/cuotas/previa", "TODOS", "  tarifa ANUAL 2026 ", "10"))
                .andExpect(content().string(containsString("Se crearán 2 cuotas por un total de $20.00")))
                .andExpect(content().string(containsString("1 socios ya tienen esta cuota y se omitirán")));
        emitir("TODOS", "TARIFA anual 2026", "10");
        assertThat(todas()).hasSize(3);
        emitir("SOCIO", "Tarifa anual 2026", "10")
                .andExpect(content().string(containsString("Este socio ya tiene una cuota con ese concepto")));
        emitir("TODOS", "Tarifa anual 2026", "10")
                .andExpect(content().string(containsString("No hay socios a quienes emitir esta cuota")));
        assertThat(todas()).hasSize(3);
    }

    @Test
    @DisplayName("HU1-6 y HU1-7 anular sin pagos con motivo; con pagos no se anula")
    void anular() throws Exception {
        Cuota libre = fabrica.cuota(ana, "Medidor", "5.00", LocalDate.now().plusDays(5));
        Cuota pagada = fabrica.cuota(beto, "Medidor", "5.00", LocalDate.now().plusDays(5));
        pagada.aplicar(new BigDecimal("2.00"));
        cuotas.save(pagada);
        mvc.perform(post("/cuotas/" + libre.getId() + "/anular").with(fabrica.sesion(tesorero)).with(csrf())
                .param("motivo", " ")).andExpect(flash().attribute("error", "motivo.obligatorio"));
        mvc.perform(post("/cuotas/" + libre.getId() + "/anular").with(fabrica.sesion(tesorero)).with(csrf())
                .param("motivo", "Emitida por error")).andExpect(flash().attribute("exito", "cuota.anulada"));
        Cuota anulada = cuotas.findById(libre.getId()).orElseThrow();
        assertThat(anulada.getEstado()).isEqualTo(EstadoObligacion.ANULADA);
        assertThat(anulada.getMotivoAnulacion()).isEqualTo("Emitida por error");
        assertThat(cuotas.deudaDeSocio(ana.getId())).isEqualByComparingTo("0");
        mvc.perform(post("/cuotas/" + pagada.getId() + "/anular").with(fabrica.sesion(tesorero)).with(csrf())
                .param("motivo", "Error")).andExpect(flash().attribute("error", "cuota.conPagos"));
        assertThat(cuotas.findById(pagada.getId()).orElseThrow().getEstado()).isEqualTo(EstadoObligacion.PARCIAL);
        mvc.perform(get("/cuotas").with(fabrica.sesion(tesorero)))
                .andExpect(content().string(containsString("Emitida por error")))
                .andExpect(content().string(not(containsString("data-anular=\"" + pagada.getId() + "\""))))
                .andExpect(content().string(not(containsString("data-anular=\"" + libre.getId() + "\""))));
        Cuota otra = fabrica.cuota(carla, "Medidor", "5.00", LocalDate.now().plusDays(5));
        mvc.perform(get("/cuotas").with(fabrica.sesion(tesorero)))
                .andExpect(content().string(containsString("data-anular=\"" + otra.getId() + "\"")));
    }

    @Test
    @DisplayName("HU1-8 ADMIN_JUNTA solo lectura; otra junta responde 404")
    void permisosYAislamiento() throws Exception {
        Cuota cuota = fabrica.cuota(ana, "Medidor", "5.00", LocalDate.now().plusDays(5));
        Usuario admin = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        mvc.perform(get("/cuotas").with(fabrica.sesion(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Medidor")))
                .andExpect(content().string(not(containsString("Emitir Cuotas"))));
        mvc.perform(post("/cuotas").with(fabrica.sesion(admin)).with(csrf()).param("destino", "TODOS"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/cuotas/" + cuota.getId() + "/anular").with(fabrica.sesion(admin)).with(csrf())
                .param("motivo", "x")).andExpect(status().isForbidden());
        Junta otra = fabrica.junta("Junta Ajena Cuotas");
        Usuario tesoreroAjeno = fabrica.tesorero(otra);
        mvc.perform(post("/cuotas/" + cuota.getId() + "/anular").with(fabrica.sesion(tesoreroAjeno)).with(csrf())
                .param("motivo", "x")).andExpect(status().isNotFound());
        mvc.perform(post("/cuotas").with(fabrica.sesion(tesoreroAjeno)).with(csrf()).param("destino", "SOCIO")
                .param("socioId", ana.getId().toString()).param("concepto", "X").param("monto", "1")
                .param("fechaVencimiento", LocalDate.now().toString())).andExpect(status().isNotFound());
        mvc.perform(get("/cuotas").with(fabrica.sesion(tesoreroAjeno)))
                .andExpect(content().string(not(containsString("Medidor"))));
    }
}
