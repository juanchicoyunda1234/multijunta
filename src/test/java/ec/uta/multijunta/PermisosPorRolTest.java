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

import ec.uta.multijunta.config.MatrizPermisos;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Usuario;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
@DisplayName("Matriz de permisos por rol (docs/contexto/seguridad.md)")
class PermisosPorRolTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    static Stream<Arguments> combinaciones() {
        return MatrizPermisos.entradas().stream()
                .flatMap(entrada -> Arrays.stream(Rol.values()).map(rol -> Arguments.of(entrada, rol)));
    }

    @ParameterizedTest(name = "{0} con rol {1}")
    @MethodSource("combinaciones")
    @DisplayName("SC-003 cada fila × rol se comporta según la matriz")
    void matrizCompleta(MatrizPermisos.Entrada entrada, Rol rol) throws Exception {
        Usuario usuario = rol == Rol.SUPER_ADMIN ? fabrica.superAdmin() : fabrica.usuario(rol, fabrica.junta("Junta Permisos"));
        int lectura = mvc.perform(get(entrada.ruta()).with(fabrica.sesion(usuario))).andReturn().getResponse().getStatus();
        int escritura = mvc.perform(post(entrada.ruta()).with(fabrica.sesion(usuario)).with(csrf()))
                .andReturn().getResponse().getStatus();
        if (entrada.completo().contains(rol)) {
            assertThat(lectura).as("GET permitido").isNotEqualTo(403);
            assertThat(escritura).as("POST permitido").isNotEqualTo(403);
        } else if (entrada.lectura().contains(rol)) {
            assertThat(lectura).as("GET de solo lectura").isNotEqualTo(403);
            assertThat(escritura).as("POST negado en solo lectura").isEqualTo(403);
        } else {
            assertThat(lectura).as("GET negado").isEqualTo(403);
            assertThat(escritura).as("POST negado").isEqualTo(403);
        }
    }

    @Test
    @DisplayName("HU2-1 el menú muestra solo las opciones del rol y la barra sus datos")
    void menuSegunRol() throws Exception {
        Junta junta = fabrica.junta("Junta Menú");
        Usuario secretario = fabrica.usuario(Rol.SECRETARIO, junta);
        mvc.perform(get("/panel").with(fabrica.sesion(secretario)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nómina de socios")))
                .andExpect(content().string(containsString("Multas")))
                .andExpect(content().string(not(containsString(">Pagos<"))))
                .andExpect(content().string(not(containsString("Solicitudes de recuperación"))))
                .andExpect(content().string(containsString("Secretario/a")))
                .andExpect(content().string(containsString("Junta Menú")))
                .andExpect(content().string(containsString("Cambiar contraseña")));

        Usuario socio = fabrica.usuario(Rol.SOCIO, junta);
        mvc.perform(get("/mi-informacion").with(fabrica.sesion(socio)))
                .andExpect(content().string(containsString("Anuncios y repositorio")))
                .andExpect(content().string(not(containsString("Nómina de socios"))));

        Usuario superAdmin = fabrica.superAdmin();
        mvc.perform(get("/plataforma/juntas").with(fabrica.sesion(superAdmin)))
                .andExpect(content().string(containsString("Juntas")))
                .andExpect(content().string(containsString("Super Administrador")))
                .andExpect(content().string(not(containsString(" · Junta Menú"))));
    }

    @Test
    @DisplayName("HU2-2 y HU2-5 acceso sin permiso responde 403, también para el Super Administrador")
    void accesoNegado() throws Exception {
        Usuario socio = fabrica.usuario(Rol.SOCIO, fabrica.junta("Junta 403"));
        mvc.perform(get("/pagos").with(fabrica.sesion(socio))).andExpect(status().isForbidden());
        mvc.perform(get("/socios").with(fabrica.sesion(fabrica.superAdmin()))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("HU2-3 solo lectura no puede modificar")
    void soloLecturaNoModifica() throws Exception {
        Usuario tesorero = fabrica.usuario(Rol.TESORERO, fabrica.junta("Junta Lectura"));
        mvc.perform(get("/socios").with(fabrica.sesion(tesorero))).andExpect(redirectedUrl("/en-construccion"));
        mvc.perform(post("/socios").with(fabrica.sesion(tesorero)).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("HU2-6 una página inexistente responde 404")
    void paginaInexistente() throws Exception {
        Usuario admin = fabrica.usuario(Rol.ADMIN_JUNTA, fabrica.junta("Junta 404"));
        mvc.perform(get("/no-existe").with(fabrica.sesion(admin))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("FR-018 los módulos sin construir llevan a la página en construcción")
    void moduloEnConstruccion() throws Exception {
        Usuario secretario = fabrica.usuario(Rol.SECRETARIO, fabrica.junta("Junta Construcción"));
        mvc.perform(get("/predios").with(fabrica.sesion(secretario))).andExpect(redirectedUrl("/en-construccion"));
        mvc.perform(get("/en-construccion").with(fabrica.sesion(secretario)))
                .andExpect(content().string(containsString("Módulo en construcción")));
    }
}
