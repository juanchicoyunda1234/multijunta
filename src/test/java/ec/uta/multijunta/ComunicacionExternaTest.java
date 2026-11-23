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

import ec.uta.multijunta.model.ComunicacionExterna;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.TipoDocumentoExterno;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.ComunicacionExternaRepository;
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
@DisplayName("CU19 Registrar comunicaciones recibidas de otras instituciones")
class ComunicacionExternaTest {

    private static final String PREFIJO = "CE-" + LocalDate.now().getYear() + "-";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private ComunicacionExternaRepository comunicaciones;

    private Junta junta;
    private Usuario secretario;

    @BeforeEach
    void preparar() {
        junta = fabrica.junta("Junta Correspondencia");
        secretario = fabrica.secretario(junta);
    }

    private MockMultipartHttpServletRequestBuilder formulario(Usuario usuario, String remitente, String asunto,
            LocalDate fecha, MockMultipartFile documento) {
        MockMultipartHttpServletRequestBuilder solicitud = multipart("/comunicaciones");
        if (documento != null) {
            solicitud.file(documento);
        }
        solicitud.with(fabrica.sesion(usuario)).with(csrf());
        solicitud.param("remitente", remitente).param("asunto", asunto).param("fecha", fecha.toString())
                .param("tipoDocumento", "OFICIO");
        return solicitud;
    }

    private ResultActions registrar(String remitente, LocalDate fecha, MockMultipartFile documento) throws Exception {
        return mvc.perform(formulario(secretario, remitente, "Inspección de canales", fecha, documento));
    }

    private MockMultipartFile pdf() {
        return new MockMultipartFile("documento", "oficio.pdf", "application/pdf", Fabrica.PDF_MINIMO);
    }

    private List<ComunicacionExterna> deJunta() {
        return comunicaciones.findByJuntaIdOrderByFechaDescIdDesc(junta.getId());
    }

