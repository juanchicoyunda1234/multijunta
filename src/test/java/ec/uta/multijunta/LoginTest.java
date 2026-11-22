package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.UsuarioRepository;
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
@DisplayName("CU20 Iniciar y cerrar sesión")
class LoginTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private UsuarioRepository usuarios;

    private Junta junta;

    @BeforeEach
    void preparar() {
        junta = fabrica.junta("Junta Login");
    }

    @Test
    @DisplayName("HU1-1 sin sesión cualquier página lleva al formulario de inicio")
    void sinSesionVaAlLogin() throws Exception {
        mvc.perform(get("/socios")).andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Iniciar Sesión")))
                .andExpect(content().string(containsString("¿Olvidaste tu contraseña?")));
    }

    @Test
    @DisplayName("HU1-2 la directiva entra y llega al panel")
    void directivaLlegaAlPanel() throws Exception {
        for (Rol rol : new Rol[] {Rol.ADMIN_JUNTA, Rol.SECRETARIO, Rol.TESORERO}) {
            Usuario usuario = fabrica.usuario(rol, junta);
            mvc.perform(formLogin("/login").user(usuario.getUsername()).password(Fabrica.CLAVE))
                    .andExpect(redirectedUrl("/inicio"));
            mvc.perform(get("/inicio").with(fabrica.sesion(usuario))).andExpect(redirectedUrl("/panel"));
        }
    }

    @Test
    @DisplayName("HU1-3 el socio entra y llega a Mi información")
    void socioLlegaAMiInformacion() throws Exception {
        Usuario socio = fabrica.usuario(Rol.SOCIO, junta);
        mvc.perform(formLogin("/login").user(socio.getUsername()).password(Fabrica.CLAVE))
                .andExpect(redirectedUrl("/inicio"));
        mvc.perform(get("/inicio").with(fabrica.sesion(socio))).andExpect(redirectedUrl("/mi-informacion"));
    }

    @Test
    @DisplayName("HU1-4 el Super Administrador llega al listado de juntas")
    void superAdminLlegaAPlataforma() throws Exception {
        Usuario admin = fabrica.superAdmin();
        mvc.perform(formLogin("/login").user(admin.getUsername()).password(Fabrica.CLAVE))
                .andExpect(redirectedUrl("/inicio"));
        mvc.perform(get("/inicio").with(fabrica.sesion(admin))).andExpect(redirectedUrl("/plataforma"));
        mvc.perform(get("/plataforma").with(fabrica.sesion(admin))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("HU1-5 una cuenta que debe cambiar contraseña es llevada al cambio")
    void debeCambiarPasswordEsRedirigido() throws Exception {
        Usuario usuario = fabrica.usuario(Rol.SECRETARIO, junta);
        usuario.setDebeCambiarPassword(true);
        fabrica.guardar(usuario);
        mvc.perform(get("/panel").with(fabrica.sesion(usuario))).andExpect(redirectedUrl("/cuenta/cambiar-password"));
        mvc.perform(get("/notificaciones").with(fabrica.sesion(usuario)))
                .andExpect(redirectedUrl("/cuenta/cambiar-password"));
        mvc.perform(get("/cuenta/cambiar-password").with(fabrica.sesion(usuario))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("HU1-6 credenciales incorrectas muestran el mismo mensaje")
    void credencialesIncorrectas() throws Exception {
        Usuario usuario = fabrica.usuario(Rol.TESORERO, junta);
        mvc.perform(formLogin("/login").user(usuario.getUsername()).password("otraClave1"))
                .andExpect(redirectedUrl("/login?error"));
        mvc.perform(formLogin("/login").user("noexiste").password("otraClave1"))
                .andExpect(redirectedUrl("/login?error"));
        mvc.perform(get("/login").param("error", ""))
                .andExpect(content().string(containsString("Usuario o contraseña inválidos")));
    }

    @Test
    @DisplayName("HU1-7 una cuenta desactivada no entra")
    void cuentaDesactivada() throws Exception {
        Usuario usuario = fabrica.usuario(Rol.SECRETARIO, junta);
        usuario.setActivo(false);
        fabrica.guardar(usuario);
        mvc.perform(formLogin("/login").user(usuario.getUsername()).password(Fabrica.CLAVE))
                .andExpect(redirectedUrl("/login?desactivada"));
        mvc.perform(formLogin("/login").user(usuario.getUsername()).password("otraClave1"))
                .andExpect(redirectedUrl("/login?error"));
        mvc.perform(get("/login").param("desactivada", ""))
                .andExpect(content().string(containsString("Cuenta desactivada, contacte a la directiva")));
    }

    @Test
    @DisplayName("HU1-8 los usuarios de una junta suspendida no entran")
    void juntaSuspendida() throws Exception {
        Usuario usuario = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        junta.setActiva(false);
        fabrica.guardar(junta);
        mvc.perform(formLogin("/login").user(usuario.getUsername()).password(Fabrica.CLAVE))
                .andExpect(redirectedUrl("/login?suspendida"));
        mvc.perform(get("/login").param("suspendida", ""))
                .andExpect(content().string(containsString("La organización se encuentra suspendida")));
    }

    @Test
    @DisplayName("HU1-9 el quinto intento fallido bloquea la cuenta 15 minutos")
    void bloqueoTrasCincoIntentos() throws Exception {
        Usuario usuario = fabrica.usuario(Rol.TESORERO, junta);
        for (int intento = 1; intento <= 4; intento++) {
            mvc.perform(formLogin("/login").user(usuario.getUsername()).password("malaClave1"))
                    .andExpect(redirectedUrl("/login?error"));
        }
        mvc.perform(formLogin("/login").user(usuario.getUsername()).password("malaClave1"))
                .andExpect(redirectedUrl("/login?bloqueada"));
        mvc.perform(formLogin("/login").user(usuario.getUsername()).password(Fabrica.CLAVE))
                .andExpect(redirectedUrl("/login?bloqueada"));
        Usuario guardado = usuarios.findById(usuario.getId()).orElseThrow();
        assertThat(guardado.getBloqueadoHasta()).isAfter(java.time.LocalDateTime.now().plusMinutes(14));
        mvc.perform(get("/login").param("bloqueada", ""))
                .andExpect(content().string(containsString("Cuenta bloqueada temporalmente, intente de nuevo en 15 minutos")));
    }

    @Test
    @DisplayName("HU1-10 un ingreso correcto reinicia el contador de intentos")
    void ingresoCorrectoReiniciaContador() throws Exception {
        Usuario usuario = fabrica.usuario(Rol.SECRETARIO, junta);
        for (int intento = 1; intento <= 3; intento++) {
            mvc.perform(formLogin("/login").user(usuario.getUsername()).password("malaClave1"));
        }
        assertThat(usuarios.findById(usuario.getId()).orElseThrow().getIntentosFallidos()).isEqualTo(3);
        mvc.perform(formLogin("/login").user(usuario.getUsername()).password(Fabrica.CLAVE))
                .andExpect(redirectedUrl("/inicio"));
        assertThat(usuarios.findById(usuario.getId()).orElseThrow().getIntentosFallidos()).isZero();
    }

    @Test
    @DisplayName("HU1-11 cerrar sesión vuelve al inicio y las páginas no quedan en caché")
    void cerrarSesion() throws Exception {
        Usuario usuario = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        mvc.perform(get("/panel").with(fabrica.sesion(usuario)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(content().string(containsString("Cerrar Sesión")));
        mvc.perform(post("/logout").with(fabrica.sesion(usuario)).with(csrf()))
                .andExpect(redirectedUrl("/login?salio"));
    }

    @Test
    @DisplayName("Borde: el usuario ignora espacios y mayúsculas")
    void usuarioSinEspaciosNiMayusculas() throws Exception {
        Usuario usuario = fabrica.usuario(Rol.SECRETARIO, junta);
        mvc.perform(formLogin("/login").user("  " + usuario.getUsername().toUpperCase() + " ").password(Fabrica.CLAVE))
                .andExpect(redirectedUrl("/inicio"));
    }

    @Test
    @DisplayName("Borde: una sesión expirada vuelve al inicio de sesión con aviso")
    void sesionExpirada() throws Exception {
        mvc.perform(get("/panel").with(solicitud -> {
                    solicitud.setRequestedSessionId("sesion-vencida");
                    solicitud.setRequestedSessionIdValid(false);
                    return solicitud;
                }))
                .andExpect(redirectedUrl("/login?expirada"));
    }

    @Test
    @DisplayName("FR-010 desactivar la cuenta o suspender la junta cierra la sesión abierta")
    void cierreDeSesionPorCambioDeEstado() throws Exception {
        Usuario usuario = fabrica.usuario(Rol.SECRETARIO, junta);
        var sesion = fabrica.sesion(usuario);
        usuario.setActivo(false);
        fabrica.guardar(usuario);
        mvc.perform(get("/panel").with(sesion)).andExpect(redirectedUrl("/login?desactivada"));

        Usuario otro = fabrica.usuario(Rol.TESORERO, junta);
        var otraSesion = fabrica.sesion(otro);
        junta.setActiva(false);
        fabrica.guardar(junta);
        mvc.perform(get("/panel").with(otraSesion)).andExpect(redirectedUrl("/login?suspendida"));
    }
}
