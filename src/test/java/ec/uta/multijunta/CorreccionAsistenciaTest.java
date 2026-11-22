package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.uta.multijunta.model.Asistencia;
import ec.uta.multijunta.model.CorreccionAsistencia;
import ec.uta.multijunta.model.EstadoAsistencia;
import ec.uta.multijunta.model.EstadoCorreccion;
import ec.uta.multijunta.model.Evento;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.TipoEvento;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.AsistenciaRepository;
import ec.uta.multijunta.repository.CorreccionAsistenciaRepository;
import ec.uta.multijunta.repository.NotificacionRepository;
import ec.uta.multijunta.service.IAsistencia;
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
@DisplayName("CU28 Corregir asistencia cerrada")
class CorreccionAsistenciaTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private AsistenciaRepository asistencias;

    @Autowired
    private CorreccionAsistenciaRepository correcciones;

    @Autowired
    private NotificacionRepository notificaciones;

    @Autowired
    private IAsistencia iAsistencia;

    private Junta junta;
    private Usuario secretario;
    private Usuario admin;
    private Evento evento;
    private Asistencia ausencia;

    @BeforeEach
    void preparar() throws Exception {
        junta = fabrica.junta("Junta Correcciones");
        Sector sector = fabrica.sector(junta);
        Socio socio = fabrica.socio(junta, sector);
        secretario = fabrica.secretario(junta);
        admin = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        evento = fabrica.evento(junta, TipoEvento.ASAMBLEA, LocalDate.now().minusDays(2));
        mvc.perform(post("/asistencia/eventos/" + evento.getId() + "/cerrar").with(fabrica.sesion(secretario)).with(csrf()));
        ausencia = asistencias.findByEventoIdOrderBySocioApellidosAscSocioNombresAsc(evento.getId()).stream()
                .filter(a -> a.getSocio().getId().equals(socio.getId())).findFirst().orElseThrow();
    }

    private ResultActions solicitar(Usuario usuario, String estado, String motivo) throws Exception {
        return mvc.perform(post("/asistencia/correcciones").with(fabrica.sesion(usuario)).with(csrf())
                .param("asistenciaId", ausencia.getId().toString()).param("estadoNuevo", estado)
                .param("motivo", motivo).param("eventoId", evento.getId().toString()));
    }

    private CorreccionAsistencia pendiente() {
        return correcciones.findByJuntaIdOrderByFechaSolicitudDesc(junta.getId()).get(0);
    }

    @Test
    @DisplayName("HU7-1 y HU7-3 solicitar, notificar y aprobar con auditoría")
    void solicitarYAprobar() throws Exception {
        solicitar(secretario, "JUSTIFICADO", "Certificado médico")
                .andExpect(flash().attribute("exito", "correccion.solicitada"));
        assertThat(notificaciones.findByUsuarioIdOrderByLeidaAscFechaDesc(admin.getId()))
                .extracting(n -> n.getTitulo()).containsExactly("Corrección de asistencia por autorizar");
        CorreccionAsistencia correccion = pendiente();
        assertThat(correccion.getEstado()).isEqualTo(EstadoCorreccion.PENDIENTE);
        assertThat(correccion.getEstadoAnterior()).isEqualTo(EstadoAsistencia.AUSENTE);
        assertThat(iAsistencia.inasistenciasDelEvento(junta.getId(), evento.getId())).hasSize(1);

        mvc.perform(post("/asistencia/correcciones/" + correccion.getId() + "/aprobar").with(fabrica.sesion(admin))
                .with(csrf())).andExpect(flash().attribute("exito", "correccion.aprobada"));
        CorreccionAsistencia aprobada = correcciones.findById(correccion.getId()).orElseThrow();
        assertThat(aprobada.getEstado()).isEqualTo(EstadoCorreccion.APROBADA);
        assertThat(aprobada.getResolutor().getId()).isEqualTo(admin.getId());
        assertThat(aprobada.getSolicitante().getId()).isEqualTo(secretario.getId());
        assertThat(aprobada.getFechaResolucion()).isNotNull();
        assertThat(asistencias.findById(ausencia.getId()).orElseThrow().getEstado()).isEqualTo(EstadoAsistencia.JUSTIFICADO);
        assertThat(iAsistencia.inasistenciasDelEvento(junta.getId(), evento.getId())).isEmpty();
        assertThat(notificaciones.findByUsuarioIdOrderByLeidaAscFechaDesc(secretario.getId()))
                .extracting(n -> n.getTitulo()).contains("Corrección de asistencia aprobada");
        mvc.perform(get("/asistencia/eventos/" + evento.getId()).with(fabrica.sesion(secretario)))
                .andExpect(content().string(containsString("Certificado médico")))
                .andExpect(content().string(containsString("Aprobada")));
    }

    @Test
    @DisplayName("HU7-2 y HU7-6 motivo vacío, mismo estado y una sola pendiente")
    void validaciones() throws Exception {
        solicitar(secretario, "JUSTIFICADO", " ").andExpect(flash().attribute("error", "correccion.motivo"));
        solicitar(secretario, "AUSENTE", "Error").andExpect(flash().attribute("error", "correccion.mismoEstado"));
        solicitar(secretario, "JUSTIFICADO", "Motivo").andExpect(flash().attribute("exito", "correccion.solicitada"));
        solicitar(secretario, "PRESENTE", "Otro").andExpect(flash().attribute("error", "correccion.pendiente"));
        assertThat(correcciones.findByJuntaIdOrderByFechaSolicitudDesc(junta.getId())).hasSize(1);
        mvc.perform(get("/asistencia/eventos/" + evento.getId()).with(fabrica.sesion(secretario))
                        .flashAttr("error", "correccion.pendiente"))
                .andExpect(content().string(containsString("Ya existe una solicitud pendiente para este socio")));
    }

    @Test
    @DisplayName("HU7-4 y HU7-5 rechazar deja la asistencia igual y avisa al solicitante")
    void rechazar() throws Exception {
        solicitar(secretario, "PRESENTE", "Llegó tarde");
        CorreccionAsistencia correccion = pendiente();
        mvc.perform(post("/asistencia/correcciones/" + correccion.getId() + "/rechazar").with(fabrica.sesion(admin))
                .with(csrf())).andExpect(flash().attribute("exito", "correccion.rechazada"));
        assertThat(correcciones.findById(correccion.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoCorreccion.RECHAZADA);
        assertThat(asistencias.findById(ausencia.getId()).orElseThrow().getEstado()).isEqualTo(EstadoAsistencia.AUSENTE);
        assertThat(notificaciones.findByUsuarioIdOrderByLeidaAscFechaDesc(secretario.getId()))
                .extracting(n -> n.getTitulo()).contains("Corrección de asistencia rechazada");
        mvc.perform(post("/asistencia/correcciones/" + correccion.getId() + "/aprobar").with(fabrica.sesion(admin))
                .with(csrf())).andExpect(flash().attribute("error", "correccion.yaResuelta"));
    }

    @Test
    @DisplayName("HU7-7 y HU7-8 historial sin edición; roles separados con 403")
    void historialYRoles() throws Exception {
        solicitar(admin, "JUSTIFICADO", "Intento").andExpect(status().isForbidden());
        solicitar(secretario, "JUSTIFICADO", "Motivo válido");
        CorreccionAsistencia correccion = pendiente();
        mvc.perform(post("/asistencia/correcciones/" + correccion.getId() + "/aprobar").with(fabrica.sesion(secretario))
                .with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/asistencia/correcciones").with(fabrica.sesion(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Motivo válido")))
                .andExpect(content().string(containsString("Aprobar")));
        mvc.perform(get("/asistencia/correcciones").with(fabrica.sesion(secretario)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString(">Aprobar<"))));
        int borrar = mvc.perform(delete("/asistencia/correcciones/" + correccion.getId()).with(fabrica.sesion(admin))
                .with(csrf())).andReturn().getResponse().getStatus();
        assertThat(borrar).isIn(404, 405);
        mvc.perform(post("/asistencia/correcciones/" + correccion.getId() + "/editar").with(fabrica.sesion(admin))
                .with(csrf())).andExpect(status().isNotFound());
        assertThat(correcciones.findById(correccion.getId())).isPresent();
    }

    @Test
    @DisplayName("Aislamiento: asistencia o solicitud de otra junta responden 404")
    void otraJunta() throws Exception {
        solicitar(secretario, "JUSTIFICADO", "Motivo");
        CorreccionAsistencia correccion = pendiente();
        Junta otra = fabrica.junta("Junta Ajena Correcciones");
        Usuario adminAjeno = fabrica.usuario(Rol.ADMIN_JUNTA, otra);
        Usuario secretarioAjeno = fabrica.secretario(otra);
        mvc.perform(post("/asistencia/correcciones/" + correccion.getId() + "/aprobar").with(fabrica.sesion(adminAjeno))
                .with(csrf())).andExpect(status().isNotFound());
        solicitar(secretarioAjeno, "PRESENTE", "Ajeno").andExpect(status().isNotFound());
        assertThat(correcciones.findById(correccion.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoCorreccion.PENDIENTE);
    }
}
