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

import ec.uta.multijunta.model.Convocatoria;
import ec.uta.multijunta.model.EstadoConvocatoria;
import ec.uta.multijunta.model.EstadoSocio;
import ec.uta.multijunta.model.Evento;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.TipoConvocatoria;
import ec.uta.multijunta.model.TipoEvento;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.ConvocatoriaRepository;
import ec.uta.multijunta.repository.NotificacionRepository;
import ec.uta.multijunta.service.ConvocatoriaService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CU15 Emitir convocatorias")
class ConvocatoriaTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private ConvocatoriaRepository convocatorias;

    @Autowired
    private NotificacionRepository notificaciones;

    @Autowired
    private ConvocatoriaService servicio;

    private Junta junta;
    private Usuario secretario;
    private Socio conCuenta;
    private Socio sinCuenta;
    private Socio inactivo;

    @BeforeEach
    void preparar() {
        junta = fabrica.junta("Junta Convocatorias");
        Sector sector = fabrica.sector(junta);
        secretario = fabrica.secretario(junta);
        conCuenta = fabrica.socio(junta, sector);
        sinCuenta = fabrica.socioSinCuenta(junta, sector, EstadoSocio.ACTIVO);
        inactivo = fabrica.socio(junta, sector);
        inactivo.setEstado(EstadoSocio.INACTIVO);
        fabrica.guardar(inactivo);
    }

    private MockHttpServletRequestBuilder formulario(Usuario usuario, String prioridad, LocalDate fecha,
            LocalDateTime publicacion) {
        MockHttpServletRequestBuilder peticion = post("/convocatorias").with(fabrica.sesion(usuario)).with(csrf())
                .param("titulo", "Asamblea extraordinaria de regantes").param("tipo", "ASAMBLEA")
                .param("fechaEvento", fecha.toString()).param("hora", "09:00").param("lugar", "Casa comunal")
                .param("contenido", "1. Turnos de estiaje\n2. Varios").param("prioridad", prioridad);
        if (publicacion != null) {
            peticion.param("fechaPublicacion", publicacion.withSecond(0).withNano(0).toString());
        }
        return peticion;
    }

    private ResultActions crear(String prioridad, LocalDate fecha, LocalDateTime publicacion) throws Exception {
        return mvc.perform(formulario(secretario, prioridad, fecha, publicacion));
    }

    private List<Convocatoria> deJunta() {
        return convocatorias.findByJuntaIdOrderByFechaPublicacionDescIdDesc(junta.getId());
    }

    private List<String> titulosDe(Usuario usuario) {
        return notificaciones.findByUsuarioIdOrderByLeidaAscFechaDesc(usuario.getId()).stream()
                .map(n -> n.getTitulo()).toList();
    }

    @Test
    @DisplayName("HU2-1 y HU2-2 Urgente se publica al momento y notifica solo a socios activos con cuenta")
    void publicarInmediatamente() throws Exception {
        mvc.perform(get("/convocatorias/nueva").with(fabrica.sesion(secretario)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Urgente")))
                .andExpect(content().string(containsString("Lugar")))
                .andExpect(content().string(containsString("Publicar Inmediatamente")));
        crear("URGENTE", LocalDate.now().plusDays(10), null).andExpect(flash().attribute("exito", "convocatoria.publicada"));
        Convocatoria convocatoria = deJunta().get(0);
        assertThat(convocatoria.getEstado()).isEqualTo(EstadoConvocatoria.PUBLICADA);
        assertThat(convocatoria.getFechaPublicacion()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(convocatoria.getAutor().getId()).isEqualTo(secretario.getId());
        assertThat(titulosDe(conCuenta)).containsExactly("Nueva convocatoria");
        assertThat(notificaciones.findByUsuarioIdOrderByLeidaAscFechaDesc(conCuenta.getId()).get(0).getEnlace())
                .isEqualTo("/anuncios");
        assertThat(titulosDe(sinCuenta)).isEmpty();
        assertThat(titulosDe(inactivo)).isEmpty();
        assertThat(titulosDe(secretario)).isEmpty();
    }

    @Test
    @DisplayName("HU2-3 Normal queda en borrador y el programador la publica al llegar su fecha")
    void programada() throws Exception {
        crear("NORMAL", LocalDate.now().plusDays(10), LocalDateTime.now().plusDays(1))
                .andExpect(flash().attribute("exito", "convocatoria.programada"));
        Convocatoria borrador = deJunta().get(0);
        assertThat(borrador.getEstado()).isEqualTo(EstadoConvocatoria.BORRADOR);
        servicio.publicarPendientes();
        assertThat(convocatorias.findById(borrador.getId()).orElseThrow().getEstado()).isEqualTo(EstadoConvocatoria.BORRADOR);
        assertThat(titulosDe(conCuenta)).isEmpty();
        borrador.setFechaPublicacion(LocalDateTime.now().minusMinutes(1));
        convocatorias.save(borrador);
        servicio.publicarPendientes();
        assertThat(convocatorias.findById(borrador.getId()).orElseThrow().getEstado()).isEqualTo(EstadoConvocatoria.PUBLICADA);
        assertThat(titulosDe(conCuenta)).containsExactly("Nueva convocatoria");
    }

    @Test
    @DisplayName("SC-003 el programador revisa al menos una vez por minuto")
    void frecuenciaDelProgramador() throws Exception {
        Scheduled programado = ConvocatoriaService.class.getMethod("publicarPendientes").getAnnotation(Scheduled.class);
        assertThat(programado).isNotNull();
        assertThat(programado.fixedDelay()).isPositive().isLessThanOrEqualTo(60000);
    }

    @Test
    @DisplayName("HU2-4 fechas inválidas")
    void fechasInvalidas() throws Exception {
        crear("URGENTE", LocalDate.now().minusDays(1), null)
                .andExpect(content().string(containsString("La fecha debe ser futura")));
        crear("NORMAL", LocalDate.now().plusDays(5), LocalDateTime.now().minusHours(1))
                .andExpect(content().string(containsString("La fecha de publicación debe ser futura y anterior al evento")));
        crear("NORMAL", LocalDate.now().plusDays(5), LocalDateTime.now().plusDays(6))
                .andExpect(content().string(containsString("La fecha de publicación debe ser futura y anterior al evento")));
        crear("NORMAL", LocalDate.now().plusDays(5), null)
                .andExpect(content().string(containsString("La fecha de publicación debe ser futura y anterior al evento")));
        assertThat(deJunta()).isEmpty();
    }

    @Test
    @DisplayName("Edge borrador con evento ya pasado queda 'Vencida sin publicar'")
    void vencidaSinPublicar() throws Exception {
        Convocatoria vencida = fabrica.convocatoria(junta, TipoConvocatoria.MINGA, LocalDate.now().minusDays(1),
                EstadoConvocatoria.BORRADOR);
        vencida.setFechaPublicacion(LocalDateTime.now().minusDays(3));
        convocatorias.save(vencida);
        servicio.publicarPendientes();
        assertThat(convocatorias.findById(vencida.getId()).orElseThrow().getEstado()).isEqualTo(EstadoConvocatoria.BORRADOR);
        assertThat(titulosDe(conCuenta)).isEmpty();
        mvc.perform(get("/convocatorias").with(fabrica.sesion(secretario)))
                .andExpect(content().string(containsString("Vencida sin publicar")));
    }

    @Test
    @DisplayName("HU2-5 y HU2-6 borradores editables y eliminables; publicadas no")
    void edicion() throws Exception {
        Convocatoria borrador = fabrica.convocatoria(junta, TipoConvocatoria.MINGA, LocalDate.now().plusDays(8),
                EstadoConvocatoria.BORRADOR);
        Convocatoria publicada = fabrica.convocatoria(junta, TipoConvocatoria.ASAMBLEA, LocalDate.now().plusDays(8),
                EstadoConvocatoria.PUBLICADA);
        mvc.perform(get("/convocatorias/" + borrador.getId() + "/editar").with(fabrica.sesion(secretario)))
                .andExpect(status().isOk());
        mvc.perform(post("/convocatorias/" + borrador.getId()).with(fabrica.sesion(secretario)).with(csrf())
                        .param("titulo", "Minga reprogramada").param("tipo", "MINGA")
                        .param("fechaEvento", LocalDate.now().plusDays(9).toString()).param("hora", "07:00")
                        .param("lugar", "Acequia").param("contenido", "Limpieza").param("prioridad", "NORMAL")
                        .param("fechaPublicacion", LocalDateTime.now().plusDays(2).withSecond(0).withNano(0).toString()))
                .andExpect(flash().attribute("exito", "convocatoria.actualizada"));
        assertThat(convocatorias.findById(borrador.getId()).orElseThrow().getTitulo()).isEqualTo("Minga reprogramada");
        mvc.perform(post("/convocatorias/" + publicada.getId() + "/eliminar").with(fabrica.sesion(secretario)).with(csrf()))
                .andExpect(flash().attribute("error", "convocatoria.noEditable"));
        mvc.perform(get("/convocatorias/" + publicada.getId() + "/editar").with(fabrica.sesion(secretario)))
                .andExpect(flash().attribute("error", "convocatoria.noEditable"));
        mvc.perform(post("/convocatorias/" + borrador.getId() + "/eliminar").with(fabrica.sesion(secretario)).with(csrf()))
                .andExpect(flash().attribute("exito", "convocatoria.eliminada"));
        assertThat(convocatorias.findById(borrador.getId())).isEmpty();
        assertThat(convocatorias.findById(publicada.getId())).isPresent();
    }

    @Test
    @DisplayName("HU2-7 al guardar una Asamblea o Minga se ofrece la convocatoria precargada")
    void ofertaDesdeEventos() throws Exception {
        LocalDate fecha = LocalDate.now().plusDays(12);
        mvc.perform(post("/eventos").with(fabrica.sesion(secretario)).with(csrf()).param("tipo", "ASAMBLEA")
                        .param("fecha", fecha.toString()).param("hora", "10:30").param("lugar", "Coliseo parroquial"))
                .andExpect(flash().attributeExists("ofrecerConvocatoria"));
        Evento evento = fabrica.evento(junta, TipoEvento.MINGA, fecha);
        mvc.perform(get("/convocatorias/nueva").param("eventoId", evento.getId().toString()).with(fabrica.sesion(secretario)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"" + fecha + "\"")))
                .andExpect(content().string(containsString("value=\"Casa comunal\"")))
                .andExpect(content().string(containsString("value=\"MINGA\" selected=\"selected\"")));
        Junta otra = fabrica.junta("Junta Ajena Eventos");
        Evento ajeno = fabrica.evento(otra, TipoEvento.ASAMBLEA, fecha);
        mvc.perform(get("/convocatorias/nueva").param("eventoId", ajeno.getId().toString()).with(fabrica.sesion(secretario)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/eventos").with(fabrica.sesion(secretario)).with(csrf()).param("tipo", "REUNION")
                        .param("fecha", fecha.toString()).param("hora", "18:00").param("lugar", "Sede"))
                .andExpect(flash().attributeCount(1));
    }

    @Test
    @DisplayName("HU2-8 ADMIN_JUNTA solo lectura; otra junta responde 404")
    void permisos() throws Exception {
        Convocatoria borrador = fabrica.convocatoria(junta, TipoConvocatoria.MINGA, LocalDate.now().plusDays(8),
                EstadoConvocatoria.BORRADOR);
        Usuario admin = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        mvc.perform(get("/convocatorias").with(fabrica.sesion(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(borrador.getTitulo())))
                .andExpect(content().string(not(containsString("Redactar Convocatoria"))));
        mvc.perform(formulario(admin, "URGENTE", LocalDate.now().plusDays(3), null)).andExpect(status().isForbidden());
        mvc.perform(post("/convocatorias/" + borrador.getId() + "/eliminar").with(fabrica.sesion(admin)).with(csrf()))
                .andExpect(status().isForbidden());
        Usuario ajeno = fabrica.secretario(fabrica.junta("Junta Ajena Convocatorias"));
        mvc.perform(post("/convocatorias/" + borrador.getId() + "/eliminar").with(fabrica.sesion(ajeno)).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/convocatorias").with(fabrica.sesion(ajeno)))
                .andExpect(content().string(not(containsString(borrador.getTitulo()))));
    }
}
