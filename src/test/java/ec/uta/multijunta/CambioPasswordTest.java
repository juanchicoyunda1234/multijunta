package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CU22 Cambiar contraseña")
class CambioPasswordTest {

    private static final String RUTA = "/cuenta/cambiar-password";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private UsuarioRepository usuarios;

    private Usuario usuario;

    @BeforeEach
    void preparar() {
        usuario = fabrica.usuario(Rol.SOCIO, fabrica.junta("Junta Contraseña"));
    }

    private ResultActions cambiar(String actual, String nueva, String confirmacion) throws Exception {
        return mvc.perform(post(RUTA).with(fabrica.sesion(usuario)).with(csrf())
                .param("actual", actual).param("nueva", nueva).param("confirmacion", confirmacion));
    }

    @Test
    @DisplayName("HU3-1 el formulario pide actual, nueva y confirmación")
    void formulario() throws Exception {
        mvc.perform(get(RUTA).with(fabrica.sesion(usuario)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"actual\"")))
                .andExpect(content().string(containsString("name=\"nueva\"")))
                .andExpect(content().string(containsString("name=\"confirmacion\"")));
    }

    @Test
    @DisplayName("HU3-2 y HU3-7 un cambio válido guarda, confirma, quita la marca y lleva al inicio")
    void cambioValido() throws Exception {
        usuario.setDebeCambiarPassword(true);
        fabrica.guardar(usuario);
        cambiar(Fabrica.CLAVE, "NuevaClave9", "NuevaClave9")
                .andExpect(redirectedUrl("/mi-informacion"))
                .andExpect(flash().attribute("exito", "password.actualizada"));
        assertThat(usuarios.findById(usuario.getId()).orElseThrow().isDebeCambiarPassword()).isFalse();
        mvc.perform(formLogin("/login").user(usuario.getUsername()).password("NuevaClave9"))
                .andExpect(redirectedUrl("/inicio"));
        mvc.perform(formLogin("/login").user(usuario.getUsername()).password(Fabrica.CLAVE))
                .andExpect(redirectedUrl("/login?error"));
        mvc.perform(get("/mi-informacion").with(fabrica.sesion(usuario)).flashAttr("exito", "password.actualizada"))
                .andExpect(content().string(containsString("Contraseña actualizada correctamente")));
    }

    @Test
    @DisplayName("HU3-3 contraseña actual incorrecta")
    void actualIncorrecta() throws Exception {
        cambiar("Equivocada1", "NuevaClave9", "NuevaClave9")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La contraseña actual es incorrecta")));
    }

    @Test
    @DisplayName("HU3-4 nueva y confirmación distintas")
    void confirmacionDistinta() throws Exception {
        cambiar(Fabrica.CLAVE, "NuevaClave9", "NuevaClave8")
                .andExpect(content().string(containsString("La confirmación no coincide con la nueva contraseña")));
    }

    @Test
    @DisplayName("HU3-5 nueva igual a la anterior")
    void igualALaAnterior() throws Exception {
        cambiar(Fabrica.CLAVE, Fabrica.CLAVE, Fabrica.CLAVE)
                .andExpect(content().string(containsString("La nueva contraseña debe ser distinta de la anterior")));
    }

    @Test
    @DisplayName("HU3-6 la nueva contraseña debe tener 8 caracteres, una letra y un número")
    void formatoInvalido() throws Exception {
        String mensaje = "La contraseña debe tener al menos 8 caracteres, con al menos una letra y un número";
        cambiar(Fabrica.CLAVE, "Corta1", "Corta1").andExpect(content().string(containsString(mensaje)));
        cambiar(Fabrica.CLAVE, "SoloLetras", "SoloLetras").andExpect(content().string(containsString(mensaje)));
        cambiar(Fabrica.CLAVE, "12345678", "12345678").andExpect(content().string(containsString(mensaje)));
        assertThat(usuarios.findById(usuario.getId()).orElseThrow().getPassword()).isEqualTo(usuario.getPassword());
    }

    @Test
    @DisplayName("Borde: quien está obligado a cambiar puede cerrar sesión y sigue obligado")
    void obligadoPuedeCerrarSesion() throws Exception {
        usuario.setDebeCambiarPassword(true);
        fabrica.guardar(usuario);
        mvc.perform(get(RUTA).with(fabrica.sesion(usuario)))
                .andExpect(content().string(containsString("Debes cambiar tu contraseña")));
        mvc.perform(post("/logout").with(fabrica.sesion(usuario)).with(csrf()))
                .andExpect(redirectedUrl("/login?salio"));
        mvc.perform(get("/mi-informacion").with(fabrica.sesion(usuario))).andExpect(redirectedUrl(RUTA));
    }
}
