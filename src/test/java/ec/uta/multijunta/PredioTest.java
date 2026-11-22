package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Predio;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.PredioRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CU07 Registrar predios y localización en mapa")
class PredioTest {

    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private PredioRepository predios;

    private Junta junta;
    private Sector sector;
    private Socio socio;
    private Usuario secretario;

    @BeforeEach
    void preparar() {
        junta = fabrica.junta("Junta Predios");
        sector = fabrica.sector(junta);
        socio = fabrica.socio(junta, sector);
        secretario = fabrica.secretario(junta);
    }

    private MockMultipartHttpServletRequestBuilder nuevo(String area, String latitud, String longitud) {
        MockMultipartHttpServletRequestBuilder solicitud = multipart("/predios");
        solicitud.param("socioId", socio.getId().toString()).param("area", area)
                .param("sectorId", sector.getId().toString());
        if (latitud != null) {
            solicitud.param("latitud", latitud).param("longitud", longitud);
        }
        solicitud.with(fabrica.sesion(secretario)).with(csrf());
        return solicitud;
    }

    @Test
    @DisplayName("HU6-1 y HU6-2 formulario con mapa y guardado con ubicación y foto")
    void guardarConFoto() throws Exception {
        mvc.perform(get("/predios/nuevo").param("socioId", socio.getId().toString()).with(fabrica.sesion(secretario)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("mapaPredio")))
                .andExpect(content().string(containsString("leaflet")))
                .andExpect(content().string(containsString("Guardar Predio")));
        mvc.perform(nuevo("2450.00", "-1.168000", "-78.532000")
                        .file(new MockMultipartFile("foto", "terreno.png", "image/png", PNG)))
                .andExpect(redirectedUrl("/socios/" + socio.getId()))
                .andExpect(flash().attribute("exito", "predio.guardado"));
        Predio predio = predios.findBySocioIdAndJuntaIdOrderByIdAsc(socio.getId(), junta.getId()).get(0);
        assertThat(predio.getCodigo()).startsWith("P-");
        assertThat(predio.getLatitud()).isEqualByComparingTo(new BigDecimal("-1.168"));
        assertThat(predio.getFotoNombre()).endsWith(".png");
        mvc.perform(get("/socios/" + socio.getId()).with(fabrica.sesion(secretario)))
                .andExpect(content().string(containsString("mapa-predio-" + predio.getId())))
                .andExpect(content().string(containsString("/predios/" + predio.getId() + "/foto")));
        mvc.perform(get("/predios/" + predio.getId() + "/foto").with(fabrica.sesion(secretario)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }

    @Test
    @DisplayName("HU6-3 guardar sin foto y agregarla al editar")
    void fotoDespues() throws Exception {
        mvc.perform(nuevo("100", "-1.2", "-78.6")).andExpect(flash().attribute("exito", "predio.guardado"));
        Predio predio = predios.findBySocioIdAndJuntaIdOrderByIdAsc(socio.getId(), junta.getId()).get(0);
        assertThat(predio.getFotoNombre()).isNull();
        MockMultipartHttpServletRequestBuilder edicion = multipart("/predios/" + predio.getId());
        edicion.param("area", "150").param("sectorId", sector.getId().toString()).param("latitud", "-1.2")
                .param("longitud", "-78.6").with(fabrica.sesion(secretario)).with(csrf());
        mvc.perform(edicion.file(new MockMultipartFile("foto", "f.png", "image/png", PNG)))
                .andExpect(flash().attribute("exito", "predio.guardado"));
        Predio editado = predios.findById(predio.getId()).orElseThrow();
        assertThat(editado.getFotoNombre()).isNotNull();
        assertThat(editado.getArea()).isEqualByComparingTo("150");
    }

    @Test
    @DisplayName("HU6-4 y HU6-5 foto grande o de otro tipo, sin ubicación, fuera del Ecuador y área 0")
    void validaciones() throws Exception {
        byte[] grande = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, grande, 0, PNG.length);
        mvc.perform(nuevo("100", "-1.2", "-78.6").file(new MockMultipartFile("foto", "g.png", "image/png", grande)))
                .andExpect(content().string(containsString("El archivo excede el límite permitido")));
        mvc.perform(nuevo("100", "-1.2", "-78.6").file(new MockMultipartFile("foto", "f.gif", "image/gif",
                        "GIF89a".getBytes())))
                .andExpect(content().string(containsString("La foto debe ser JPG o PNG")));
        mvc.perform(nuevo("100", null, null))
                .andExpect(content().string(containsString("Coloque la ubicación del predio en el mapa")));
        mvc.perform(nuevo("100", "40.4", "-3.7"))
                .andExpect(content().string(containsString("La ubicación debe estar dentro del Ecuador")));
        mvc.perform(nuevo("0", "-1.2", "-78.6"))
                .andExpect(content().string(containsString("El área debe ser mayor que 0")));
        mvc.perform(nuevo("100", "-0.74", "-90.31")).andExpect(flash().attribute("exito", "predio.guardado"));
        assertThat(predios.findBySocioIdAndJuntaIdOrderByIdAsc(socio.getId(), junta.getId())).hasSize(1);
    }

    @Test
    @DisplayName("HU6-6 lectura: ADMIN_JUNTA ve predios sin editar; TESORERO no ve la sección")
    void lectura() throws Exception {
        Predio predio = fabrica.predio(socio, sector);
        Usuario admin = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        mvc.perform(get("/socios/" + socio.getId()).with(fabrica.sesion(admin)))
                .andExpect(content().string(containsString(predio.getCodigo())))
                .andExpect(content().string(not(containsString("Asignar Predio"))))
                .andExpect(content().string(not(containsString("Editar predio"))));
        mvc.perform(get("/predios/nuevo").param("socioId", socio.getId().toString()).with(fabrica.sesion(admin)))
                .andExpect(status().isForbidden());
        Usuario tesorero = fabrica.usuario(Rol.TESORERO, junta);
        mvc.perform(get("/socios/" + socio.getId()).with(fabrica.sesion(tesorero)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(predio.getCodigo()))));
    }

    @Test
    @DisplayName("Aislamiento: socio, predio y foto de otra junta responden 404")
    void otraJunta() throws Exception {
        Junta otra = fabrica.junta("Junta Ajena Predios");
        Sector sectorAjeno = fabrica.sector(otra);
        Socio ajeno = fabrica.socio(otra, sectorAjeno);
        Predio predioAjeno = fabrica.predio(ajeno, sectorAjeno);
        mvc.perform(get("/predios/nuevo").param("socioId", ajeno.getId().toString()).with(fabrica.sesion(secretario)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/predios/" + predioAjeno.getId() + "/editar").with(fabrica.sesion(secretario)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/predios/" + predioAjeno.getId() + "/foto").with(fabrica.sesion(secretario)))
                .andExpect(status().isNotFound());
        MockMultipartHttpServletRequestBuilder solicitud = multipart("/predios");
        solicitud.param("socioId", ajeno.getId().toString()).param("area", "10")
                .param("sectorId", sector.getId().toString()).param("latitud", "-1.2").param("longitud", "-78.6")
                .with(fabrica.sesion(secretario)).with(csrf());
        mvc.perform(solicitud).andExpect(status().isNotFound());
        assertThat(predios.findBySocioIdAndJuntaIdOrderByIdAsc(ajeno.getId(), otra.getId())).hasSize(1);
    }
}
