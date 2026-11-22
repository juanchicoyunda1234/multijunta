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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.UsuarioRepository;
import ec.uta.multijunta.service.Credencial;
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
@DisplayName("CU23 Gestionar cuentas de la directiva")
class DirectivaTest {

    private static final String RUTA = "/usuarios/directiva";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private UsuarioRepository usuarios;

    private Junta junta;
    private Usuario admin;

    @BeforeEach
    void preparar() {
        junta = fabrica.junta("Junta Directiva");
        admin = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
    }

    private ResultActions crear(String cedula, String email, String rol) throws Exception {
        return mvc.perform(post(RUTA).with(fabrica.sesion(admin)).with(csrf())
                .param("cedula", cedula).param("nombres", "María").param("apellidos", "Toapanta")
                .param("email", email).param("rol", rol));
    }

    private ResultActions editar(Long id, String email, String rol) throws Exception {
        return mvc.perform(post(RUTA + "/" + id).with(fabrica.sesion(admin)).with(csrf())
                .param("nombres", "Lucía").param("apellidos", "Medina").param("email", email).param("rol", rol));
    }

    @Test
    @DisplayName("HU5-1 lista la directiva de su junta")
    void listado() throws Exception {
        Usuario tesorero = fabrica.directiva(Rol.TESORERO, junta, "tesorero@junta.ec");
        Usuario ajeno = fabrica.directiva(Rol.SECRETARIO, fabrica.junta("Junta Ajena"), "ajeno@junta.ec");
        mvc.perform(get(RUTA).with(fabrica.sesion(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(tesorero.getNombreCompleto())))
                .andExpect(content().string(containsString(admin.getNombreCompleto())))
                .andExpect(content().string(containsString("Nueva cuenta")))
                .andExpect(content().string(not(containsString(ajeno.getNombreCompleto()))));
    }

    @Test
    @DisplayName("HU5-2 crea una cuenta con contraseña temporal mostrada una vez")
    void crearCuenta() throws Exception {
        Credencial credencial = (Credencial) crear("1712345678", "Maria@Junta.ec", "SECRETARIO")
                .andExpect(redirectedUrl(RUTA))
                .andExpect(flash().attribute("exito", "directiva.creada"))
                .andReturn().getFlashMap().get("credencial");
        Usuario creado = usuarios.findByCedula("1712345678").orElseThrow();
        assertThat(creado.getRol()).isEqualTo(Rol.SECRETARIO);
        assertThat(creado.getJunta().getId()).isEqualTo(junta.getId());
        assertThat(creado.getUsername()).isEqualTo("1712345678");
        assertThat(creado.isDebeCambiarPassword()).isTrue();
        assertThat(creado.getEmail()).isEqualTo("maria@junta.ec");
        mvc.perform(formLogin("/login").user("1712345678").password(credencial.passwordTemporal()))
                .andExpect(redirectedUrl("/inicio"));
        mvc.perform(get(RUTA).with(fabrica.sesion(admin)))
                .andExpect(content().string(not(containsString(credencial.passwordTemporal()))));
    }

    @Test
    @DisplayName("HU5-3 editar y cambiar de rol rige desde la siguiente acción")
    void editarYCambiarRol() throws Exception {
        Usuario secretario = fabrica.directiva(Rol.SECRETARIO, junta, "secre@junta.ec");
        var sesionAbierta = fabrica.sesion(secretario);
        mvc.perform(get("/panel").with(sesionAbierta)).andExpect(content().string(containsString("Nómina de socios")));
        editar(secretario.getId(), "lucia@junta.ec", "TESORERO")
                .andExpect(redirectedUrl(RUTA))
                .andExpect(flash().attribute("exito", "directiva.actualizada"));
        Usuario editado = usuarios.findById(secretario.getId()).orElseThrow();
        assertThat(editado.getRol()).isEqualTo(Rol.TESORERO);
        assertThat(editado.getNombres()).isEqualTo("Lucía");
        assertThat(editado.getCedula()).isEqualTo(secretario.getCedula());
        mvc.perform(get("/panel").with(sesionAbierta))
                .andExpect(content().string(containsString("Pagos")))
                .andExpect(content().string(containsString("Tesorero/a")));
        mvc.perform(get("/predios").with(sesionAbierta)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("HU5-4 desactivar y reactivar")
    void desactivarYReactivar() throws Exception {
        Usuario tesorero = fabrica.directiva(Rol.TESORERO, junta, "teso@junta.ec");
        mvc.perform(post(RUTA + "/" + tesorero.getId() + "/desactivar").with(fabrica.sesion(admin)).with(csrf()))
                .andExpect(flash().attribute("exito", "directiva.desactivada"));
        mvc.perform(formLogin("/login").user(tesorero.getUsername()).password(Fabrica.CLAVE))
                .andExpect(redirectedUrl("/login?desactivada"));
        mvc.perform(post(RUTA + "/" + tesorero.getId() + "/reactivar").with(fabrica.sesion(admin)).with(csrf()))
                .andExpect(flash().attribute("exito", "directiva.reactivada"));
        mvc.perform(formLogin("/login").user(tesorero.getUsername()).password(Fabrica.CLAVE))
                .andExpect(redirectedUrl("/inicio"));
    }

    @Test
    @DisplayName("HU5-5 cédula y correo duplicados")
    void duplicados() throws Exception {
        Usuario existente = fabrica.directiva(Rol.TESORERO, fabrica.junta("Otra"), "usado@junta.ec");
        crear(existente.getCedula(), "nuevo@junta.ec", "SECRETARIO")
                .andExpect(model().attribute("abrir", "nueva"))
                .andExpect(content().string(containsString("Esta cédula ya está registrada en la plataforma")));
        crear("1712345679", "USADO@junta.ec", "SECRETARIO")
                .andExpect(content().string(containsString("Este correo ya está en uso por otra cuenta administrativa")));
        Usuario propio = fabrica.directiva(Rol.SECRETARIO, junta, "propio@junta.ec");
        editar(propio.getId(), "usado@junta.ec", "SECRETARIO")
                .andExpect(model().attribute("abrir", "edicion"))
                .andExpect(content().string(containsString("Este correo ya está en uso por otra cuenta administrativa")));
        editar(propio.getId(), "propio@junta.ec", "SECRETARIO").andExpect(redirectedUrl(RUTA));
    }

    @Test
    @DisplayName("HU5-6 solo SECRETARIO y TESORERO; las cuentas ADMIN_JUNTA no se gestionan aquí")
    void rolesPermitidos() throws Exception {
        crear("1712345680", "admin2@junta.ec", "ADMIN_JUNTA").andExpect(status().isForbidden());
        crear("1712345681", "super@junta.ec", "SUPER_ADMIN").andExpect(status().isForbidden());
        assertThat(usuarios.findByCedula("1712345680")).isEmpty();
        Usuario secretario = fabrica.directiva(Rol.SECRETARIO, junta, "s@junta.ec");
        editar(secretario.getId(), "s@junta.ec", "ADMIN_JUNTA").andExpect(status().isForbidden());
        Usuario otroAdmin = fabrica.directiva(Rol.ADMIN_JUNTA, junta, "a2@junta.ec");
        editar(otroAdmin.getId(), "a2@junta.ec", "SECRETARIO").andExpect(status().isForbidden());
        mvc.perform(post(RUTA + "/" + otroAdmin.getId() + "/desactivar").with(fabrica.sesion(admin)).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(usuarios.findById(otroAdmin.getId()).orElseThrow().isActivo()).isTrue();
        assertThat(usuarios.findById(secretario.getId()).orElseThrow().getRol()).isEqualTo(Rol.SECRETARIO);
    }

    @Test
    @DisplayName("HU5-7 una cuenta de otra junta responde 404")
    void otraJunta() throws Exception {
        Usuario ajeno = fabrica.directiva(Rol.SECRETARIO, fabrica.junta("Junta Vecina"), "vecino@junta.ec");
        editar(ajeno.getId(), "vecino@junta.ec", "TESORERO").andExpect(status().isNotFound());
        mvc.perform(post(RUTA + "/" + ajeno.getId() + "/desactivar").with(fabrica.sesion(admin)).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(usuarios.findById(ajeno.getId()).orElseThrow().getRol()).isEqualTo(Rol.SECRETARIO);
        assertThat(usuarios.findById(ajeno.getId()).orElseThrow().isActivo()).isTrue();
    }
}
