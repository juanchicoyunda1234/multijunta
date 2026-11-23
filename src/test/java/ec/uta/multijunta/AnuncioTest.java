package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.uta.multijunta.model.Acta;
import ec.uta.multijunta.model.Convocatoria;
import ec.uta.multijunta.model.EstadoConvocatoria;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.TipoConvocatoria;
import ec.uta.multijunta.repository.ConvocatoriaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
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
@DisplayName("CU32 Consultar anuncios y repositorio")
class AnuncioTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private ConvocatoriaRepository convocatorias;

    private Junta junta;
    private Socio socio;

    @BeforeEach
    void preparar() {
        junta = fabrica.junta("Junta Anuncios");
        socio = fabrica.socio(junta, fabrica.sector(junta));
    }

    @Test
    @DisplayName("HU3-3 sin contenido muestra 'No hay publicaciones por ahora'")
    void vacio() throws Exception {
        mvc.perform(get("/anuncios").with(fabrica.sesion(socio)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No hay publicaciones por ahora")));
        mvc.perform(get("/anuncios/repositorio").with(fabrica.sesion(socio)))
                .andExpect(content().string(containsString("No hay publicaciones por ahora")));
    }

    @Test
    @DisplayName("HU3-1 y HU3-4 anuncios con Asamblea futura fijada arriba y sin borradores")
    void anuncios() throws Exception {
        Convocatoria minga = fabrica.convocatoria(junta, TipoConvocatoria.MINGA, LocalDate.now().plusDays(2),
                EstadoConvocatoria.PUBLICADA);
        Convocatoria asamblea = fabrica.convocatoria(junta, TipoConvocatoria.ASAMBLEA, LocalDate.now().plusDays(15),
                EstadoConvocatoria.PUBLICADA);
        asamblea.setFechaPublicacion(LocalDateTime.now().minusDays(5));
        convocatorias.save(asamblea);
        Convocatoria pasada = fabrica.convocatoria(junta, TipoConvocatoria.ASAMBLEA, LocalDate.now().minusDays(3),
                EstadoConvocatoria.PUBLICADA);
        pasada.setFechaPublicacion(LocalDateTime.now().minusDays(10));
        convocatorias.save(pasada);
        Convocatoria borrador = fabrica.convocatoria(junta, TipoConvocatoria.MINGA, LocalDate.now().plusDays(4),
                EstadoConvocatoria.BORRADOR);
        Junta otra = fabrica.junta("Junta Ajena Anuncios");
        Convocatoria ajena = fabrica.convocatoria(otra, TipoConvocatoria.MINGA, LocalDate.now().plusDays(4),
                EstadoConvocatoria.PUBLICADA);
        String html = mvc.perform(get("/anuncios").with(fabrica.sesion(socio)))
                .andExpect(content().string(containsString("Orden del día")))
                .andExpect(content().string(not(containsString(borrador.getTitulo()))))
                .andExpect(content().string(not(containsString(ajena.getTitulo()))))
                .andReturn().getResponse().getContentAsString();
        assertThat(html.indexOf(asamblea.getTitulo())).isLessThan(html.indexOf(minga.getTitulo()));
        assertThat(html.indexOf(minga.getTitulo())).isLessThan(html.indexOf(pasada.getTitulo()));
    }

    @Test
    @DisplayName("HU3-2 y HU3-4 repositorio con actas públicas descargables; las privadas no existen para el socio")
    void repositorio() throws Exception {
        Acta publica = fabrica.acta(junta, "Acta de Asamblea Ordinaria", true);
        Acta privada = fabrica.acta(junta, "Acta reservada de directiva", false);
        Acta ajena = fabrica.acta(fabrica.junta("Junta Ajena Repositorio"), "Acta de otra junta", true);
        mvc.perform(get("/anuncios/repositorio").with(fabrica.sesion(socio)))
                .andExpect(content().string(containsString("Acta de Asamblea Ordinaria")))
                .andExpect(content().string(not(containsString("Acta reservada de directiva"))))
                .andExpect(content().string(not(containsString("Acta de otra junta"))));
        mvc.perform(get("/anuncios/repositorio/" + publica.getId() + "/documento").with(fabrica.sesion(socio)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
        mvc.perform(get("/anuncios/repositorio/" + privada.getId() + "/documento").with(fabrica.sesion(socio)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/anuncios/repositorio/" + ajena.getId() + "/documento").with(fabrica.sesion(socio)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("HU3-5 solo lectura para el socio; la directiva no entra")
    void permisos() throws Exception {
        Acta publica = fabrica.acta(junta, "Acta pública", true);
        mvc.perform(post("/anuncios").with(fabrica.sesion(socio)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/anuncios/repositorio/" + publica.getId() + "/documento").with(fabrica.sesion(socio)).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/anuncios").with(fabrica.sesion(fabrica.usuario(Rol.SECRETARIO, junta))))
                .andExpect(status().isForbidden());
    }
}