    @Test
    @DisplayName("HU6-1 y HU6-2 registro con documento, número correlativo y orden por fecha de recepción")
    void registrar() throws Exception {
        mvc.perform(get("/comunicaciones").with(fabrica.sesion(secretario)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Registrar Comunicación Recibida")))
                .andExpect(content().string(containsString("Notificación")))
                .andExpect(content().string(containsString("id=\"botonRegistrarComunicacion\"")));
        registrar("Ministerio de Ambiente", LocalDate.now().minusDays(5), pdf())
                .andExpect(flash().attribute("exitoTexto", "Comunicación registrada con el número " + PREFIJO + "0001"));
        registrar("Prefectura de Tungurahua", LocalDate.now().minusDays(1), null)
                .andExpect(flash().attribute("exitoTexto", "Comunicación registrada con el número " + PREFIJO + "0002"));
        List<ComunicacionExterna> lista = deJunta();
        assertThat(lista).extracting(ComunicacionExterna::getRemitente)
                .containsExactly("Prefectura de Tungurahua", "Ministerio de Ambiente");
        assertThat(lista.get(1).getTipoDocumento()).isEqualTo(TipoDocumentoExterno.OFICIO);
        assertThat(lista.get(1).getRegistradoPor().getId()).isEqualTo(secretario.getId());
        mvc.perform(get("/comunicaciones/" + lista.get(1).getId() + "/documento").with(fabrica.sesion(secretario)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("HU6-3 sin documento queda pendiente de digitalización y se adjunta después")
    void pendienteDeDigitalizacion() throws Exception {
        registrar("GAD Parroquial", LocalDate.now(), null);
        ComunicacionExterna comunicacion = deJunta().get(0);
        assertThat(comunicacion.isPendienteDigitalizacion()).isTrue();
        mvc.perform(get("/comunicaciones").with(fabrica.sesion(secretario)))
                .andExpect(content().string(containsString("Pendiente de digitalización")));
        mvc.perform(get("/comunicaciones/" + comunicacion.getId() + "/documento").with(fabrica.sesion(secretario)))
                .andExpect(status().isNotFound());
        mvc.perform(multipart("/comunicaciones/" + comunicacion.getId() + "/documento").file(pdf())
                        .with(fabrica.sesion(secretario)).with(csrf()))
                .andExpect(flash().attribute("exito", "comunicacion.adjuntada"));
        assertThat(comunicaciones.findById(comunicacion.getId()).orElseThrow().isPendienteDigitalizacion()).isFalse();
    }

    @Test
    @DisplayName("HU6-4 y HU6-6 campos obligatorios, fecha futura y archivo inválido")
    void validaciones() throws Exception {
        mvc.perform(formulario(secretario, " ", " ", LocalDate.now(), null))
                .andExpect(content().string(containsString("Este campo es obligatorio")));
        registrar("Ministerio", LocalDate.now().plusDays(1), null)
                .andExpect(content().string(containsString("La fecha de recepción no puede ser futura")));
        registrar("Ministerio", LocalDate.now(), new MockMultipartFile("documento", "a.txt", "text/plain", "x".getBytes()))
                .andExpect(content().string(containsString("Formato no permitido. Use PDF, JPG o PNG")));
        assertThat(deJunta()).isEmpty();
    }

    @Test
    @DisplayName("HU6-5 eliminar no libera el número y cada junta numera por separado")
    void numeracion() throws Exception {
        registrar("Uno", LocalDate.now(), null);
        registrar("Dos", LocalDate.now(), null);
        ComunicacionExterna segunda = deJunta().stream().filter(c -> c.getRemitente().equals("Dos")).findFirst().orElseThrow();
        mvc.perform(post("/comunicaciones/" + segunda.getId() + "/eliminar").with(fabrica.sesion(secretario)).with(csrf()))
                .andExpect(flash().attribute("exito", "comunicacion.eliminada"));
        registrar("Tres", LocalDate.now(), null)
                .andExpect(flash().attribute("exitoTexto", "Comunicación registrada con el número " + PREFIJO + "0003"));
        assertThat(deJunta()).extracting(ComunicacionExterna::getNumeroIngreso)
                .containsExactlyInAnyOrder(PREFIJO + "0001", PREFIJO + "0003");
        Junta otra = fabrica.junta("Junta Ajena Correspondencia");
        mvc.perform(formulario(fabrica.secretario(otra), "Otra", "Asunto", LocalDate.now(), null))
                .andExpect(flash().attribute("exitoTexto", "Comunicación registrada con el número " + PREFIJO + "0001"));
    }

    @Test
    @DisplayName("HU6-7 ADMIN_JUNTA solo lectura; otra junta 404")
    void permisos() throws Exception {
        registrar("Ministerio", LocalDate.now(), pdf());
        ComunicacionExterna comunicacion = deJunta().get(0);
        Usuario admin = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        mvc.perform(get("/comunicaciones").with(fabrica.sesion(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ministerio")))
                .andExpect(content().string(not(containsString("Registrar Comunicación Recibida"))));
        mvc.perform(get("/comunicaciones/" + comunicacion.getId() + "/documento").with(fabrica.sesion(admin)))
                .andExpect(status().isOk());
        mvc.perform(formulario(admin, "X", "Y", LocalDate.now(), null)).andExpect(status().isForbidden());
        mvc.perform(post("/comunicaciones/" + comunicacion.getId() + "/eliminar").with(fabrica.sesion(admin)).with(csrf()))
                .andExpect(status().isForbidden());
        Usuario ajeno = fabrica.secretario(fabrica.junta("Junta Ajena Permisos"));
        mvc.perform(post("/comunicaciones/" + comunicacion.getId() + "/eliminar").with(fabrica.sesion(ajeno)).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/comunicaciones/" + comunicacion.getId() + "/documento").with(fabrica.sesion(ajeno)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/comunicaciones").with(fabrica.sesion(fabrica.socio(junta, fabrica.sector(junta)))))
                .andExpect(status().isForbidden());
    }
}
