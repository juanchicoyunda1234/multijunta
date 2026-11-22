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

import ec.uta.multijunta.model.EstadoCuenta;
import ec.uta.multijunta.model.EstadoSocio;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.SocioRepository;
import ec.uta.multijunta.service.Credencial;
import java.util.ArrayList;
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
@DisplayName("CU04 Generar accesos de socios")
class CuentasSociosTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private SocioRepository socios;

    private Junta junta;
    private Sector sector;
    private Usuario admin;

    @BeforeEach
    void preparar() {
        junta = fabrica.junta("Junta Socios");
        sector = fabrica.sector(junta);
        admin = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
    }

    @SuppressWarnings("unchecked")
    private List<Credencial> crear(Long... ids) throws Exception {
        var solicitud = post("/usuarios/socios/credenciales").with(fabrica.sesion(admin)).with(csrf());
        for (Long id : ids) {
            solicitud.param("socioIds", id.toString());
        }
        MvcResult resultado = mvc.perform(solicitud).andExpect(redirectedUrl("/usuarios/credenciales")).andReturn();
        return (List<Credencial>) resultado.getFlashMap().get("credenciales");
    }

    @Test
    @DisplayName("HU4-1 la nómina muestra estados de cuenta y casillas")
    void nomina() throws Exception {
        Socio sinCuenta = fabrica.socioSinCuenta(junta, sector, EstadoSocio.ACTIVO);
        Socio inactivo = fabrica.socioSinCuenta(junta, sector, EstadoSocio.INACTIVO);
        Socio conCuenta = fabrica.socio(junta, sector);
        mvc.perform(get("/usuarios").with(fabrica.sesion(admin))).andExpect(redirectedUrl("/usuarios/socios"));
        String html = mvc.perform(get("/usuarios/socios").with(fabrica.sesion(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(sinCuenta.getNombreCompleto())))
                .andExpect(content().string(containsString("Sin cuenta")))
                .andExpect(content().string(containsString("Activa")))
                .andExpect(content().string(containsString("Crear Credenciales")))
                .andExpect(content().string(containsString("socios seleccionados")))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).containsPattern("value=\"" + inactivo.getId() + "\"[^>]*disabled");
        assertThat(html).containsPattern("value=\"" + conCuenta.getId() + "\"[^>]*disabled");
        assertThat(html).doesNotContainPattern("value=\"" + sinCuenta.getId() + "\"[^>]*disabled");
    }

    @Test
    @DisplayName("HU4-2 y HU4-8 crea cuentas con contraseñas distintas, marca de cambio e ingreso obligado")
    void creacionEnBloque() throws Exception {
        Socio uno = fabrica.socioSinCuenta(junta, sector, EstadoSocio.ACTIVO);
        Socio dos = fabrica.socioSinCuenta(junta, sector, EstadoSocio.ACTIVO);
        List<Credencial> credenciales = crear(uno.getId(), dos.getId());
        assertThat(credenciales).hasSize(2);
        assertThat(credenciales.get(0).passwordTemporal()).isNotEqualTo(credenciales.get(1).passwordTemporal());
        assertThat(credenciales).extracting(Credencial::username).containsExactlyInAnyOrder(uno.getCedula(), dos.getCedula());
        Socio actualizado = socios.findById(uno.getId()).orElseThrow();
        assertThat(actualizado.getEstadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA);
        assertThat(actualizado.isDebeCambiarPassword()).isTrue();

        mvc.perform(get("/usuarios/credenciales").with(fabrica.sesion(admin)).flashAttr("credenciales", credenciales))
                .andExpect(content().string(containsString(credenciales.get(0).passwordTemporal())))
                .andExpect(content().string(containsString("Imprimir")));
        mvc.perform(get("/usuarios/credenciales").with(fabrica.sesion(admin))).andExpect(redirectedUrl("/usuarios/socios"));

        String clave = credenciales.stream().filter(c -> c.username().equals(uno.getCedula())).findFirst().orElseThrow()
                .passwordTemporal();
        mvc.perform(formLogin("/login").user(uno.getCedula()).password(clave)).andExpect(redirectedUrl("/inicio"));
        mvc.perform(get("/mi-informacion").with(fabrica.sesion(actualizado)))
                .andExpect(redirectedUrl("/cuenta/cambiar-password"));
    }

    @Test
    @DisplayName("HU4-4 un socio INACTIVO o con cuenta no recibe credenciales")
    void inactivoIgnorado() throws Exception {
        Socio inactivo = fabrica.socioSinCuenta(junta, sector, EstadoSocio.INACTIVO);
        Socio conCuenta = fabrica.socio(junta, sector);
        String claveAnterior = conCuenta.getPassword();
        assertThat(crear(inactivo.getId(), conCuenta.getId())).isEmpty();
        assertThat(socios.findById(inactivo.getId()).orElseThrow().getEstadoCuenta()).isEqualTo(EstadoCuenta.SIN_CUENTA);
        assertThat(socios.findById(conCuenta.getId()).orElseThrow().getPassword()).isEqualTo(claveAnterior);
    }

    @Test
    @DisplayName("HU4-5 sin selección se muestra el mensaje")
    void seleccionVacia() throws Exception {
        mvc.perform(post("/usuarios/socios/credenciales").with(fabrica.sesion(admin)).with(csrf()))
                .andExpect(redirectedUrl("/usuarios/socios"))
                .andExpect(flash().attribute("error", "socios.seleccionVacia"));
        mvc.perform(get("/usuarios/socios").with(fabrica.sesion(admin)).flashAttr("error", "socios.seleccionVacia"))
                .andExpect(content().string(containsString("Seleccione al menos un socio")));
    }

    @Test
    @DisplayName("HU4-6 y HU4-7 desactivar impide el ingreso; reactivar conserva la contraseña")
    void desactivarYReactivar() throws Exception {
        Socio socio = fabrica.socio(junta, sector);
        var sesion = fabrica.sesion(socio);
        mvc.perform(post("/usuarios/socios/" + socio.getId() + "/desactivar").with(fabrica.sesion(admin)).with(csrf()))
                .andExpect(flash().attribute("exito", "socios.desactivada"));
        assertThat(socios.findById(socio.getId()).orElseThrow().getEstadoCuenta()).isEqualTo(EstadoCuenta.DESACTIVADA);
        mvc.perform(formLogin("/login").user(socio.getUsername()).password(Fabrica.CLAVE))
                .andExpect(redirectedUrl("/login?desactivada"));
        mvc.perform(get("/mi-informacion").with(sesion)).andExpect(redirectedUrl("/login?desactivada"));
        mvc.perform(get("/usuarios/socios").with(fabrica.sesion(admin)))
                .andExpect(content().string(containsString("Desactivada")))
                .andExpect(content().string(containsString("Reactivar")));

        mvc.perform(post("/usuarios/socios/" + socio.getId() + "/reactivar").with(fabrica.sesion(admin)).with(csrf()))
                .andExpect(flash().attribute("exito", "socios.reactivada"));
        mvc.perform(formLogin("/login").user(socio.getUsername()).password(Fabrica.CLAVE))
                .andExpect(redirectedUrl("/inicio"));
    }

    @Test
    @DisplayName("Aislamiento: un socio de otra junta responde 404 y nada cambia")
    void otraJunta() throws Exception {
        Junta otra = fabrica.junta("Junta Lejana");
        Socio ajeno = fabrica.socioSinCuenta(otra, fabrica.sector(otra), EstadoSocio.ACTIVO);
        Socio propio = fabrica.socioSinCuenta(junta, sector, EstadoSocio.ACTIVO);
        mvc.perform(post("/usuarios/socios/credenciales").with(fabrica.sesion(admin)).with(csrf())
                        .param("socioIds", propio.getId().toString(), ajeno.getId().toString()))
                .andExpect(status().isNotFound());
        assertThat(socios.findById(propio.getId()).orElseThrow().getEstadoCuenta()).isEqualTo(EstadoCuenta.SIN_CUENTA);
        assertThat(socios.findById(ajeno.getId()).orElseThrow().getEstadoCuenta()).isEqualTo(EstadoCuenta.SIN_CUENTA);
        mvc.perform(post("/usuarios/socios/" + ajeno.getId() + "/desactivar").with(fabrica.sesion(admin)).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/usuarios/socios").with(fabrica.sesion(admin)))
                .andExpect(content().string(not(containsString(ajeno.getNombreCompleto()))));
    }

    @Test
    @DisplayName("SC-003 genera 50 credenciales en una operación en menos de 10 segundos")
    void cincuentaSocios() throws Exception {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            ids.add(fabrica.socioSinCuenta(junta, sector, EstadoSocio.ACTIVO).getId());
        }
        long inicio = System.nanoTime();
        List<Credencial> credenciales = crear(ids.toArray(Long[]::new));
        long milisegundos = (System.nanoTime() - inicio) / 1_000_000;
        assertThat(credenciales).hasSize(50);
        assertThat(milisegundos).isLessThan(10_000);
    }
}
