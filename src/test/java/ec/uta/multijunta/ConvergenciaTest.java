package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;

import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.UsuarioRepository;
import ec.uta.multijunta.service.ReglaNegocioException;
import ec.uta.multijunta.service.SocioService;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDate;
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
@DisplayName("Convergencia: reglas del modelo y barra superior")
class ConvergenciaTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private SocioService socioService;

    @Autowired
    private UsuarioRepository usuarios;

    private Socio nuevoSocio(Junta junta, Sector sector, String cedula, String codigo) {
        Socio socio = new Socio();
        socio.setCedula(cedula);
        socio.setUsername(cedula);
        socio.setNombres("Nombre");
        socio.setApellidos("Apellido");
        socio.setPassword("x");
        socio.setJunta(junta);
        socio.setSector(sector);
        socio.setCodigoSocio(codigo);
        socio.setFechaIngreso(LocalDate.of(2022, 5, 1));
        return socio;
    }

    @Test
    @DisplayName("data-model Socio.codigoSocio es único dentro de la junta y se repite entre juntas")
    void codigoSocioUnicoPorJunta() {
        Junta juntaA = fabrica.junta("Junta Código A");
        Junta juntaB = fabrica.junta("Junta Código B");
        socioService.registrar(nuevoSocio(juntaA, fabrica.sector(juntaA), "1900000001", "C-001"));
        assertThatThrownBy(() -> socioService.registrar(nuevoSocio(juntaA, fabrica.sector(juntaA), "1900000002", "C-001")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("socio.codigoDuplicado");
        assertThat(socioService.registrar(nuevoSocio(juntaB, fabrica.sector(juntaB), "1900000003", "C-001")).getId())
                .isNotNull();
    }

    @Test
    @DisplayName("FR-012 todo usuario que no es Super Administrador pertenece a una junta")
    void juntaObligatoria() {
        Usuario sinJunta = new Usuario();
        sinJunta.setCedula("1900000010");
        sinJunta.setUsername("sinjunta");
        sinJunta.setNombres("Sin");
        sinJunta.setApellidos("Junta");
        sinJunta.setPassword("x");
        sinJunta.setRol(Rol.TESORERO);
        assertThatThrownBy(() -> usuarios.saveAndFlush(sinJunta)).isInstanceOf(ConstraintViolationException.class);
        assertThat(fabrica.superAdmin().getJunta()).isNull();
    }

    @Test
    @DisplayName("FR-019 la barra superior muestra nombre, rol y junta también en celular")
    void barraEnCelular() throws Exception {
        Junta junta = fabrica.junta("Junta Barra Móvil");
        Usuario tesorero = fabrica.usuario(Rol.TESORERO, junta);
        mvc.perform(get("/panel").with(fabrica.sesion(tesorero)))
                .andExpect(content().string(containsString("id=\"datosSesionMovil\"")))
                .andExpect(content().string(containsString(" · Junta Barra Móvil")));
    }

    @Test
    @DisplayName("Contrato rutas-ui: GET / es alias de /inicio")
    void raizEsAliasDeInicio() throws Exception {
        mvc.perform(get("/")).andExpect(redirectedUrlPattern("**/login"));
        Usuario socio = fabrica.usuario(Rol.SOCIO, fabrica.junta("Junta Raíz"));
        mvc.perform(get("/").with(fabrica.sesion(socio))).andExpect(redirectedUrl("/mi-informacion"));
    }
}
