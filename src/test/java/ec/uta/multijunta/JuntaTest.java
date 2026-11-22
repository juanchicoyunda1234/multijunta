package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.TipoJunta;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.JuntaRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CU01 Registrar juntas y CU24 Editar o suspender juntas")
class JuntaTest {

    private static final String RUTA = "/plataforma/juntas";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private JuntaRepository juntas;

    private Usuario superAdmin;

    @BeforeEach
    void preparar() {
        superAdmin = fabrica.superAdmin();
    }

    private MockHttpServletRequestBuilder conDatos(MockHttpServletRequestBuilder solicitud, String nombre, String ruc,
            String provincia, String canton) {
        return solicitud.with(fabrica.sesion(superAdmin)).with(csrf())
                .param("nombre", nombre).param("ruc", ruc).param("tipo", "RIEGO")
                .param("provincia", provincia).param("canton", canton);
    }

    private ResultActions registrar(String nombre, String ruc, String provincia, String canton) throws Exception {
        return mvc.perform(conDatos(post(RUTA), nombre, ruc, provincia, canton));
    }

    @Test
    @DisplayName("HU1-1 el inicio del Super Administrador es el listado de juntas")
    void inicioEsListado() throws Exception {
        Junta junta = fabrica.junta("Junta Listado");
        fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        mvc.perform(get("/inicio").with(fabrica.sesion(superAdmin))).andExpect(redirectedUrl(RUTA));
        mvc.perform(get("/plataforma").with(fabrica.sesion(superAdmin))).andExpect(redirectedUrl(RUTA));
        mvc.perform(get(RUTA).with(fabrica.sesion(superAdmin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("+ Nueva Organización")))
                .andExpect(content().string(containsString("Junta Listado")))
                .andExpect(content().string(containsString(junta.getRuc())))
                .andExpect(content().string(containsString("Administradores")))
                .andExpect(content().string(containsString("Registro de Nueva Junta")));
    }

    @Test
    @DisplayName("HU1-3 registro válido: activa, con fecha de hoy y mensaje de éxito")
    void registroValido() throws Exception {
        registrar("Junta Nueva Quero", "1899999999001", "Tungurahua", "Quero")
                .andExpect(redirectedUrl(RUTA))
                .andExpect(flash().attribute("exito", "junta.registrada"));
        Junta junta = juntas.findAll().stream().filter(j -> j.getRuc().equals("1899999999001")).findFirst().orElseThrow();
        assertThat(junta.isActiva()).isTrue();
        assertThat(junta.getFechaRegistro()).isEqualTo(LocalDate.now());
        assertThat(junta.getTipo()).isEqualTo(TipoJunta.RIEGO);
        mvc.perform(get(RUTA).with(fabrica.sesion(superAdmin)).flashAttr("exito", "junta.registrada"))
                .andExpect(content().string(containsString("Junta registrada con éxito")));
    }

    @Test
    @DisplayName("HU1-5 RUC duplicado: el modal sigue abierto con lo escrito y el mensaje")
    void rucDuplicado() throws Exception {
        Junta existente = fabrica.junta("Junta Existente");
        long antes = juntas.count();
        registrar("Junta Repetida", existente.getRuc(), "Tungurahua", "Ambato")
                .andExpect(status().isOk())
                .andExpect(model().attribute("abrir", "registro"))
                .andExpect(model().attributeHasFieldErrorCode("juntaFormulario", "ruc", "junta.rucDuplicado"))
                .andExpect(content().string(containsString("Este RUC ya está registrado")))
                .andExpect(content().string(containsString("value=\"Junta Repetida\"")));
        assertThat(juntas.count()).isEqualTo(antes);
    }

    @Test
    @DisplayName("HU1-6 RUC con formato inválido y campos obligatorios vacíos")
    void formatoYObligatorios() throws Exception {
        for (String ruc : new String[] {"189999999900", "18999999990011", "1899-99999900", " 1899999999001"}) {
            registrar("Junta Formato", ruc, "Tungurahua", "Ambato")
                    .andExpect(model().attributeHasFieldErrors("juntaFormulario", "ruc"))
                    .andExpect(content().string(containsString("El RUC debe tener 13 dígitos")));
        }
        registrar("", "1899999999002", "", "")
                .andExpect(model().attributeHasFieldErrors("juntaFormulario", "nombre", "provincia", "canton"))
                .andExpect(content().string(containsString("Este campo es obligatorio")));
    }

    @Test
    @DisplayName("Borde: un cantón de otra provincia se rechaza")
    void cantonDeOtraProvincia() throws Exception {
        registrar("Junta Cantón", "1899999999003", "Pichincha", "Ambato")
                .andExpect(model().attributeHasFieldErrorCode("juntaFormulario", "canton", "junta.cantonInvalido"));
    }

    @Test
    @DisplayName("HU6-1 y HU6-2 edición con validaciones de CU01")
    void edicion() throws Exception {
        Junta junta = fabrica.junta("Junta Editable");
        Junta otra = fabrica.junta("Junta Otra");
        mvc.perform(conDatos(post(RUTA + "/" + junta.getId()), "Junta Editada", junta.getRuc(), "Pichincha", "Quito"))
                .andExpect(redirectedUrl(RUTA))
                .andExpect(flash().attribute("exito", "junta.actualizada"));
        Junta editada = juntas.findById(junta.getId()).orElseThrow();
        assertThat(editada.getNombre()).isEqualTo("Junta Editada");
        assertThat(editada.getCanton()).isEqualTo("Quito");
        mvc.perform(conDatos(post(RUTA + "/" + junta.getId()), "Junta Editada", otra.getRuc(), "Pichincha", "Quito"))
                .andExpect(model().attribute("abrir", "edicion"))
                .andExpect(model().attributeHasFieldErrorCode("edicionFormulario", "ruc", "junta.rucDuplicado"));
    }

    @Test
    @DisplayName("HU6-3 a HU6-5 suspender con motivo, bloquear el ingreso y reactivar")
    void suspenderYReactivar() throws Exception {
        Junta junta = fabrica.junta("Junta Suspendible");
        Usuario admin = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        var sesionAbierta = fabrica.sesion(admin);
        mvc.perform(post(RUTA + "/" + junta.getId() + "/suspender").with(fabrica.sesion(superAdmin)).with(csrf())
                        .param("motivo", " "))
                .andExpect(flash().attribute("error", "junta.motivoObligatorio"));
        assertThat(juntas.findById(junta.getId()).orElseThrow().isActiva()).isTrue();
        mvc.perform(post(RUTA + "/" + junta.getId() + "/suspender").with(fabrica.sesion(superAdmin)).with(csrf())
                        .param("motivo", "m".repeat(301)))
                .andExpect(flash().attribute("error", "junta.motivoLargo"));
        mvc.perform(get(RUTA).with(fabrica.sesion(superAdmin)).flashAttr("error", "junta.motivoLargo"))
                .andExpect(content().string(containsString("El motivo admite hasta 300 caracteres")));
        assertThat(juntas.findById(junta.getId()).orElseThrow().isActiva()).isTrue();

        mvc.perform(post(RUTA + "/" + junta.getId() + "/suspender").with(fabrica.sesion(superAdmin)).with(csrf())
                        .param("motivo", "Falta de pago"))
                .andExpect(flash().attribute("exito", "junta.suspendida"));
        Junta suspendida = juntas.findById(junta.getId()).orElseThrow();
        assertThat(suspendida.isActiva()).isFalse();
        assertThat(suspendida.getMotivoSuspension()).isEqualTo("Falta de pago");
        mvc.perform(formLogin("/login").user(admin.getUsername()).password(Fabrica.CLAVE))
                .andExpect(redirectedUrl("/login?suspendida"));
        mvc.perform(get("/panel").with(sesionAbierta)).andExpect(redirectedUrl("/login?suspendida"));
        mvc.perform(get(RUTA).with(fabrica.sesion(superAdmin)))
                .andExpect(content().string(containsString("Falta de pago")))
                .andExpect(content().string(containsString("Suspendida")));

        mvc.perform(post(RUTA + "/" + junta.getId() + "/reactivar").with(fabrica.sesion(superAdmin)).with(csrf()))
                .andExpect(flash().attribute("exito", "junta.reactivada"));
        Junta reactivada = juntas.findById(junta.getId()).orElseThrow();
        assertThat(reactivada.isActiva()).isTrue();
        assertThat(reactivada.getMotivoSuspension()).isNull();
        mvc.perform(formLogin("/login").user(admin.getUsername()).password(Fabrica.CLAVE))
                .andExpect(redirectedUrl("/inicio"));
    }

    @Test
    @DisplayName("HU6-6 no existe forma de eliminar una junta")
    void sinEliminacion() throws Exception {
        Junta junta = fabrica.junta("Junta Permanente");
        int estado = mvc.perform(delete(RUTA + "/" + junta.getId()).with(fabrica.sesion(superAdmin)).with(csrf()))
                .andReturn().getResponse().getStatus();
        assertThat(estado).isIn(404, 405);
        mvc.perform(post(RUTA + "/" + junta.getId() + "/eliminar").with(fabrica.sesion(superAdmin)).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(juntas.findById(junta.getId())).isPresent();
    }

    @Test
    @DisplayName("Permisos: solo el Super Administrador gestiona juntas")
    void soloSuperAdmin() throws Exception {
        Usuario admin = fabrica.usuario(Rol.ADMIN_JUNTA, fabrica.junta("Junta Ajena"));
        mvc.perform(get(RUTA).with(fabrica.sesion(admin))).andExpect(status().isForbidden());
        mvc.perform(post(RUTA).with(fabrica.sesion(admin)).with(csrf())).andExpect(status().isForbidden());
    }
}
