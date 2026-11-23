package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import ec.uta.multijunta.model.Asistencia;
import ec.uta.multijunta.model.EstadoAsistencia;
import ec.uta.multijunta.model.EstadoSocio;
import ec.uta.multijunta.model.Evento;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Notificacion;
import ec.uta.multijunta.model.Recurso;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.TipoEvento;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.ConversacionRepository;
import ec.uta.multijunta.repository.NotificacionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CU29 Recibir notificaciones de todos los módulos")
class NotificacionOrigenesTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private NotificacionRepository notificaciones;

    @Autowired
    private ConversacionRepository conversaciones;

    private Junta junta;
    private Sector sector;
    private Socio conCuenta;
    private Socio sinCuenta;
    private Usuario secretario;
    private Usuario tesorero;
    private Usuario admin;
    private Socio ajeno;

    @BeforeEach
    void preparar() {
        junta = fabrica.junta("Junta Orígenes");
        junta.setMontoMultaInasistencia(new BigDecimal("5.00"));
        fabrica.guardar(junta);
        sector = fabrica.sector(junta);
        conCuenta = fabrica.socio(junta, sector);
        sinCuenta = fabrica.socioSinCuenta(junta, sector, EstadoSocio.ACTIVO);
        secretario = fabrica.secretario(junta);
        tesorero = fabrica.tesorero(junta);
        admin = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        Junta otra = fabrica.junta("Junta Otra Orígenes");
        ajeno = fabrica.socio(otra, fabrica.sector(otra));
    }

    private List<Notificacion> de(Usuario usuario) {
        return notificaciones.findByUsuarioIdOrderByLeidaAscFechaDesc(usuario.getId());
    }

    @Test
    @DisplayName("HU7-1 convocatoria publicada notifica a socios con cuenta de la junta")
    void convocatoria() throws Exception {
        mvc.perform(post("/convocatorias").with(fabrica.sesion(secretario)).with(csrf()).param("titulo", "Asamblea general")
                .param("tipo", "ASAMBLEA").param("fechaEvento", LocalDate.now().plusDays(6).toString()).param("hora", "09:00")
                .param("lugar", "Casa comunal").param("contenido", "Orden del día").param("prioridad", "URGENTE"));
        assertThat(de(conCuenta)).extracting(n -> n.getTitulo() + " " + n.getEnlace()).containsExactly("Nueva convocatoria /anuncios");
        assertThat(de(conCuenta).get(0).getMensaje()).contains("Asamblea general");
        assertThat(de(sinCuenta)).isEmpty();
        assertThat(de(ajeno)).isEmpty();
    }

    @Test
    @DisplayName("HU7-2 mensaje nuevo notifica a sus destinatarios con enlace a la conversación")
    void mensaje() throws Exception {
        mvc.perform(get("/mensajes").with(fabrica.sesion(conCuenta)));
        Long conversacionId = conversaciones.findBySocioId(conCuenta.getId()).orElseThrow().getId();
        mvc.perform(multipart("/mensajes/" + conversacionId).param("contenido", "Consulta")
                .with(fabrica.sesion(conCuenta)).with(csrf()));
        for (Usuario miembro : List.of(admin, secretario, tesorero)) {
            assertThat(de(miembro)).extracting(n -> n.getTitulo() + " " + n.getEnlace())
                    .containsExactly("Nuevo mensaje /mensajes/" + conversacionId);
        }
        assertThat(de(conCuenta)).isEmpty();
    }

    @Test
    @DisplayName("HU7-3 cuota emitida y multa aprobada notifican al socio con cuenta")
    void cuotaYMulta() throws Exception {
        mvc.perform(post("/cuotas").with(fabrica.sesion(tesorero)).with(csrf()).param("destino", "TODOS")
                .param("concepto", "Tarifa anual 2026").param("monto", "12.50")
                .param("fechaVencimiento", LocalDate.now().plusDays(15).toString()));
        assertThat(de(conCuenta)).extracting(n -> n.getTitulo() + " " + n.getEnlace())
                .containsExactly("Nueva cuota /mi-informacion/estado-cuenta");
        assertThat(de(conCuenta).get(0).getMensaje()).contains("Tarifa anual 2026").contains("$12.50");
        assertThat(de(sinCuenta)).isEmpty();
        Evento minga = fabrica.evento(junta, TipoEvento.MINGA, LocalDate.now().minusDays(2));
        Map<Socio, Asistencia> asistencias = fabrica.cerrarAsistencia(minga, Map.of(conCuenta, EstadoAsistencia.AUSENTE,
                sinCuenta, EstadoAsistencia.AUSENTE));
        mvc.perform(post("/multas/sincronizar").with(fabrica.sesion(tesorero)).with(csrf())
                .param("seleccion", asistencias.get(conCuenta).getId().toString(), asistencias.get(sinCuenta).getId().toString())
                .param("monto[" + asistencias.get(conCuenta).getId() + "]", "5.00")
                .param("monto[" + asistencias.get(sinCuenta).getId() + "]", "5.00"));
        assertThat(de(conCuenta)).extracting(n -> n.getTitulo()).contains("Nueva multa");
        assertThat(de(conCuenta)).filteredOn(n -> n.getTitulo().equals("Nueva multa"))
                .allSatisfy(n -> assertThat(n.getEnlace()).isEqualTo("/mi-informacion/estado-cuenta"));
        assertThat(de(sinCuenta)).isEmpty();
    }

    @Test
    @DisplayName("HU7-4 turno creado notifica al socio con enlace a su calendario")
    void turno() throws Exception {
        Recurso recurso = fabrica.recurso(junta, sector, "Compuerta Orígenes");
        mvc.perform(post("/turnos").with(fabrica.sesion(secretario)).with(csrf())
                .param("fecha", Fabrica.proximoDiaLaboral(LocalDate.now().plusDays(3)).toString())
                .param("horaInicio", "08:00").param("horaFin", "10:00").param("sectorId", sector.getId().toString())
                .param("recursoId", recurso.getId().toString()).param("responsable", "Juez de aguas"));
        assertThat(de(conCuenta)).extracting(n -> n.getEnlace()).containsExactly("/mi-informacion/calendario");
    }

    @Test
    @DisplayName("HU7-5 el contador de la campana en /contadores cuenta solo las no leídas")
    void contador() throws Exception {
        mvc.perform(post("/cuotas").with(fabrica.sesion(tesorero)).with(csrf()).param("destino", "SOCIO")
                .param("socioId", conCuenta.getId().toString()).param("concepto", "Medidor").param("monto", "3")
                .param("fechaVencimiento", LocalDate.now().plusDays(5).toString()));
        mvc.perform(get("/contadores").with(fabrica.sesion(conCuenta))).andExpect(jsonPath("$.notificaciones").value(1));
        mvc.perform(post("/notificaciones/marcar-todas").with(fabrica.sesion(conCuenta)).with(csrf()));
        mvc.perform(get("/contadores").with(fabrica.sesion(conCuenta))).andExpect(jsonPath("$.notificaciones").value(0));
    }

    @Test
    @DisplayName("T063 un socio INACTIVO con cuenta no recibe ninguna notificación")
    void socioInactivoSinNotificaciones() throws Exception {
        Socio inactivo = fabrica.socio(junta, sector);
        Evento minga = fabrica.evento(junta, TipoEvento.MINGA, LocalDate.now().minusDays(2));
        Map<Socio, Asistencia> asistencias = fabrica.cerrarAsistencia(minga, Map.of(inactivo, EstadoAsistencia.AUSENTE));
        mvc.perform(get("/mensajes").with(fabrica.sesion(inactivo)));
        Long conversacionId = conversaciones.findBySocioId(inactivo.getId()).orElseThrow().getId();
        inactivo.setEstado(EstadoSocio.INACTIVO);
        fabrica.guardar(inactivo);
        mvc.perform(post("/cuotas").with(fabrica.sesion(tesorero)).with(csrf()).param("destino", "SOCIO")
                .param("socioId", inactivo.getId().toString()).param("concepto", "Medidor").param("monto", "3")
                .param("fechaVencimiento", LocalDate.now().plusDays(5).toString()));
        mvc.perform(post("/multas/sincronizar").with(fabrica.sesion(tesorero)).with(csrf())
                .param("seleccion", asistencias.get(inactivo).getId().toString())
                .param("monto[" + asistencias.get(inactivo).getId() + "]", "5.00"));
        mvc.perform(multipart("/mensajes/" + conversacionId).param("contenido", "Aviso de la directiva")
                .with(fabrica.sesion(admin)).with(csrf()));
        Recurso recurso = fabrica.recurso(junta, sector, "Compuerta Inactivo");
        mvc.perform(post("/turnos").with(fabrica.sesion(secretario)).with(csrf())
                .param("fecha", Fabrica.proximoDiaLaboral(LocalDate.now().plusDays(3)).toString())
                .param("horaInicio", "08:00").param("horaFin", "10:00").param("sectorId", sector.getId().toString())
                .param("recursoId", recurso.getId().toString()).param("responsable", "Juez de aguas"));
        mvc.perform(post("/convocatorias").with(fabrica.sesion(secretario)).with(csrf()).param("titulo", "Asamblea")
                .param("tipo", "ASAMBLEA").param("fechaEvento", LocalDate.now().plusDays(6).toString()).param("hora", "09:00")
                .param("lugar", "Casa comunal").param("contenido", "Orden").param("prioridad", "URGENTE"));
        assertThat(de(inactivo)).isEmpty();
        assertThat(de(conCuenta)).isNotEmpty();
    }
}
