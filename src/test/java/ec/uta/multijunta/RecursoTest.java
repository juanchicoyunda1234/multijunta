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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Recurso;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.model.TipoRecurso;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.RecursoRepository;
import java.time.LocalDate;
import java.time.LocalTime;
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
@DisplayName("CU27 Gestionar recursos de riego")
class RecursoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private RecursoRepository recursos;

    private Junta junta;
    private Sector sector;
    private Usuario secretario;

    @BeforeEach
    void preparar() {
        junta = fabrica.junta("Junta Recursos");
        sector = fabrica.sector(junta);
        secretario = fabrica.secretario(junta);
    }

    private ResultActions crear(String nombre, String caudal) throws Exception {
        return mvc.perform(post("/recursos").with(fabrica.sesion(secretario)).with(csrf())
                .param("nombre", nombre).param("tipo", "COMPUERTA").param("sectorId", sector.getId().toString())
                .param("caudal", caudal));
    }

    @Test
    @DisplayName("HU4-1 y HU4-2 listar y crear recursos activos")
    void crearYListar() throws Exception {
        crear("Compuerta #3", "30").andExpect(flash().attribute("exito", "recurso.guardado"));
        Recurso recurso = recursos.findByJuntaIdOrderByNombreAsc(junta.getId()).get(0);
        assertThat(recurso.isActivo()).isTrue();
        assertThat(recurso.getTipo()).isEqualTo(TipoRecurso.COMPUERTA);
        mvc.perform(get("/recursos").with(fabrica.sesion(secretario)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Compuerta #3")))
                .andExpect(content().string(containsString("Recursos")));
    }

    @Test
    @DisplayName("HU4-3 nombre repetido sin distinguir mayúsculas y caudal no positivo")
    void validaciones() throws Exception {
        fabrica.recurso(junta, sector, "Compuerta #9");
        crear("compuerta #9", "10")
                .andExpect(model().attribute("abrir", "nuevo"))
                .andExpect(content().string(containsString("Ya existe un recurso con ese nombre")));
        crear("Canal Alto", "0").andExpect(content().string(containsString("El caudal debe ser mayor que 0")));
        crear("Canal Bajo", "-5").andExpect(content().string(containsString("El caudal debe ser mayor que 0")));
        assertThat(recursos.findByJuntaIdOrderByNombreAsc(junta.getId())).hasSize(1);
        Junta otra = fabrica.junta("Junta Otra Recursos");
        fabrica.recurso(otra, fabrica.sector(otra), "Reservorio Norte");
        crear("Reservorio Norte", "50").andExpect(flash().attribute("exito", "recurso.guardado"));
    }

    @Test
    @DisplayName("HU4-4 desactivar y reactivar; los turnos se conservan")
    void desactivarYReactivar() throws Exception {
        Recurso recurso = fabrica.recurso(junta, sector, "Compuerta #1");
        LocalDate dia = Fabrica.proximoDiaLaboral(LocalDate.now().minusDays(7));
        fabrica.turno(junta, sector, recurso, dia, LocalTime.of(8, 0), LocalTime.of(9, 0));
        mvc.perform(post("/recursos/" + recurso.getId() + "/desactivar").with(fabrica.sesion(secretario)).with(csrf()))
                .andExpect(flash().attribute("exito", "recurso.desactivado"));
        assertThat(recursos.findByJuntaIdAndActivoTrueOrderByNombreAsc(junta.getId())).isEmpty();
        mvc.perform(get("/turnos").param("semana", dia.toString()).with(fabrica.sesion(secretario)))
                .andExpect(content().string(containsString("Compuerta #1")));
        mvc.perform(post("/recursos/" + recurso.getId() + "/reactivar").with(fabrica.sesion(secretario)).with(csrf()))
                .andExpect(flash().attribute("exito", "recurso.reactivado"));
        assertThat(recursos.findById(recurso.getId()).orElseThrow().isActivo()).isTrue();
    }

    @Test
    @DisplayName("Permisos y aislamiento")
    void permisosYAislamiento() throws Exception {
        Recurso recurso = fabrica.recurso(junta, sector, "Compuerta #2");
        Usuario admin = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        mvc.perform(get("/recursos").with(fabrica.sesion(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("data-bs-target=\"#modalNuevo\""))));
        mvc.perform(post("/recursos/" + recurso.getId() + "/desactivar").with(fabrica.sesion(admin)).with(csrf()))
                .andExpect(status().isForbidden());
        Junta otra = fabrica.junta("Junta Lejana Recursos");
        Sector sectorAjeno = fabrica.sector(otra);
        Recurso ajeno = fabrica.recurso(otra, sectorAjeno, "Ajeno");
        mvc.perform(post("/recursos/" + ajeno.getId() + "/desactivar").with(fabrica.sesion(secretario)).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/recursos").with(fabrica.sesion(secretario)).with(csrf())
                        .param("nombre", "Con sector ajeno").param("tipo", "CANAL")
                        .param("sectorId", sectorAjeno.getId().toString()).param("caudal", "5"))
                .andExpect(status().isNotFound());
        assertThat(recursos.findById(ajeno.getId()).orElseThrow().isActivo()).isTrue();
    }
}
