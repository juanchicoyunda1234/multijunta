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

import ec.uta.multijunta.model.Asistencia;
import ec.uta.multijunta.model.CorreccionAsistencia;
import ec.uta.multijunta.model.EstadoAsistencia;
import ec.uta.multijunta.model.EstadoObligacion;
import ec.uta.multijunta.model.Evento;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Multa;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.TipoEvento;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.CorreccionAsistenciaRepository;
import ec.uta.multijunta.repository.MultaRepository;
import ec.uta.multijunta.repository.NotificacionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
@DisplayName("CU10 Generar multas por inasistencia")
class MultaTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private MultaRepository multas;

    @Autowired
    private CorreccionAsistenciaRepository correcciones;

    @Autowired
    private NotificacionRepository notificaciones;

    private Junta junta;
    private Usuario tesorero;
    private Socio ausenteA;
    private Socio ausenteB;
    private Socio justificado;
    private Socio presente;
    private Evento minga;
    private Map<Socio, Asistencia> asistencias;

    @BeforeEach
    void preparar() {
        junta = fabrica.junta("Junta Multas");
        junta.setMontoMultaInasistencia(new BigDecimal("5.00"));
        fabrica.guardar(junta);
        Sector sector = fabrica.sector(junta);
        tesorero = fabrica.tesorero(junta);
        ausenteA = fabrica.socio(junta, sector);
        ausenteB = fabrica.socio(junta, sector);
        justificado = fabrica.socio(junta, sector);
        presente = fabrica.socio(junta, sector);
        minga = fabrica.evento(junta, TipoEvento.MINGA, LocalDate.now().minusDays(3));
        Map<Socio, EstadoAsistencia> estados = new LinkedHashMap<>();
        estados.put(ausenteA, EstadoAsistencia.AUSENTE);
        estados.put(ausenteB, EstadoAsistencia.AUSENTE);
        estados.put(justificado, EstadoAsistencia.JUSTIFICADO);
        estados.put(presente, EstadoAsistencia.PRESENTE);
        asistencias = fabrica.cerrarAsistencia(minga, estados);
    }

    private Long asistencia(Socio socio) {
        return asistencias.get(socio).getId();
    }

    private MockHttpServletRequestBuilder aprobar(Usuario usuario, Map<Socio, String> montos) {
        MockHttpServletRequestBuilder peticion = post("/multas/sincronizar").with(fabrica.sesion(usuario)).with(csrf());
        montos.forEach((socio, monto) -> peticion.param("seleccion", asistencia(socio).toString())
                .param("monto[" + asistencia(socio) + "]", monto));
        return peticion;
    }

    private ResultActions sincronizar(Usuario usuario) throws Exception {
        return mvc.perform(get("/multas/sincronizar").with(fabrica.sesion(usuario)));
    }

    private List<Multa> multasDeJunta() {
        return multas.findByJuntaIdOrderByFechaDescIdDesc(junta.getId());
    }

    @Test
    @DisplayName("HU3-1, HU3-5 y HU3-7 tabla de ausentes con monto de la junta, sin justificados y con actas pendientes")
    void tablaDeSincronizacion() throws Exception {
        fabrica.evento(junta, TipoEvento.ASAMBLEA, LocalDate.now().minusDays(1));
        Junta otra = fabrica.junta("Junta Ajena Multas");
        Socio ajeno = fabrica.socio(otra, fabrica.sector(otra));
        fabrica.cerrarAsistencia(fabrica.evento(otra, TipoEvento.MINGA, LocalDate.now().minusDays(2)),
                Map.of(ajeno, EstadoAsistencia.AUSENTE));
        sincronizar(tesorero)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(ausenteA.getNombreCompleto())))
                .andExpect(content().string(containsString(ausenteB.getNombreCompleto())))
                .andExpect(content().string(not(containsString(justificado.getNombreCompleto()))))
                .andExpect(content().string(not(containsString(presente.getNombreCompleto()))))
                .andExpect(content().string(not(containsString(ajeno.getNombreCompleto()))))
                .andExpect(content().string(containsString("2 inasistencias detectadas")))
                .andExpect(content().string(containsString("value=\"5.00\"")))
                .andExpect(content().string(containsString("Acta de evento no oficializada por Secretaría")))
                .andExpect(content().string(containsString("Aprobar y Facturar Multas")));
    }

    @Test
    @DisplayName("HU3-2, HU3-3 y HU3-4 monto editado, socio desmarcado y multas creadas en lote")
    void aprobarMultas() throws Exception {
        mvc.perform(aprobar(tesorero, Map.of(ausenteA, "6.00")))
                .andExpect(flash().attribute("exitoTexto", "Se generaron 1 multas por $6.00"));
        List<Multa> creadas = multasDeJunta();
        assertThat(creadas).hasSize(1);
        Multa multa = creadas.get(0);
        assertThat(multa.getSocio().getId()).isEqualTo(ausenteA.getId());
        assertThat(multa.getAsistenciaId()).isEqualTo(asistencia(ausenteA));
        assertThat(multa.getEstado()).isEqualTo(EstadoObligacion.PENDIENTE);
        assertThat(multa.getSaldoPendiente()).isEqualByComparingTo("6.00");
        assertThat(multa.getFecha()).isEqualTo(LocalDate.now());
        assertThat(multa.getMotivo()).isEqualTo("Inasistencia a Minga del "
                + minga.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        assertThat(multas.deudaDeSocio(ausenteA.getId())).isEqualByComparingTo("6.00");
        sincronizar(tesorero)
                .andExpect(content().string(containsString(ausenteB.getNombreCompleto())))
                .andExpect(content().string(not(containsString(ausenteA.getNombreCompleto()))));
    }

    @Test
    @DisplayName("HU3-2 montos inválidos y selección vacía")
    void montosInvalidos() throws Exception {
        mvc.perform(aprobar(tesorero, Map.of(ausenteA, "0")))
                .andExpect(content().string(containsString("El monto debe ser mayor que cero")));
        mvc.perform(aprobar(tesorero, Map.of(ausenteA, "5.555")))
                .andExpect(content().string(containsString("El monto admite hasta dos decimales")));
        mvc.perform(post("/multas/sincronizar").with(fabrica.sesion(tesorero)).with(csrf()))
                .andExpect(content().string(containsString("Seleccione al menos una inasistencia")));
        assertThat(multasDeJunta()).isEmpty();
    }

    @Test
    @DisplayName("HU3-6 y HU3-8 una asistencia no genera dos multas; tabla vacía")
    void sinDuplicados() throws Exception {
        mvc.perform(aprobar(tesorero, Map.of(ausenteA, "5.00", ausenteB, "5.00")));
        assertThat(multasDeJunta()).hasSize(2);
        mvc.perform(aprobar(tesorero, Map.of(ausenteA, "5.00")))
                .andExpect(content().string(containsString("Esta inasistencia ya tiene una multa")));
        assertThat(multasDeJunta()).hasSize(2);
        sincronizar(tesorero)
                .andExpect(content().string(containsString("No hay inasistencias pendientes de multar")))
                .andExpect(content().string(not(containsString("Aprobar y Facturar Multas"))));
    }

    @Test
    @DisplayName("Supuesto: junta sin monto configurado propone $0.00 y exige un monto mayor que cero")
    void juntaSinMonto() throws Exception {
        junta.setMontoMultaInasistencia(null);
        fabrica.guardar(junta);
        sincronizar(tesorero).andExpect(content().string(containsString("value=\"0.00\"")));
        mvc.perform(aprobar(tesorero, Map.of(ausenteA, "0.00")))
                .andExpect(content().string(containsString("El monto debe ser mayor que cero")));
    }

    @Test
    @DisplayName("HU3-9 SECRETARIO y ADMIN_JUNTA solo lectura")
    void permisos() throws Exception {
        for (Usuario lector : List.of(fabrica.secretario(junta), fabrica.usuario(Rol.ADMIN_JUNTA, junta))) {
            mvc.perform(get("/multas").with(fabrica.sesion(lector)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("href=\"/multas/sincronizar\""))));
            sincronizar(lector).andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("Aprobar y Facturar Multas"))));
            mvc.perform(aprobar(lector, Map.of(ausenteA, "5.00"))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/multas").with(fabrica.sesion(tesorero)))
                .andExpect(content().string(containsString("Sincronizar Multas de Asistencia")));
        assertThat(multasDeJunta()).isEmpty();
    }

    @Test
    @DisplayName("FR-015 corrección aprobada anula la multa sin pagos o avisa al tesorero si tiene pagos")
    void efectoDeCorrecciones() throws Exception {
        mvc.perform(aprobar(tesorero, Map.of(ausenteA, "5.00", ausenteB, "5.00")));
        Multa multaB = multas.findByAsistenciaIdAndJuntaIdAndEstadoNot(asistencia(ausenteB), junta.getId(),
                EstadoObligacion.ANULADA).orElseThrow();
        multaB.aplicar(new BigDecimal("2.00"));
        multas.save(multaB);
        Usuario secretario = fabrica.secretario(junta);
        Usuario admin = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        for (Socio socio : List.of(ausenteA, ausenteB)) {
            mvc.perform(post("/asistencia/correcciones").with(fabrica.sesion(secretario)).with(csrf())
                    .param("asistenciaId", asistencia(socio).toString()).param("estadoNuevo", "JUSTIFICADO")
                    .param("motivo", "Certificado médico").param("eventoId", minga.getId().toString()));
            CorreccionAsistencia correccion = correcciones.findByJuntaIdOrderByFechaSolicitudDesc(junta.getId()).get(0);
            mvc.perform(post("/asistencia/correcciones/" + correccion.getId() + "/aprobar")
                    .with(fabrica.sesion(admin)).with(csrf())).andExpect(flash().attribute("exito", "correccion.aprobada"));
        }
        Multa multaA = multas.findById(multas.findByJuntaIdOrderByFechaDescIdDesc(junta.getId()).stream()
                .filter(m -> m.getSocio().getId().equals(ausenteA.getId())).findFirst().orElseThrow().getId()).orElseThrow();
        assertThat(multaA.getEstado()).isEqualTo(EstadoObligacion.ANULADA);
        assertThat(multaA.getMotivoAnulacion()).isEqualTo("Asistencia corregida");
        assertThat(multaA.getSaldoPendiente()).isEqualByComparingTo("0");
        assertThat(multas.findById(multaB.getId()).orElseThrow().getEstado()).isEqualTo(EstadoObligacion.PARCIAL);
        assertThat(notificaciones.findByUsuarioIdOrderByLeidaAscFechaDesc(tesorero.getId()))
                .extracting(n -> n.getTitulo()).containsExactly("Multa pagada en asistencia corregida");
    }

    @Test
    @DisplayName("SC-002 aprobar 200 multas responde en menos de 3 segundos")
    void rendimientoDoscientas() throws Exception {
        Sector sector = fabrica.sector(junta);
        Map<Socio, EstadoAsistencia> estados = new LinkedHashMap<>();
        for (int indice = 0; indice < 200; indice++) {
            estados.put(fabrica.socio(junta, sector), EstadoAsistencia.AUSENTE);
        }
        Evento asamblea = fabrica.evento(junta, TipoEvento.ASAMBLEA, LocalDate.now().minusDays(1));
        Map<Socio, Asistencia> nuevas = fabrica.cerrarAsistencia(asamblea, estados);
        MockHttpServletRequestBuilder peticion = post("/multas/sincronizar").with(fabrica.sesion(tesorero)).with(csrf());
        nuevas.values().forEach(asistencia -> peticion.param("seleccion", asistencia.getId().toString())
                .param("monto[" + asistencia.getId() + "]", "5.00"));
        long inicio = System.nanoTime();
        mvc.perform(peticion).andExpect(flash().attribute("exitoTexto", "Se generaron 200 multas por $1,000.00"));
        long milisegundos = (System.nanoTime() - inicio) / 1_000_000;
        assertThat(milisegundos).isLessThan(3000);
        assertThat(multasDeJunta()).hasSize(200);
    }
}
