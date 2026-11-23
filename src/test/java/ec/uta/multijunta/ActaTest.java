package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.uta.multijunta.model.Acta;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.ActaRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CU16 Archivar y publicar actas")
class ActaTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private ActaRepository actas;

    private Junta junta;
    private Usuario secretario;
    private Socio socio;

    @BeforeEach
    void preparar() {
        junta = fabrica.junta("Junta Actas");
        secretario = fabrica.secretario(junta);
        socio = fabrica.socio(junta, fabrica.sector(junta));
    }

    private MockMultipartHttpServletRequestBuilder formulario(Usuario usuario, String titulo, MockMultipartFile archivo,
            boolean publica, LocalDate fecha) {
        MockMultipartHttpServletRequestBuilder solicitud = multipart("/actas");
        if (archivo != null) {
            solicitud.file(archivo);
        }
        solicitud.with(fabrica.sesion(usuario)).with(csrf());
        solicitud.param("titulo", titulo).param("fecha", fecha.toString()).param("tipoEvento", "ASAMBLEA")
                .param("contenido", "Fijación de tarifa anual").param("publicaSocios", String.valueOf(publica));
        return solicitud;
    }

    private ResultActions subir(String titulo, boolean publica) throws Exception {
        return mvc.perform(formulario(secretario, titulo, pdf(), publica, LocalDate.now().minusDays(1)));
    }

    private MockMultipartFile pdf() {
        return new MockMultipartFile("documento", "acta.pdf", "application/pdf", Fabrica.PDF_MINIMO);
    }

    private List<Acta> deJunta() {
        return actas.findByJuntaIdOrderByFechaDescIdDesc(junta.getId());
    }

    @Test
    @DisplayName("HU5-1, HU5-2 y HU5-3 subir actas pública y privada; el socio solo ve la pública")
    void subirActas() throws Exception {
        mvc.perform(get("/actas").with(fabrica.sesion(secretario)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Subir Documento")))
                .andExpect(content().string(containsString("Público en Portal Socios")))
                .andExpect(content().string(containsString("Guardar en Archivo")));
        subir("Acta de Asamblea Nº 04", true).andExpect(flash().attribute("exito", "acta.guardada"));
        subir("Acta de directiva", false).andExpect(flash().attribute("exito", "acta.guardada"));
        assertThat(deJunta()).hasSize(2).allSatisfy(acta -> assertThat(acta.getDocumentoNombre()).endsWith(".pdf"));
        mvc.perform(get("/actas").with(fabrica.sesion(secretario)))
                .andExpect(content().string(containsString("Acta de Asamblea Nº 04")))
                .andExpect(content().string(containsString("Acta de directiva")));
        mvc.perform(get("/anuncios/repositorio").with(fabrica.sesion(socio)))
                .andExpect(content().string(containsString("Acta de Asamblea Nº 04")))
                .andExpect(content().string(not(containsString("Acta de directiva"))));
    }

    @Test
    @DisplayName("HU5-4 solo PDF de hasta 5 MB y fecha no futura")
    void validaciones() throws Exception {
        mvc.perform(formulario(secretario, "Acta en imagen",
                        new MockMultipartFile("documento", "acta.png", "image/png", Fabrica.PNG_MINIMO), true,
                        LocalDate.now()))
                .andExpect(content().string(containsString("Formato no válido. Solo se admiten archivos PDF por seguridad.")));
        mvc.perform(formulario(secretario, "Acta sin archivo", null, true, LocalDate.now()))
                .andExpect(content().string(containsString("Formato no válido. Solo se admiten archivos PDF por seguridad.")));
        mvc.perform(formulario(secretario, "Acta futura", pdf(), true, LocalDate.now().plusDays(1)))
                .andExpect(content().string(containsString("La fecha de reunión no puede ser futura")));
        assertThat(deJunta()).isEmpty();
    }

    @Test
    @DisplayName("HU5-5 y FR-022 cambiar visibilidad; ninguna acta se elimina")
    void visibilidadYSinBorrado() throws Exception {
        Acta acta = fabrica.acta(junta, "Acta pública", true);
        mvc.perform(post("/actas/" + acta.getId() + "/visibilidad").param("publicaSocios", "false")
                .with(fabrica.sesion(secretario)).with(csrf())).andExpect(flash().attribute("exito", "acta.visibilidad"));
        assertThat(actas.findById(acta.getId()).orElseThrow().isPublicaSocios()).isFalse();
        mvc.perform(get("/anuncios/repositorio").with(fabrica.sesion(socio)))
                .andExpect(content().string(not(containsString("Acta pública"))));
        int borrar = mvc.perform(post("/actas/" + acta.getId() + "/eliminar").with(fabrica.sesion(secretario)).with(csrf()))
                .andReturn().getResponse().getStatus();
        assertThat(borrar).isIn(404, 405);
        assertThat(actas.findById(acta.getId())).isPresent();
    }

    @Test
    @DisplayName("HU5-6 y HU5-7 socio sin acceso; ADMIN_JUNTA y TESORERO solo lectura; otra junta 404")
    void permisos() throws Exception {
        Acta acta = fabrica.acta(junta, "Acta de lectura", false);
        mvc.perform(formulario(socio, "Intento", pdf(), true, LocalDate.now())).andExpect(status().isForbidden());
        mvc.perform(post("/actas/" + acta.getId() + "/visibilidad").param("publicaSocios", "true")
                .with(fabrica.sesion(socio)).with(csrf())).andExpect(status().isForbidden());
        for (Rol rol : new Rol[] {Rol.ADMIN_JUNTA, Rol.TESORERO}) {
            Usuario lector = fabrica.usuario(rol, junta);
            mvc.perform(get("/actas").with(fabrica.sesion(lector)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Acta de lectura")))
                    .andExpect(content().string(not(containsString("Subir Documento"))));
            mvc.perform(get("/actas/" + acta.getId() + "/documento").with(fabrica.sesion(lector)))
                    .andExpect(status().isOk());
            mvc.perform(formulario(lector, "Intento", pdf(), true, LocalDate.now())).andExpect(status().isForbidden());
        }
        Usuario ajeno = fabrica.secretario(fabrica.junta("Junta Ajena Actas"));
        mvc.perform(get("/actas/" + acta.getId() + "/documento").with(fabrica.sesion(ajeno))).andExpect(status().isNotFound());
        mvc.perform(post("/actas/" + acta.getId() + "/visibilidad").param("publicaSocios", "true")
                .with(fabrica.sesion(ajeno)).with(csrf())).andExpect(status().isNotFound());
    }
}
