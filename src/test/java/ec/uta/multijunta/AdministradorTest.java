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
@DisplayName("CU02 Asignar Administrador de la Junta")
class AdministradorTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private UsuarioRepository usuarios;

    private Usuario superAdmin;
    private Junta junta;

    @BeforeEach
    void preparar() {
        superAdmin = fabrica.superAdmin();
        junta = fabrica.junta("Junta Administrada");
    }

    private ResultActions asignar(Junta destino, String cedula, String email, Long reemplazadoId) throws Exception {
        var solicitud = post("/plataforma/juntas/" + destino.getId() + "/administradores")
                .with(fabrica.sesion(superAdmin)).with(csrf())
                .param("cedula", cedula).param("nombres", "Carlos").param("apellidos", "Guamán")
                .param("email", email);
        if (reemplazadoId != null) {
            solicitud.param("reemplazadoId", reemplazadoId.toString());
        }
        return mvc.perform(solicitud);
    }

    @Test
    @DisplayName("HU2-1, HU2-2 y HU2-7 crea la cuenta, muestra credenciales una vez y obliga a cambiar la contraseña")
    void asignacionValida() throws Exception {
        mvc.perform(get("/plataforma/juntas").with(fabrica.sesion(superAdmin)))
                .andExpect(content().string(containsString("Generar Cuenta y Asignar Rol")))
                .andExpect(content().string(containsString("Junta destino")));
        Credencial credencial = (Credencial) asignar(junta, "1803948271", "CGuaman@Pillaro-Riego.ec", null)
                .andExpect(redirectedUrl("/plataforma/juntas"))
                .andExpect(flash().attribute("credencialJunta", containsString(junta.getRuc())))
                .andReturn().getFlashMap().get("credencial");
        assertThat(credencial.username()).isEqualTo("1803948271");
        assertThat(credencial.passwordTemporal()).matches("(?=.*[A-Za-z])(?=.*\\d).{8,}");
        Usuario creado = usuarios.findByCedula("1803948271").orElseThrow();
        assertThat(creado.getRol()).isEqualTo(Rol.ADMIN_JUNTA);
        assertThat(creado.getJunta().getId()).isEqualTo(junta.getId());
        assertThat(creado.isDebeCambiarPassword()).isTrue();
        assertThat(creado.getEmail()).isEqualTo("cguaman@pillaro-riego.ec");

        mvc.perform(get("/plataforma/juntas").with(fabrica.sesion(superAdmin)).flashAttr("credencial", credencial)
                        .flashAttr("credencialJunta", "Junta"))
                .andExpect(content().string(containsString(credencial.passwordTemporal())));
        mvc.perform(get("/plataforma/juntas").with(fabrica.sesion(superAdmin)))
                .andExpect(content().string(not(containsString(credencial.passwordTemporal()))));

        mvc.perform(formLogin("/login").user("1803948271").password(credencial.passwordTemporal()))
                .andExpect(redirectedUrl("/inicio"));
        mvc.perform(get("/panel").with(fabrica.sesion(creado))).andExpect(redirectedUrl("/cuenta/cambiar-password"));
    }

    @Test
    @DisplayName("HU2-3 no se asigna un tercer administrador activo")
    void limiteDeDos() throws Exception {
        asignar(junta, "1803948271", "uno@junta.ec", null).andExpect(redirectedUrl("/plataforma/juntas"));
        asignar(junta, "1803948272", "dos@junta.ec", null).andExpect(redirectedUrl("/plataforma/juntas"));
        asignar(junta, "1803948273", "tres@junta.ec", null)
                .andExpect(status().isOk())
                .andExpect(model().attribute("abrir", "administrador"))
                .andExpect(content().string(containsString("La junta ya tiene 2 administradores activos; use Reasignar")));
        assertThat(usuarios.findByCedula("1803948273")).isEmpty();
    }

    @Test
    @DisplayName("HU2-4 reasignar desactiva la cuenta elegida y crea la nueva")
    void reasignar() throws Exception {
        Usuario anterior = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        asignar(junta, "1803948274", "nuevo@junta.ec", anterior.getId())
                .andExpect(redirectedUrl("/plataforma/juntas"))
                .andExpect(flash().attributeExists("credencial"));
        assertThat(usuarios.findById(anterior.getId()).orElseThrow().isActivo()).isFalse();
        assertThat(usuarios.countByJuntaIdAndRolAndActivoTrue(junta.getId(), Rol.ADMIN_JUNTA)).isEqualTo(2);
        mvc.perform(formLogin("/login").user(anterior.getUsername()).password(Fabrica.CLAVE))
                .andExpect(redirectedUrl("/login?desactivada"));
    }

    @Test
    @DisplayName("HU2-5 correo en uso por otra cuenta administrativa de cualquier junta")
    void correoDuplicado() throws Exception {
        fabrica.directiva(Rol.TESORERO, fabrica.junta("Otra Junta"), "tesorero@otra.ec");
        asignar(junta, "1803948275", "TESORERO@otra.ec", null)
                .andExpect(model().attributeHasFieldErrorCode("administradorFormulario", "email", "cuenta.correoDuplicado"))
                .andExpect(content().string(containsString("Este correo ya está en uso por otra cuenta administrativa")));
        assertThat(usuarios.findByCedula("1803948275")).isEmpty();
    }

    @Test
    @DisplayName("HU2-6 cédula ya registrada, incluso de un socio sin cuenta")
    void cedulaDuplicada() throws Exception {
        Usuario socio = fabrica.socioSinCuenta(junta, fabrica.sector(junta), ec.uta.multijunta.model.EstadoSocio.ACTIVO);
        asignar(junta, socio.getCedula(), "otro@junta.ec", null)
                .andExpect(model().attributeHasFieldErrorCode("administradorFormulario", "cedula", "cuenta.cedulaDuplicada"))
                .andExpect(content().string(containsString("Esta cédula ya está registrada en la plataforma")));
    }

    @Test
    @DisplayName("Borde: una junta suspendida admite asignar administrador")
    void juntaSuspendida() throws Exception {
        junta.setActiva(false);
        fabrica.guardar(junta);
        asignar(junta, "1803948276", "suspendida@junta.ec", null).andExpect(flash().attributeExists("credencial"));
    }

    @Test
    @DisplayName("Aislamiento: reasignar un administrador de otra junta responde 404 sin cambios")
    void reemplazadoDeOtraJunta() throws Exception {
        Usuario ajeno = fabrica.usuario(Rol.ADMIN_JUNTA, fabrica.junta("Junta Lejana"));
        asignar(junta, "1803948277", "lejano@junta.ec", ajeno.getId()).andExpect(status().isNotFound());
        assertThat(usuarios.findById(ajeno.getId()).orElseThrow().isActivo()).isTrue();
        assertThat(usuarios.findByCedula("1803948277")).isEmpty();
    }
}
