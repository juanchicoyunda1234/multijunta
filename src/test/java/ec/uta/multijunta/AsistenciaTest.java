package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.uta.multijunta.model.Asistencia;
import ec.uta.multijunta.model.EstadoAsistencia;
import ec.uta.multijunta.model.EstadoEvento;
import ec.uta.multijunta.model.EstadoSocio;
import ec.uta.multijunta.model.Evento;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.TipoEvento;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.AsistenciaRepository;
import ec.uta.multijunta.repository.EventoRepository;
import ec.uta.multijunta.service.IAsistencia;
import ec.uta.multijunta.service.Inasistencia;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CU06 Registrar asistencia a eventos e IAsistencia")
class AsistenciaTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private AsistenciaRepository asistencias;

    @Autowired
    private EventoRepository eventos;

    @Autowired
    private IAsistencia iAsistencia;

    private Junta junta;
    private Sector sector;
    private Usuario secretario;
    private Socio presente;
    private Socio ausente;
    private Socio justificado;
    private Socio inactivo;
    private Evento asamblea;

    @BeforeEach
    void preparar() {
        junta = fabrica.junta("Junta Asistencia");
        sector = fabrica.sector(junta);
        secretario = fabrica.secretario(junta);
        presente = fabrica.socio(junta, sector);
        ausente = fabrica.socio(junta, sector);
        justificado = fabrica.socioSinCuenta(junta, sector, EstadoSocio.ACTIVO);
        inactivo = fabrica.socioSinCuenta(junta, sector, EstadoSocio.INACTIVO);
        asamblea = fabrica.evento(junta, TipoEvento.ASAMBLEA, LocalDate.now().minusDays(1));
    }

    private MockHttpServletRequestBuilder cierre(Evento evento) {
        return post("/asistencia/eventos/" + evento.getId() + "/cerrar").with(fabrica.sesion(secretario)).with(csrf())
                .param("estado[" + presente.getId() + "]", "PRESENTE")
                .param("estado[" + justificado.getId() + "]", "JUSTIFICADO");
    }

    @Test
    @DisplayName("HU3-1 la toma lista socios ACTIVOS en Ausente con buscador")
    void planilla() throws Exception {
        mvc.perform(get("/asistencia/eventos/" + asamblea.getId()).with(fabrica.sesion(secretario)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(presente.getNombreCompleto())))
                .andExpect(content().string(not(containsString(inactivo.getNombreCompleto()))))
                .andExpect(content().string(containsString("Cerrar Registro de Asistencia")))
                .andExpect(content().string(containsString("Buscar por cédula o nombre")))
                .andExpect(content().string(org.hamcrest.Matchers.matchesPattern("(?s).*value=\"AUSENTE\"[^>]*checked.*")));
    }

    @Test
    @DisplayName("HU3-2 eventos futuros, cancelados o Reuniones no admiten toma")
    void eventosQueNoAdmiten() throws Exception {
        Evento futuro = fabrica.evento(junta, TipoEvento.MINGA, LocalDate.now().plusDays(1));
        Evento reunion = fabrica.evento(junta, TipoEvento.REUNION, LocalDate.now());
        Evento cancelado = fabrica.evento(junta, TipoEvento.MINGA, LocalDate.now());
        cancelado.setEstado(EstadoEvento.CANCELADO);
        eventos.save(cancelado);
        for (Evento evento : List.of(futuro, reunion, cancelado)) {
            mvc.perform(get("/asistencia/eventos/" + evento.getId()).with(fabrica.sesion(secretario)))
                    .andExpect(redirectedUrl("/asistencia"))
                    .andExpect(flash().attribute("error", "asistencia.noAdmite"));
            mvc.perform(post("/asistencia/eventos/" + evento.getId() + "/cerrar").with(fabrica.sesion(secretario))
                    .with(csrf())).andExpect(flash().attribute("error", "asistencia.noAdmite"));
        }
        mvc.perform(get("/asistencia").with(fabrica.sesion(secretario)).flashAttr("error", "asistencia.noAdmite"))
                .andExpect(content().string(containsString("Este evento no admite toma de asistencia")));
    }

    @Test
    @DisplayName("HU3-3 y HU3-4 cierre con resumen y bloqueo")
    void cierre() throws Exception {
        mvc.perform(cierre(asamblea))
                .andExpect(redirectedUrl("/asistencia/eventos/" + asamblea.getId()))
                .andExpect(flash().attribute("exito", "asistencia.cerrada"));
        assertThat(eventos.findById(asamblea.getId()).orElseThrow().isAsistenciaCerrada()).isTrue();
        List<Asistencia> registro = asistencias.findByEventoIdOrderBySocioApellidosAscSocioNombresAsc(asamblea.getId());
        assertThat(registro).hasSize(3);
        assertThat(registro).filteredOn(a -> a.getSocio().getId().equals(ausente.getId()))
                .extracting(Asistencia::getEstado).containsExactly(EstadoAsistencia.AUSENTE);
        mvc.perform(get("/asistencia/eventos/" + asamblea.getId()).with(fabrica.sesion(secretario)))
                .andExpect(content().string(containsString("33 %")))
                .andExpect(content().string(containsString("Solicitar corrección")));
        mvc.perform(cierre(asamblea)).andExpect(flash().attribute("error", "asistencia.yaCerrada"));
        assertThat(asistencias.findByEventoIdOrderBySocioApellidosAscSocioNombresAsc(asamblea.getId())).hasSize(3);
    }

    @Test
    @DisplayName("Borde: junta sin socios activos no puede cerrar")
    void sinSociosActivos() throws Exception {
        Junta vacia = fabrica.junta("Junta Vacía");
        Usuario otroSecretario = fabrica.secretario(vacia);
        Evento evento = fabrica.evento(vacia, TipoEvento.MINGA, LocalDate.now());
        mvc.perform(post("/asistencia/eventos/" + evento.getId() + "/cerrar").with(fabrica.sesion(otroSecretario))
                .with(csrf())).andExpect(flash().attribute("error", "asistencia.sinSocios"));
        assertThat(eventos.findById(evento.getId()).orElseThrow().isAsistenciaCerrada()).isFalse();
    }

    @Test
    @DisplayName("HU3-7 ADMIN_JUNTA y TESORERO ven pero no toman ni cierran")
    void soloLectura() throws Exception {
        mvc.perform(cierre(asamblea));
        for (Rol rol : new Rol[] {Rol.ADMIN_JUNTA, Rol.TESORERO}) {
            Usuario lector = fabrica.usuario(rol, junta);
            mvc.perform(get("/asistencia").with(fabrica.sesion(lector))).andExpect(status().isOk())
                    .andExpect(content().string(containsString("Ver registro")));
            mvc.perform(get("/asistencia/eventos/" + asamblea.getId()).with(fabrica.sesion(lector)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("Solicitar corrección"))));
            Evento abierto = fabrica.evento(junta, TipoEvento.MINGA, LocalDate.now());
            mvc.perform(post("/asistencia/eventos/" + abierto.getId() + "/cerrar").with(fabrica.sesion(lector))
                    .with(csrf())).andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("IAsistencia: solo ausentes de eventos cerrados de la junta pedida")
    void interfazIAsistencia() throws Exception {
        Evento abierto = fabrica.evento(junta, TipoEvento.MINGA, LocalDate.now());
        mvc.perform(cierre(asamblea));
        Junta otra = fabrica.junta("Junta Otra IAsistencia");
        Sector sectorOtra = fabrica.sector(otra);
        fabrica.socio(otra, sectorOtra);
        Usuario secretarioOtra = fabrica.secretario(otra);
        Evento eventoOtra = fabrica.evento(otra, TipoEvento.ASAMBLEA, LocalDate.now());
        mvc.perform(post("/asistencia/eventos/" + eventoOtra.getId() + "/cerrar").with(fabrica.sesion(secretarioOtra))
                .with(csrf()));

        List<Inasistencia> deJunta = iAsistencia.inasistenciasDeEventosCerrados(junta.getId());
        assertThat(deJunta).extracting(Inasistencia::socioId).containsExactly(ausente.getId());
        assertThat(deJunta.get(0).eventoId()).isEqualTo(asamblea.getId());
        assertThat(deJunta.get(0).tipoEvento()).isEqualTo(TipoEvento.ASAMBLEA);
        assertThat(iAsistencia.inasistenciasDelEvento(junta.getId(), asamblea.getId())).hasSize(1);
        assertThat(iAsistencia.inasistenciasDelEvento(junta.getId(), abierto.getId())).isEmpty();
        assertThat(iAsistencia.inasistenciasDelEvento(junta.getId(), eventoOtra.getId())).isEmpty();
        assertThat(iAsistencia.inasistenciasDeEventosCerrados(otra.getId())).hasSize(1)
                .allMatch(inasistencia -> !inasistencia.socioId().equals(ausente.getId()));
    }

    @Test
    @DisplayName("SC-002 cerrar la asistencia de 200 socios en menos de 3 segundos")
    void doscientosSocios() throws Exception {
        Junta grande = fabrica.junta("Junta Grande");
        Sector sectorGrande = fabrica.sector(grande);
        Usuario secretarioGrande = fabrica.secretario(grande);
        List<Socio> todos = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            todos.add(fabrica.socioSinCuenta(grande, sectorGrande, EstadoSocio.ACTIVO));
        }
        Evento evento = fabrica.evento(grande, TipoEvento.ASAMBLEA, LocalDate.now());
        MockHttpServletRequestBuilder solicitud = post("/asistencia/eventos/" + evento.getId() + "/cerrar")
                .with(fabrica.sesion(secretarioGrande)).with(csrf());
        todos.forEach(socio -> solicitud.param("estado[" + socio.getId() + "]", "PRESENTE"));
        long inicio = System.nanoTime();
        mvc.perform(solicitud).andExpect(flash().attribute("exito", "asistencia.cerrada"));
        assertThat((System.nanoTime() - inicio) / 1_000_000).isLessThan(3000);
        assertThat(asistencias.findByEventoIdOrderBySocioApellidosAscSocioNombresAsc(evento.getId())).hasSize(200);
    }

    @Test
    @DisplayName("Aislamiento: un evento de otra junta responde 404")
    void otraJunta() throws Exception {
        Junta otra = fabrica.junta("Junta Lejana Asistencia");
        Evento ajeno = fabrica.evento(otra, TipoEvento.MINGA, LocalDate.now());
        mvc.perform(get("/asistencia/eventos/" + ajeno.getId()).with(fabrica.sesion(secretario)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/asistencia/eventos/" + ajeno.getId() + "/cerrar").with(fabrica.sesion(secretario))
                .with(csrf())).andExpect(status().isNotFound());
        assertThat(eventos.findById(ajeno.getId()).orElseThrow().isAsistenciaCerrada()).isFalse();
    }
}
