package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.uta.multijunta.model.EstadoSocio;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.JuntaRepository;
import ec.uta.multijunta.repository.SectorRepository;
import java.math.BigDecimal;
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
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CU03 Configurar datos generales y sectores")
class ConfiguracionJuntaTest {

    private static final String RUTA = "/junta/configuracion";
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private JuntaRepository juntas;

    @Autowired
    private SectorRepository sectores;

    private Junta junta;
    private Usuario admin;

    @BeforeEach
    void preparar() {
        junta = fabrica.junta("Junta Configurable");
        admin = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
    }

    private MockMultipartHttpServletRequestBuilder guardar() {
        MockMultipartHttpServletRequestBuilder solicitud = multipart(RUTA);
        solicitud.with(fabrica.sesion(admin)).with(csrf());
        return solicitud;
    }

    @Test
    @DisplayName("HU3-1 pestañas, campos de solo lectura y aviso sin sectores")
    void formulario() throws Exception {
        mvc.perform(get(RUTA).with(fabrica.sesion(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Información General")))
                .andExpect(content().string(containsString("Ubicación")))
                .andExpect(content().string(containsString("Sectores")))
                .andExpect(content().string(containsString("+ Añadir")))
                .andExpect(content().string(containsString("Actualizar Datos")))
                .andExpect(content().string(containsString(junta.getRuc())))
                .andExpect(content().string(containsString("Registre al menos un sector para poder inscribir socios")));
    }

    @Test
    @DisplayName("HU3-3 y HU3-5 guarda datos y sectores nuevos")
    void guardadoCompleto() throws Exception {
        mvc.perform(guardar().param("telefono", "+593 3 2800-000").param("direccion", "Av. Principal 123")
                        .param("montoMultaInasistencia", "7.50")
                        .param("sectoresNuevos", "Sector Este", "Ramal San Isidro"))
                .andExpect(redirectedUrl(RUTA))
                .andExpect(flash().attribute("exito", "configuracion.actualizada"));
        Junta guardada = juntas.findById(junta.getId()).orElseThrow();
        assertThat(guardada.getDireccion()).isEqualTo("Av. Principal 123");
        assertThat(guardada.getTelefono()).isEqualTo("+593 3 2800-000");
        assertThat(guardada.getMontoMultaInasistencia()).isEqualByComparingTo(new BigDecimal("7.50"));
        assertThat(sectores.findAllByJuntaIdOrderByNombreAsc(junta.getId())).extracting(Sector::getNombre)
                .containsExactly("Ramal San Isidro", "Sector Este");
        mvc.perform(get(RUTA).with(fabrica.sesion(admin)).flashAttr("exito", "configuracion.actualizada"))
                .andExpect(content().string(containsString("Datos de la junta actualizados")));
    }

    @Test
    @DisplayName("HU3-4 dirección vacía: pestaña Ubicación, foco y mensaje, sin guardar")
    void direccionObligatoria() throws Exception {
        mvc.perform(guardar().param("telefono", "032000000").param("direccion", " ")
                        .param("sectoresNuevos", "Sector Nuevo"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pestana", "ubicacion"))
                .andExpect(model().attribute("foco", "direccion"))
                .andExpect(content().string(containsString("La dirección es obligatoria")))
                .andExpect(content().string(containsString("Sector Nuevo (nuevo)")));
        assertThat(juntas.findById(junta.getId()).orElseThrow().getTelefono()).isNull();
        assertThat(sectores.findAllByJuntaId(junta.getId())).isEmpty();
    }

    @Test
    @DisplayName("HU3-5 sector vacío o duplicado sin distinguir mayúsculas")
    void sectoresInvalidos() throws Exception {
        Sector existente = fabrica.sector(junta);
        mvc.perform(guardar().param("direccion", "Calle 1").param("sectoresNuevos", existente.getNombre().toUpperCase()))
                .andExpect(model().attribute("pestana", "sectores"))
                .andExpect(content().string(containsString("Ese sector ya existe")));
        mvc.perform(guardar().param("direccion", "Calle 1").param("sectoresNuevos", "Uno", "uno "))
                .andExpect(content().string(containsString("Ese sector ya existe")));
        mvc.perform(guardar().param("direccion", "Calle 1").param("sectoresNuevos", "  "))
                .andExpect(content().string(containsString("Escriba el nombre del sector")));
        mvc.perform(guardar().param("direccion", "Calle 1").param("sectoresNuevos", "S".repeat(81)))
                .andExpect(content().string(containsString("El nombre del sector admite hasta 80 caracteres")));
        assertThat(sectores.findAllByJuntaId(junta.getId())).hasSize(1);
    }

    @Test
    @DisplayName("HU3-6 no se quita un sector con socios; sí uno sin socios")
    void quitarSectores() throws Exception {
        Sector conSocios = fabrica.sector(junta);
        fabrica.socioSinCuenta(junta, conSocios, EstadoSocio.ACTIVO);
        Sector libre = fabrica.sector(junta);
        mvc.perform(guardar().param("direccion", "Calle 1").param("sectoresQuitados", conSocios.getId().toString()))
                .andExpect(content().string(containsString("No se puede quitar un sector con socios asignados")));
        mvc.perform(guardar().param("direccion", "Calle 1").param("sectoresQuitados", libre.getId().toString()))
                .andExpect(redirectedUrl(RUTA));
        assertThat(sectores.findAllByJuntaId(junta.getId())).extracting(Sector::getId).containsExactly(conSocios.getId());
    }

    @Test
    @DisplayName("HU3-7 logo PNG aceptado y servido; archivo no imagen o mayor de 5 MB rechazado")
    void logo() throws Exception {
        mvc.perform(guardar().file(new MockMultipartFile("logo", "logo.png", "image/png", PNG))
                        .param("direccion", "Calle 1"))
                .andExpect(redirectedUrl(RUTA));
        assertThat(juntas.findById(junta.getId()).orElseThrow().getLogoUrl()).endsWith(".png");
        mvc.perform(get("/logo/" + junta.getId()).with(fabrica.sesion(admin)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
        mvc.perform(get("/panel").with(fabrica.sesion(admin)))
                .andExpect(content().string(containsString("id=\"logoJunta\"")));

        mvc.perform(guardar().file(new MockMultipartFile("logo", "logo.png", "image/png", "no es imagen".getBytes()))
                        .param("direccion", "Calle 1"))
                .andExpect(model().attribute("pestana", "general"))
                .andExpect(content().string(containsString("El logo debe ser una imagen PNG o JPG de hasta 5 MB")));
        byte[] grande = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, grande, 0, PNG.length);
        mvc.perform(guardar().file(new MockMultipartFile("logo", "grande.png", "image/png", grande))
                        .param("direccion", "Calle 1"))
                .andExpect(content().string(containsString("El logo debe ser una imagen PNG o JPG de hasta 5 MB")));
    }

    @Test
    @DisplayName("HU3-8 monto negativo o con más de 2 decimales")
    void montoInvalido() throws Exception {
        for (String monto : List.of("-1", "2.345", "abc")) {
            mvc.perform(guardar().param("direccion", "Calle 1").param("montoMultaInasistencia", monto))
                    .andExpect(model().attribute("pestana", "general"))
                    .andExpect(content().string(containsString("Ingrese un monto válido (0 o mayor, hasta 2 decimales)")));
        }
    }

    @Test
    @DisplayName("Aislamiento: solo cambia la propia junta; logo y sectores de otra junta no se tocan")
    void aislamiento() throws Exception {
        Junta otra = fabrica.junta("Junta Vecina");
        Sector sectorAjeno = fabrica.sector(otra);
        mvc.perform(guardar().param("direccion", "Calle 1").param("sectoresQuitados", sectorAjeno.getId().toString()))
                .andExpect(status().isNotFound());
        assertThat(sectores.findById(sectorAjeno.getId())).isPresent();
        mvc.perform(guardar().param("direccion", "Mi dirección")).andExpect(redirectedUrl(RUTA));
        assertThat(juntas.findById(otra.getId()).orElseThrow().getDireccion()).isNull();
        mvc.perform(get("/logo/" + otra.getId()).with(fabrica.sesion(admin))).andExpect(status().isNotFound());
        Usuario secretario = fabrica.usuario(Rol.SECRETARIO, junta);
        mvc.perform(get(RUTA).with(fabrica.sesion(secretario))).andExpect(status().isForbidden());
    }
}
