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

import ec.uta.multijunta.model.ActivoFijo;
import ec.uta.multijunta.model.CategoriaActivo;
import ec.uta.multijunta.model.EstadoActivo;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.ActivoFijoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
@DisplayName("CU14 Gestionar activos fijos")
class ActivoFijoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private ActivoFijoRepository activos;

    private Junta junta;
    private Usuario tesorero;

    @BeforeEach
    void preparar() {
        junta = fabrica.junta("Junta Activos");
        tesorero = fabrica.tesorero(junta);
    }

    private MockMultipartHttpServletRequestBuilder formulario(Usuario usuario, String nombre, String categoria,
            String valor, LocalDate fecha) {
        MockMultipartHttpServletRequestBuilder solicitud = multipart("/activos");
        solicitud.with(fabrica.sesion(usuario)).with(csrf());
        solicitud.param("nombre", nombre).param("valorAdquisicion", valor)
                .param("fechaAdquisicion", fecha.toString()).param("estado", "NUEVO");
        if (categoria != null) {
            solicitud.param("categoria", categoria);
        }
        return solicitud;
    }

    private ResultActions registrar(String nombre, String categoria, String valor, LocalDate fecha) throws Exception {
        return mvc.perform(formulario(tesorero, nombre, categoria, valor, fecha));
    }

    private ActivoFijo activo(String nombre) {
        return activos.findByJuntaIdOrderByIdDesc(junta.getId()).stream()
                .filter(a -> a.getNombre().equals(nombre)).findFirst().orElseThrow();
    }

    private static ActivoFijo nuevo(CategoriaActivo categoria, String valor, LocalDate fecha) {
        ActivoFijo activo = new ActivoFijo();
        activo.setCategoria(categoria);
        activo.setValorAdquisicion(new BigDecimal(valor));
        activo.setFechaAdquisicion(fecha);
        return activo;
    }

    @Test
    @DisplayName("HU7-5 depreciación lineal por meses completos con tope en el valor de compra")
    void depreciacion() {
        LocalDate hoy = LocalDate.of(2026, 7, 15);
        assertThat(nuevo(CategoriaActivo.VEHICULOS, "12000", LocalDate.of(2024, 1, 15)).depreciacion(hoy))
                .isEqualByComparingTo("6000.00");
        assertThat(nuevo(CategoriaActivo.INFRAESTRUCTURA, "24000", LocalDate.of(2025, 7, 15)).depreciacion(hoy))
                .isEqualByComparingTo("1200.00");
        assertThat(nuevo(CategoriaActivo.MAQUINARIA, "1450", LocalDate.of(2024, 6, 10)).depreciacion(hoy))
                .isEqualByComparingTo("302.08");
        assertThat(nuevo(CategoriaActivo.EQUIPO_OFICINA, "600", LocalDate.of(2015, 1, 1)).depreciacion(hoy))
                .isEqualByComparingTo("600.00");
        assertThat(nuevo(CategoriaActivo.VEHICULOS, "12000", LocalDate.of(2026, 6, 16)).depreciacion(hoy))
                .isEqualByComparingTo("0");
        ActivoFijo sinCategoria = nuevo(null, "150", LocalDate.of(2020, 1, 1));
        assertThat(sinCategoria.depreciacion(hoy)).isEqualByComparingTo("0");
        assertThat(sinCategoria.valorEnLibros(hoy)).isEqualByComparingTo("150");
    }

    @Test
    @DisplayName("HU7-1 y HU7-2 registrar con código, archivo y patrimonio")
    void registrar() throws Exception {
        mvc.perform(get("/activos").with(fabrica.sesion(tesorero)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Registrar Activo")))
                .andExpect(content().string(containsString("id=\"botonGuardarActivo\"")))
                .andExpect(content().string(containsString("Equipo de oficina")));
        LocalDate hace30Meses = LocalDate.now().minusMonths(30);
        mvc.perform(formulario(tesorero, "Camioneta", "VEHICULOS", "12000", hace30Meses)
                        .file(new MockMultipartFile("archivo", "acta.pdf", "application/pdf", Fabrica.PDF_MINIMO)))
                .andExpect(flash().attribute("exito", "activo.registrado"));
        ActivoFijo camioneta = activo("Camioneta");
        assertThat(camioneta.getCodigo()).isEqualTo("AF-" + LocalDate.now().getYear() + "-0001");
        assertThat(camioneta.getEstado()).isEqualTo(EstadoActivo.NUEVO);
        assertThat(camioneta.getArchivoNombre()).isNotNull();
        registrar("Silla", null, "150", LocalDate.now().minusYears(1)).andExpect(flash().attribute("exito", "activo.registrado"));
        assertThat(activo("Silla").getCodigo()).isEqualTo("AF-" + LocalDate.now().getYear() + "-0002");
        mvc.perform(get("/activos").with(fabrica.sesion(tesorero)))
                .andExpect(content().string(containsString("$6,000.00")))
                .andExpect(content().string(containsString("$6,150.00")));
        mvc.perform(get("/activos/" + camioneta.getId() + "/archivo").with(fabrica.sesion(tesorero)))
                .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));
    }

    @Test
    @DisplayName("HU7-3, HU7-4 y HU7-7 valor, categoría, fecha y archivo inválidos")
    void validaciones() throws Exception {
        LocalDate fecha = LocalDate.now().minusYears(1);
        registrar("Bomba", "MAQUINARIA", "0", fecha)
                .andExpect(content().string(containsString("El valor de compra debe ser mayor que cero")));
        registrar("Bomba", "MAQUINARIA", "-5", fecha)
                .andExpect(content().string(containsString("El valor de compra debe ser mayor que cero")));
        registrar("Bomba", "MAQUINARIA", "100.555", fecha)
                .andExpect(content().string(containsString("El monto admite hasta dos decimales")));
        registrar("Bomba", null, "300", fecha)
                .andExpect(content().string(containsString("Los activos de más de $200 deben tener categoría")));
        registrar("Bomba", "MAQUINARIA", "300", LocalDate.now().plusDays(1))
                .andExpect(content().string(containsString("La fecha de adquisición no puede ser futura")));
        mvc.perform(formulario(tesorero, "Bomba", "MAQUINARIA", "300", fecha)
                        .file(new MockMultipartFile("archivo", "a.txt", "text/plain", "x".getBytes())))
                .andExpect(content().string(containsString("Formato no permitido. Use PDF, JPG o PNG")));
        assertThat(activos.findByJuntaIdOrderByIdDesc(junta.getId())).isEmpty();
    }

    @Test
    @DisplayName("HU7-6 dar de baja con motivo; sale del patrimonio y conserva su historial")
    void darDeBaja() throws Exception {
        registrar("Bomba", "MAQUINARIA", "1000", LocalDate.now());
        registrar("Escritorio", "EQUIPO_OFICINA", "400", LocalDate.now());
        ActivoFijo bomba = activo("Bomba");
        mvc.perform(post("/activos/" + bomba.getId() + "/baja").param("motivo", " ")
                .with(fabrica.sesion(tesorero)).with(csrf())).andExpect(flash().attribute("error", "motivo.obligatorio"));
        mvc.perform(post("/activos/" + bomba.getId() + "/baja").param("motivo", "Daño irreparable")
                .with(fabrica.sesion(tesorero)).with(csrf())).andExpect(flash().attribute("exito", "activo.baja"));
        ActivoFijo baja = activos.findById(bomba.getId()).orElseThrow();
        assertThat(baja.getEstado()).isEqualTo(EstadoActivo.DADO_DE_BAJA);
        assertThat(baja.getMotivoBaja()).isEqualTo("Daño irreparable");
        assertThat(baja.getFechaBaja()).isEqualTo(LocalDate.now());
        mvc.perform(post("/activos/" + bomba.getId() + "/baja").param("motivo", "Otra")
                .with(fabrica.sesion(tesorero)).with(csrf())).andExpect(flash().attribute("error", "activo.yaBaja"));
        mvc.perform(get("/activos").with(fabrica.sesion(tesorero)))
                .andExpect(content().string(containsString("Daño irreparable")))
                .andExpect(content().string(containsString("Patrimonio activo: $400.00")));
    }

    @Test
    @DisplayName("HU7-8 ADMIN_JUNTA solo lectura; otra junta responde 404")
    void permisosYAislamiento() throws Exception {
        registrar("Generador Kohler", "MAQUINARIA", "1000", LocalDate.now());
        ActivoFijo bomba = activo("Generador Kohler");
        Usuario admin = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        mvc.perform(get("/activos").with(fabrica.sesion(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Generador Kohler")))
                .andExpect(content().string(not(containsString("Registrar Activo"))));
        mvc.perform(formulario(admin, "X", "MAQUINARIA", "10", LocalDate.now())).andExpect(status().isForbidden());
        mvc.perform(post("/activos/" + bomba.getId() + "/baja").param("motivo", "x")
                .with(fabrica.sesion(admin)).with(csrf())).andExpect(status().isForbidden());
        Junta otra = fabrica.junta("Junta Ajena Activos");
        Usuario ajeno = fabrica.tesorero(otra);
        mvc.perform(post("/activos/" + bomba.getId() + "/baja").param("motivo", "x")
                .with(fabrica.sesion(ajeno)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/activos/" + bomba.getId() + "/archivo").with(fabrica.sesion(ajeno)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/activos").with(fabrica.sesion(ajeno)))
                .andExpect(content().string(not(containsString("Generador Kohler"))));
    }
}
