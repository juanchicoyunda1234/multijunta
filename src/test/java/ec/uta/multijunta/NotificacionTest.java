package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Notificacion;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.NotificacionRepository;
import ec.uta.multijunta.service.NotificacionService;
import java.time.LocalDateTime;
import java.util.List;
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
@DisplayName("CU29 Campana de notificaciones")
class NotificacionTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private NotificacionService servicio;

    @Autowired
    private NotificacionRepository notificaciones;

    private Junta junta;
    private Usuario admin;

    @BeforeEach
    void preparar() {
        junta = fabrica.junta("Junta Campana");
        admin = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
    }

    @Test
    @DisplayName("HU5-1 la campana muestra el número de no leídas y nada cuando no hay")
    void contador() throws Exception {
        mvc.perform(get("/panel").with(fabrica.sesion(admin)))
                .andExpect(content().string(not(containsString("contadorNotificaciones"))));
        servicio.crear(admin, "Aviso uno", "Mensaje uno", "/panel");
        servicio.crear(admin, "Aviso dos", "Mensaje dos", "/panel");
        mvc.perform(get("/panel").with(fabrica.sesion(admin)))
                .andExpect(content().string(containsString("id=\"contadorNotificaciones\"")))
                .andExpect(content().string(containsString(">2</span>")));
    }

    @Test
    @DisplayName("HU5-2 la lista muestra título, mensaje y fecha con las no leídas primero")
    void listaOrdenada() throws Exception {
        Notificacion leida = servicio.crear(admin, "Aviso leído", "Ya visto", "/panel");
        leida.setLeida(true);
        leida.setFecha(LocalDateTime.now().plusMinutes(5));
        notificaciones.save(leida);
        servicio.crear(admin, "Aviso nuevo", "Sin ver", "/panel");
        List<Notificacion> orden = notificaciones.findByUsuarioIdOrderByLeidaAscFechaDesc(admin.getId());
        assertThat(orden.get(0).getTitulo()).isEqualTo("Aviso nuevo");
        String html = mvc.perform(get("/notificaciones").with(fabrica.sesion(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sin ver")))
                .andExpect(content().string(containsString("Nueva")))
                .andReturn().getResponse().getContentAsString();
        assertThat(html.indexOf("Aviso nuevo")).isLessThan(html.indexOf("Aviso leído"));
    }

    @Test
    @DisplayName("HU5-3 abrir una notificación la marca como leída y lleva a su enlace")
    void abrir() throws Exception {
        Notificacion notificacion = servicio.crear(admin, "Aviso", "Mensaje", "/recuperacion/solicitudes");
        mvc.perform(get("/notificaciones/" + notificacion.getId()).with(fabrica.sesion(admin)))
                .andExpect(redirectedUrl("/recuperacion/solicitudes"));
        assertThat(notificaciones.findById(notificacion.getId()).orElseThrow().isLeida()).isTrue();
    }

    @Test
    @DisplayName("HU5-4 marcar todas como leídas deja el contador en cero")
    void marcarTodas() throws Exception {
        servicio.crear(admin, "Uno", "Mensaje", "/panel");
        servicio.crear(admin, "Dos", "Mensaje", "/panel");
        mvc.perform(post("/notificaciones/marcar-todas").with(fabrica.sesion(admin)).with(csrf()))
                .andExpect(redirectedUrl("/notificaciones"));
        assertThat(notificaciones.countByUsuarioIdAndLeidaFalse(admin.getId())).isZero();
    }

    @Test
    @DisplayName("HU5-5 una solicitud de recuperación notifica a los administradores correctos")
    void notificacionPorRecuperacion() throws Exception {
        Usuario segundoAdmin = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        Usuario adminOtra = fabrica.usuario(Rol.ADMIN_JUNTA, fabrica.junta("Junta Lejana"));
        Usuario superAdmin = fabrica.superAdmin();
        Usuario socio = fabrica.usuario(Rol.SOCIO, junta);
        mvc.perform(post("/recuperar").param("identificador", socio.getCedula()).with(csrf()));
        for (Usuario destinatario : List.of(admin, segundoAdmin)) {
            List<Notificacion> recibidas = notificaciones.findByUsuarioIdOrderByLeidaAscFechaDesc(destinatario.getId());
            assertThat(recibidas).hasSize(1);
            assertThat(recibidas.get(0).getTitulo()).isEqualTo("Nueva solicitud de recuperación de contraseña");
            assertThat(recibidas.get(0).getEnlace()).isEqualTo("/recuperacion/solicitudes");
        }
        assertThat(notificaciones.countByUsuarioIdAndLeidaFalse(adminOtra.getId())).isZero();
        assertThat(notificaciones.countByUsuarioIdAndLeidaFalse(superAdmin.getId())).isZero();

        mvc.perform(post("/recuperar").param("identificador", admin.getUsername()).with(csrf()));
        assertThat(notificaciones.countByUsuarioIdAndLeidaFalse(superAdmin.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("HU5-6 nadie ve ni abre las notificaciones de otro usuario")
    void soloPropias() throws Exception {
        Usuario otro = fabrica.usuario(Rol.SECRETARIO, junta);
        Notificacion ajena = servicio.crear(otro, "Privada", "Solo para otro", "/panel");
        mvc.perform(get("/notificaciones").with(fabrica.sesion(admin)))
                .andExpect(content().string(not(containsString("Solo para otro"))));
        mvc.perform(get("/notificaciones/" + ajena.getId()).with(fabrica.sesion(admin)))
                .andExpect(status().isNotFound());
        assertThat(notificaciones.findById(ajena.getId()).orElseThrow().isLeida()).isFalse();
    }
}
