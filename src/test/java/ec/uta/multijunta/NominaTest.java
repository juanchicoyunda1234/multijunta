package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.uta.multijunta.model.EstadoSocio;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.SocioRepository;
import java.time.LocalDate;
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
@DisplayName("CU05 Registrar y mantener nómina")
class NominaTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private SocioRepository socios;

    private Junta junta;
    private Sector sector;
    private Usuario secretario;

    @BeforeEach
    void preparar() {
        junta = fabrica.junta("Junta Nómina");
        sector = fabrica.sector(junta);
        secretario = fabrica.secretario(junta);
    }

    private ResultActions registrar(String cedula, String celular) throws Exception {
        return mvc.perform(post("/socios").with(fabrica.sesion(secretario)).with(csrf())
                .param("cedula", cedula).param("nombres", "Segundo Manuel").param("apellidos", "Chango Quispe")
                .param("celular", celular).param("sectorId", sector.getId().toString()));
    }

    @Test
    @DisplayName("HU1-1 y HU1-2 padrón con buscador y formulario en dos tarjetas con validación de cédula")
    void padronYFormulario() throws Exception {
        Socio uno = fabrica.socio(junta, sector);
        Socio dos = fabrica.socio(junta, sector);
        String html = mvc.perform(get("/socios").with(fabrica.sesion(secretario)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Padrón de Miembros")))
                .andExpect(content().string(containsString("Añadir Nuevo Socio")))
                .andReturn().getResponse().getContentAsString();
        assertThat(html.indexOf(dos.getCedula())).isLessThan(html.indexOf(uno.getCedula()));
        mvc.perform(get("/socios").param("q", uno.getCedula()).with(fabrica.sesion(secretario)))
                .andExpect(content().string(containsString(uno.getNombreCompleto())))
                .andExpect(content().string(not(containsString(dos.getCedula()))));
        mvc.perform(get("/socios/nuevo").with(fabrica.sesion(secretario)))
                .andExpect(content().string(containsString("Datos Personales")))
                .andExpect(content().string(containsString("Datos de Contacto")))
                .andExpect(content().string(containsString("Guardar Ficha de Socio")))
                .andExpect(content().string(containsString("botonGuardarSocio")))
                .andExpect(content().string(containsString("\\d{10}")));
    }

    @Test
    @DisplayName("HU1-3 registro válido: ACTIVO, sin cuenta, código y fecha de hoy")
    void registroValido() throws Exception {
        registrar("1802938471", "0991234567")
                .andExpect(redirectedUrl("/socios"))
                .andExpect(flash().attribute("exito", "socio.registrado"));
        Socio socio = socios.findAllByJuntaId(junta.getId()).stream()
                .filter(s -> s.getCedula().equals("1802938471")).findFirst().orElseThrow();
        assertThat(socio.getCodigoSocio()).startsWith("S-");
        assertThat(socio.getFechaIngreso()).isEqualTo(LocalDate.now());
        assertThat(socio.getEstado()).isEqualTo(EstadoSocio.ACTIVO);
        assertThat(socio.isTieneCuenta()).isFalse();
        assertThat(socio.isActivo()).isFalse();
        assertThat(socio.getUsername()).isEqualTo("1802938471");
        assertThat(socio.getPassword()).isNotBlank();
        mvc.perform(get("/socios").with(fabrica.sesion(secretario)).flashAttr("exito", "socio.registrado"))
                .andExpect(content().string(containsString("Socio registrado")));
    }

    @Test
    @DisplayName("HU1-4 y HU1-5 cédula inválida o ya registrada; celular inválido")
    void cedulaYCelularInvalidos() throws Exception {
        for (String cedula : new String[] {"12345", "18029384AB", "180293847123"}) {
            registrar(cedula, "").andExpect(model().attributeHasFieldErrorCode("socioFormulario", "cedula", "Pattern"))
                    .andExpect(content().string(containsString("La cédula debe tener 10 dígitos")));
        }
        Usuario tesorero = fabrica.usuario(Rol.TESORERO, fabrica.junta("Otra Junta"));
        registrar(tesorero.getCedula(), "")
                .andExpect(content().string(containsString("Esta cédula ya está registrada en la plataforma")));
        registrar("1802938472", "099123")
                .andExpect(content().string(containsString("El celular debe tener 10 dígitos")));
    }

    @Test
    @DisplayName("HU1-6 junta sin sectores")
    void juntaSinSectores() throws Exception {
        Junta sinSectores = fabrica.junta("Junta Sin Sectores");
        Usuario otroSecretario = fabrica.secretario(sinSectores);
        mvc.perform(get("/socios/nuevo").with(fabrica.sesion(otroSecretario)))
                .andExpect(content().string(containsString("Registre al menos un sector para poder inscribir socios")));
        mvc.perform(post("/socios").with(fabrica.sesion(otroSecretario)).with(csrf())
                        .param("cedula", "1802938473").param("nombres", "A").param("apellidos", "B")
                        .param("sectorId", sector.getId().toString()))
                .andExpect(content().string(containsString("Registre al menos un sector")));
    }

    @Test
    @DisplayName("HU1-7 y HU1-8 editar sin cambiar la cédula y cambiar el estado")
    void editarYEstado() throws Exception {
        Socio socio = fabrica.socio(junta, sector);
        Sector otro = fabrica.sector(junta);
        String cedula = socio.getCedula();
        mvc.perform(post("/socios/" + socio.getId()).with(fabrica.sesion(secretario)).with(csrf())
                        .param("cedula", "9999999999").param("nombres", "Nuevo").param("apellidos", "Nombre")
                        .param("celular", "0987654321").param("sectorId", otro.getId().toString()))
                .andExpect(redirectedUrl("/socios/" + socio.getId()))
                .andExpect(flash().attribute("exito", "socio.actualizado"));
        Socio editado = socios.findById(socio.getId()).orElseThrow();
        assertThat(editado.getCedula()).isEqualTo(cedula);
        assertThat(editado.getNombres()).isEqualTo("Nuevo");
        assertThat(editado.getSector().getId()).isEqualTo(otro.getId());
        mvc.perform(post("/socios/" + socio.getId() + "/estado").with(fabrica.sesion(secretario)).with(csrf())
                        .param("estado", "INACTIVO"))
                .andExpect(flash().attribute("exito", "socio.estadoActualizado"));
        assertThat(socios.findById(socio.getId()).orElseThrow().getEstado()).isEqualTo(EstadoSocio.INACTIVO);
        mvc.perform(get("/socios/" + socio.getId()).with(fabrica.sesion(secretario)))
                .andExpect(content().string(containsString("Inactivo")))
                .andExpect(content().string(containsString("Asignar Predio")));
    }

    @Test
    @DisplayName("HU1-9 ADMIN_JUNTA y TESORERO solo leen")
    void soloLectura() throws Exception {
        Socio socio = fabrica.socio(junta, sector);
        for (Rol rol : new Rol[] {Rol.ADMIN_JUNTA, Rol.TESORERO}) {
            Usuario lector = fabrica.usuario(rol, junta);
            mvc.perform(get("/socios").with(fabrica.sesion(lector)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(socio.getNombreCompleto())))
                    .andExpect(content().string(not(containsString("Añadir Nuevo Socio"))));
            mvc.perform(get("/socios/" + socio.getId()).with(fabrica.sesion(lector)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("Pasar a INACTIVO"))));
            mvc.perform(get("/socios/nuevo").with(fabrica.sesion(lector))).andExpect(status().isForbidden());
            mvc.perform(post("/socios").with(fabrica.sesion(lector)).with(csrf())).andExpect(status().isForbidden());
            mvc.perform(post("/socios/" + socio.getId() + "/estado").with(fabrica.sesion(lector)).with(csrf())
                    .param("estado", "INACTIVO")).andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("Aislamiento: un socio de otra junta responde 404")
    void otraJunta() throws Exception {
        Junta otra = fabrica.junta("Junta Ajena");
        Socio ajeno = fabrica.socio(otra, fabrica.sector(otra));
        mvc.perform(get("/socios/" + ajeno.getId()).with(fabrica.sesion(secretario))).andExpect(status().isNotFound());
        mvc.perform(post("/socios/" + ajeno.getId() + "/estado").with(fabrica.sesion(secretario)).with(csrf())
                .param("estado", "INACTIVO")).andExpect(status().isNotFound());
        mvc.perform(get("/socios").with(fabrica.sesion(secretario)))
                .andExpect(content().string(not(containsString(ajeno.getCedula()))));
        assertThat(socios.findById(ajeno.getId()).orElseThrow().getEstado()).isEqualTo(EstadoSocio.ACTIVO);
    }
}
