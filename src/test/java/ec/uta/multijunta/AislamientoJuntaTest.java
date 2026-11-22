package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.uta.multijunta.config.UsuarioAutenticado;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.SectorRepository;
import ec.uta.multijunta.repository.SocioRepository;
import ec.uta.multijunta.repository.UsuarioRepository;
import ec.uta.multijunta.service.UsuarioActual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Aislamiento de datos por junta (Principio II)")
class AislamientoJuntaTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private SectorRepository sectores;

    @Autowired
    private SocioRepository socios;

    @Autowired
    private UsuarioActual usuarioActual;

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("HU2-4 los repositorios no devuelven registros de otra junta")
    void repositoriosFiltranPorJunta() {
        Junta juntaA = fabrica.junta("Junta A");
        Junta juntaB = fabrica.junta("Junta B");
        Usuario usuarioB = fabrica.usuario(Rol.TESORERO, juntaB);
        Sector sectorB = fabrica.sector(juntaB);
        Usuario socioB = fabrica.usuario(Rol.SOCIO, juntaB);

        assertThat(usuarios.findByIdAndJuntaId(usuarioB.getId(), juntaA.getId())).isEmpty();
        assertThat(sectores.findByIdAndJuntaId(sectorB.getId(), juntaA.getId())).isEmpty();
        assertThat(socios.findByIdAndJuntaId(socioB.getId(), juntaA.getId())).isEmpty();
        assertThat(sectores.findAllByJuntaId(juntaA.getId())).doesNotContain(sectorB);
        assertThat(usuarios.findByIdAndJuntaId(usuarioB.getId(), juntaB.getId())).isPresent();
    }

    @Test
    @DisplayName("FR-015 la junta se toma de la sesión y no de los parámetros")
    void juntaDesdeLaSesion() throws Exception {
        Junta juntaA = fabrica.junta("Junta Sesión A");
        Junta juntaB = fabrica.junta("Junta Sesión B");
        Usuario usuarioA = fabrica.usuario(Rol.ADMIN_JUNTA, juntaA);
        UsuarioAutenticado sesion = UsuarioAutenticado.desde(usuarioA);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(sesion, null, sesion.getAuthorities()));
        assertThat(usuarioActual.juntaId()).isEqualTo(juntaA.getId());

        mvc.perform(get("/recuperacion/solicitudes").param("juntaId", juntaB.getId().toString())
                        .with(fabrica.sesion(usuarioA)))
                .andExpect(status().isOk());
    }
}
