package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.uta.multijunta.model.EstadoSolicitud;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.SolicitudRecuperacion;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.SolicitudRecuperacionRepository;
import ec.uta.multijunta.repository.UsuarioRepository;
import ec.uta.multijunta.service.RecuperacionService;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CU21 Recuperar contraseña")
class RecuperacionTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private SolicitudRecuperacionRepository solicitudes;

    @Autowired
    private UsuarioRepository usuarios;

    private Junta junta;
    private Usuario admin;
    private Usuario socio;

    @BeforeEach
    void preparar() {
        junta = fabrica.junta("Junta Recuperación");
        admin = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        socio = fabrica.usuario(Rol.SOCIO, junta);
    }

    private void solicitar(String identificador) throws Exception {
        mvc.perform(post("/recuperar").param("identificador", identificador).with(csrf()))
                .andExpect(redirectedUrl("/recuperar?enviada"));
    }

    private List<SolicitudRecuperacion> pendientesDe(Usuario usuario) {
        return solicitudes.findAll().stream()
                .filter(s -> s.getUsuario().getId().equals(usuario.getId()) && s.getEstado() == EstadoSolicitud.PENDIENTE)
                .toList();
    }

    private Long idPendiente(Usuario usuario) {
        return pendientesDe(usuario).get(0).getId();
    }

    @Test
    @DisplayName("HU4-1 la solicitud por cédula o por usuario se registra y muestra el mensaje")
    void solicitudRegistrada() throws Exception {
        mvc.perform(get("/recuperar")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Cédula o usuario")));
        solicitar(socio.getCedula());
        assertThat(pendientesDe(socio)).hasSize(1);
        assertThat(pendientesDe(socio).get(0).getJunta().getId()).isEqualTo(junta.getId());
        Usuario secretario = fabrica.usuario(Rol.SECRETARIO, junta);
        solicitar("  " + secretario.getUsername().toUpperCase());
        assertThat(pendientesDe(secretario)).hasSize(1);
        mvc.perform(get("/recuperar").param("enviada", ""))
                .andExpect(content().string(containsString("Tu solicitud fue enviada a la directiva")));
    }

    @Test
    @DisplayName("HU4-2 el administrador ve las solicitudes de su junta y no las de otras")
    void administradorVeSoloSuJunta() throws Exception {
        solicitar(socio.getCedula());
        Usuario adminOtra = fabrica.usuario(Rol.ADMIN_JUNTA, fabrica.junta("Otra Junta"));
        mvc.perform(get("/recuperacion/solicitudes").with(fabrica.sesion(admin)))
                .andExpect(content().string(containsString(socio.getNombreCompleto())))
                .andExpect(content().string(containsString(socio.getCedula())))
                .andExpect(content().string(containsString("Socio")));
        mvc.perform(get("/recuperacion/solicitudes").with(fabrica.sesion(adminOtra)))
                .andExpect(content().string(not(containsString(socio.getNombreCompleto()))));
    }

    @Test
    @DisplayName("HU4-3 restablecer genera temporal, marca la cuenta, la desbloquea y atiende la solicitud")
    void restablecer() throws Exception {
        socio.setBloqueadoHasta(LocalDateTime.now().plusMinutes(10));
        socio.setIntentosFallidos(2);
        fabrica.guardar(socio);
        solicitar(socio.getCedula());
        Long id = idPendiente(socio);
        MvcResult resultado = mvc.perform(post("/recuperacion/solicitudes/" + id + "/restablecer")
                        .with(fabrica.sesion(admin)).with(csrf()))
                .andExpect(redirectedUrl("/recuperacion/solicitudes"))
                .andExpect(flash().attributeExists("restablecida"))
                .andReturn();
        RecuperacionService.Resultado datos = (RecuperacionService.Resultado) resultado.getFlashMap().get("restablecida");
        assertThat(datos.temporal()).hasSize(10).matches(".*[A-Za-z].*").matches(".*\\d.*");

        Usuario actualizado = usuarios.findById(socio.getId()).orElseThrow();
        assertThat(actualizado.isDebeCambiarPassword()).isTrue();
        assertThat(actualizado.getBloqueadoHasta()).isNull();
        assertThat(actualizado.getIntentosFallidos()).isZero();
        SolicitudRecuperacion solicitud = solicitudes.findById(id).orElseThrow();
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.ATENDIDA);
        assertThat(solicitud.getFechaAtencion()).isNotNull();
        assertThat(solicitud.getAtendidaPor().getId()).isEqualTo(admin.getId());

        mvc.perform(formLogin("/login").user(socio.getUsername()).password(datos.temporal()))
                .andExpect(redirectedUrl("/inicio"));
        mvc.perform(get("/mi-informacion").with(fabrica.sesion(actualizado)))
                .andExpect(redirectedUrl("/cuenta/cambiar-password"));
    }

    @Test
    @DisplayName("HU4-4 las solicitudes de un administrador las atiende el Super Administrador")
    void solicitudDeAdministrador() throws Exception {
        Usuario superAdmin = fabrica.superAdmin();
        solicitar(admin.getUsername());
        Long id = idPendiente(admin);
        Usuario otroAdmin = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        mvc.perform(get("/recuperacion/solicitudes").with(fabrica.sesion(otroAdmin)))
                .andExpect(content().string(not(containsString(admin.getNombreCompleto()))));
        mvc.perform(post("/recuperacion/solicitudes/" + id + "/restablecer").with(fabrica.sesion(otroAdmin)).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/recuperacion/solicitudes").with(fabrica.sesion(superAdmin)))
                .andExpect(content().string(containsString(admin.getNombreCompleto())));
        mvc.perform(post("/recuperacion/solicitudes/" + id + "/restablecer").with(fabrica.sesion(superAdmin)).with(csrf()))
                .andExpect(flash().attributeExists("restablecida"));
    }

    @Test
    @DisplayName("HU4-5 un identificador inexistente muestra el mismo mensaje y no crea solicitud")
    void identificadorInexistente() throws Exception {
        long antes = solicitudes.count();
        solicitar("9999999999");
        solicitar("nadie.existe");
        assertThat(solicitudes.count()).isEqualTo(antes);
    }

    @Test
    @DisplayName("HU4-6 una solicitud atendida no se atiende de nuevo")
    void solicitudAtendida() throws Exception {
        solicitar(socio.getCedula());
        Long id = idPendiente(socio);
        mvc.perform(post("/recuperacion/solicitudes/" + id + "/restablecer").with(fabrica.sesion(admin)).with(csrf()));
        mvc.perform(post("/recuperacion/solicitudes/" + id + "/restablecer").with(fabrica.sesion(admin)).with(csrf()))
                .andExpect(flash().attribute("error", "recuperar.atendida"))
                .andExpect(flash().attributeCount(1));
        mvc.perform(get("/recuperacion/solicitudes").with(fabrica.sesion(admin)).flashAttr("error", "recuperar.atendida"))
                .andExpect(content().string(containsString("La solicitud ya fue atendida")));
    }

    @Test
    @DisplayName("HU4-7 no se duplica una solicitud pendiente")
    void sinDuplicados() throws Exception {
        solicitar(socio.getCedula());
        solicitar(socio.getUsername());
        assertThat(pendientesDe(socio)).hasSize(1);
    }

    @Test
    @DisplayName("HU4-8 la contraseña temporal no se vuelve a mostrar al recargar")
    void temporalSeMuestraUnaVez() throws Exception {
        solicitar(socio.getCedula());
        Long id = idPendiente(socio);
        MvcResult resultado = mvc.perform(post("/recuperacion/solicitudes/" + id + "/restablecer")
                .with(fabrica.sesion(admin)).with(csrf())).andReturn();
        RecuperacionService.Resultado datos = (RecuperacionService.Resultado) resultado.getFlashMap().get("restablecida");
        mvc.perform(get("/recuperacion/solicitudes").with(fabrica.sesion(admin)).flashAttr("restablecida", datos))
                .andExpect(content().string(containsString(datos.temporal())));
        mvc.perform(get("/recuperacion/solicitudes").with(fabrica.sesion(admin)))
                .andExpect(content().string(not(containsString(datos.temporal()))));
    }

    @Test
    @DisplayName("Borde: el Super Administrador y las cuentas desactivadas no generan solicitud")
    void superAdminYDesactivadas() throws Exception {
        Usuario superAdmin = fabrica.superAdmin();
        Usuario desactivado = fabrica.usuario(Rol.TESORERO, junta);
        desactivado.setActivo(false);
        fabrica.guardar(desactivado);
        long antes = solicitudes.count();
        solicitar(superAdmin.getUsername());
        solicitar(desactivado.getCedula());
        assertThat(solicitudes.count()).isEqualTo(antes);
    }

    @Test
    @DisplayName("Borde: una junta sin administrador activo conserva la solicitud pendiente")
    void juntaSinAdministrador() throws Exception {
        Junta sinAdmin = fabrica.junta("Junta Sin Administrador");
        Usuario socioSinAdmin = fabrica.usuario(Rol.SOCIO, sinAdmin);
        solicitar(socioSinAdmin.getCedula());
        assertThat(pendientesDe(socioSinAdmin)).hasSize(1);
        Usuario nuevoAdmin = fabrica.usuario(Rol.ADMIN_JUNTA, sinAdmin);
        mvc.perform(get("/recuperacion/solicitudes").with(fabrica.sesion(nuevoAdmin)))
                .andExpect(content().string(containsString(socioSinAdmin.getNombreCompleto())));
    }

    @Test
    @DisplayName("Aislamiento: un administrador de otra junta recibe 404 al restablecer")
    void restablecerDeOtraJunta() throws Exception {
        solicitar(socio.getCedula());
        Long id = idPendiente(socio);
        Usuario adminOtra = fabrica.usuario(Rol.ADMIN_JUNTA, fabrica.junta("Junta Ajena"));
        mvc.perform(post("/recuperacion/solicitudes/" + id + "/restablecer").with(fabrica.sesion(adminOtra)).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString(socio.getNombreCompleto()))));
        assertThat(solicitudes.findById(id).orElseThrow().getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
    }
}
