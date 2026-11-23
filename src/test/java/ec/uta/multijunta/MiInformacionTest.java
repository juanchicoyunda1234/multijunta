package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.uta.multijunta.model.Cuota;
import ec.uta.multijunta.model.EstadoConvocatoria;
import ec.uta.multijunta.model.EstadoSocio;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Multa;
import ec.uta.multijunta.model.Predio;
import ec.uta.multijunta.model.Recurso;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.TipoConvocatoria;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.PagoRepository;
import ec.uta.multijunta.repository.PredioRepository;
import ec.uta.multijunta.service.ArchivoService;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CU17 Consultar información individual")
class MiInformacionTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private PagoRepository pagos;

    @Autowired
    private PredioRepository predios;

    @Autowired
    private ArchivoService archivos;

    private Junta junta;
    private Sector norte;
    private Sector sur;
    private Socio socio;
    private Socio vecino;
    private Usuario tesorero;

    @BeforeEach
    void preparar() {
        junta = fabrica.junta("Junta Mi Información");
        norte = fabrica.sector(junta);
        sur = fabrica.sector(junta);
        socio = fabrica.socio(junta, norte);
        vecino = fabrica.socio(junta, sur);
        tesorero = fabrica.tesorero(junta);
    }

    private void cobrar(Socio deudor, String clave, String monto) throws Exception {
        mvc.perform(post("/pagos/cobrar/" + deudor.getId()).with(fabrica.sesion(tesorero)).with(csrf())
                .param("obligaciones", clave).param("monto", monto).param("metodo", "EFECTIVO")
                .param("confirmarParcial", "true"));
    }

    @Test
    @DisplayName("HU1-1 y HU1-2 el socio llega a Mi Panel con sus pestañas, ficha, deuda, turno y convocatorias")
    void resumen() throws Exception {
        Recurso recurso = fabrica.recurso(junta, norte, "Compuerta Mi Panel");
        LocalDate dia = Fabrica.proximoDiaLaboral(LocalDate.now().plusDays(2));
        fabrica.turno(junta, norte, recurso, dia, LocalTime.of(8, 0), LocalTime.of(10, 0));
        fabrica.convocatoria(junta, TipoConvocatoria.MINGA, LocalDate.now().plusDays(3), EstadoConvocatoria.PUBLICADA);
        fabrica.convocatoria(junta, TipoConvocatoria.ASAMBLEA, LocalDate.now().plusDays(9), EstadoConvocatoria.PUBLICADA);
        fabrica.convocatoria(junta, TipoConvocatoria.ASAMBLEA, LocalDate.now().plusDays(4), EstadoConvocatoria.BORRADOR);
        fabrica.cuota(socio, "Tarifa anual", "12.00", LocalDate.now().minusDays(1));
        String html = mvc.perform(get("/mi-informacion").with(fabrica.sesion(socio)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(socio.getNombreCompleto())))
                .andExpect(content().string(containsString(socio.getCedula())))
                .andExpect(content().string(containsString(norte.getNombre())))
                .andExpect(content().string(containsString("$12.00")))
                .andExpect(content().string(containsString("Compuerta Mi Panel")))
                .andReturn().getResponse().getContentAsString();
        for (String pestana : new String[] {"Resumen", "Estado de Cuenta", "Mi Predio", "Calendario", "Anuncios",
                "Repositorio", "Mensajes"}) {
            assertThat(html).contains(pestana);
        }
        assertThat(html.indexOf("Asamblea")).isLessThan(html.indexOf("Minga"));
        assertThat(html).doesNotContain("Borrador");
    }

    @Test
    @DisplayName("HU1-3 estado de cuenta en rojo con deuda, detalle, últimos pagos con reverso e impresión")
    void estadoDeCuentaConDeuda() throws Exception {
        Cuota cuota = fabrica.cuota(socio, "Tarifa anual", "25.00", LocalDate.now().minusDays(5));
        Multa multa = fabrica.multa(socio, "5.00");
        cobrar(socio, multa.getClave(), "5.00");
        cobrar(socio, cuota.getClave(), "10.00");
        Long reversado = pagos.buscar(junta.getId(), "TX-000002").get(0).getId();
        mvc.perform(post("/pagos/" + reversado + "/reversar").with(fabrica.sesion(tesorero)).with(csrf())
                .param("motivo", "Error"));
        mvc.perform(get("/mi-informacion/estado-cuenta").with(fabrica.sesion(socio)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("deuda-pendiente")))
                .andExpect(content().string(containsString("$25.00")))
                .andExpect(content().string(containsString("Tarifa anual")))
                .andExpect(content().string(containsString("TX-000001")))
                .andExpect(content().string(containsString("TX-000002")))
                .andExpect(content().string(containsString("Reversado")))
                .andExpect(content().string(containsString("Imprimir estado de cuenta")));
    }

    @Test
    @DisplayName("HU1-3 estado de cuenta en verde sin deuda y solo con los últimos 10 pagos")
    void estadoDeCuentaAlDia() throws Exception {
        for (int indice = 1; indice <= 11; indice++) {
            Multa multa = fabrica.multa(socio, "1.00");
            cobrar(socio, multa.getClave(), "1.00");
        }
        String html = mvc.perform(get("/mi-informacion/estado-cuenta").with(fabrica.sesion(socio)))
                .andExpect(content().string(containsString("deuda-cero")))
                .andExpect(content().string(containsString("$0.00")))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("TX-000011").contains("TX-000002").doesNotContain("TX-000001<");
    }

    @Test
    @DisplayName("HU1-4 Mi Predio con mapa y foto propia; la foto de otro socio responde 404")
    void miPredio() throws Exception {
        mvc.perform(get("/mi-informacion/predio").with(fabrica.sesion(socio)))
                .andExpect(content().string(containsString("No tiene predios registrados")));
        Predio propio = fabrica.predio(socio, norte);
        propio.setFotoNombre(archivos.guardar(ArchivoService.PREDIOS, "predio-" + propio.getId(),
                new MockMultipartFile("foto", "f.png", "image/png", Fabrica.PNG_MINIMO), "png", null));
        predios.save(propio);
        Predio ajeno = fabrica.predio(vecino, sur);
        mvc.perform(get("/mi-informacion/predio").with(fabrica.sesion(socio)))
                .andExpect(content().string(containsString(propio.getCodigo())))
                .andExpect(content().string(containsString("mapa-mini")))
                .andExpect(content().string(not(containsString(ajeno.getCodigo()))));
        mvc.perform(get("/mi-informacion/predio/" + propio.getId() + "/foto").with(fabrica.sesion(socio)))
                .andExpect(status().isOk());
        mvc.perform(get("/mi-informacion/predio/" + ajeno.getId() + "/foto").with(fabrica.sesion(socio)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("HU1-5 calendario con los turnos de su sector en las próximas 4 semanas")
    void calendario() throws Exception {
        mvc.perform(get("/mi-informacion/calendario").with(fabrica.sesion(socio)))
                .andExpect(content().string(containsString("No hay turnos de su sector en las próximas 4 semanas")));
        Recurso recurso = fabrica.recurso(junta, norte, "Canal Norte");
        fabrica.turno(junta, norte, recurso, Fabrica.proximoDiaLaboral(LocalDate.now().plusDays(3)),
                LocalTime.of(8, 0), LocalTime.of(9, 0)).setResponsable("Responsable cercano");
        fabrica.turno(junta, norte, recurso, Fabrica.proximoDiaLaboral(LocalDate.now().plusDays(40)),
                LocalTime.of(8, 0), LocalTime.of(9, 0)).setResponsable("Responsable lejano");
        Recurso recursoSur = fabrica.recurso(junta, sur, "Canal Sur");
        fabrica.turno(junta, sur, recursoSur, Fabrica.proximoDiaLaboral(LocalDate.now().plusDays(3)),
                LocalTime.of(8, 0), LocalTime.of(9, 0));
        mvc.perform(get("/mi-informacion/calendario").with(fabrica.sesion(socio)))
                .andExpect(content().string(containsString("Canal Norte")))
                .andExpect(content().string(containsString("Responsable cercano")))
                .andExpect(content().string(not(containsString("Responsable lejano"))))
                .andExpect(content().string(not(containsString("Canal Sur"))));
    }

    @Test
    @DisplayName("HU1-6 y HU1-8 solo lectura; la directiva no entra; datos de otro socio nunca aparecen")
    void soloLecturaYPermisos() throws Exception {
        fabrica.cuota(vecino, "Cuota del vecino", "99.00", LocalDate.now());
        for (String ruta : new String[] {"/mi-informacion", "/mi-informacion/estado-cuenta", "/mi-informacion/predio",
                "/mi-informacion/calendario"}) {
            String html = mvc.perform(get(ruta).with(fabrica.sesion(socio)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString(vecino.getNombreCompleto()))))
                    .andExpect(content().string(not(containsString("Cuota del vecino"))))
                    .andReturn().getResponse().getContentAsString();
            assertThat(html.split("method=\"post\"", -1)).hasSize(2);
            assertThat(html).contains("action=\"/logout\"").doesNotContain(">Guardar").doesNotContain(">Eliminar");
            mvc.perform(post(ruta).with(fabrica.sesion(socio)).with(csrf())).andExpect(status().isForbidden());
        }
        for (Rol rol : new Rol[] {Rol.ADMIN_JUNTA, Rol.SECRETARIO, Rol.TESORERO}) {
            mvc.perform(get("/mi-informacion").with(fabrica.sesion(fabrica.usuario(rol, junta))))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("T014 el menú del socio muestra solo Mi información, Anuncios y repositorio y Mensajes")
    void menuDelSocio() throws Exception {
        mvc.perform(get("/mi-informacion").with(fabrica.sesion(socio)))
                .andExpect(content().string(containsString("href=\"/mi-informacion\"")))
                .andExpect(content().string(containsString("href=\"/anuncios\"")))
                .andExpect(content().string(containsString("href=\"/mensajes\"")))
                .andExpect(content().string(not(containsString("href=\"/pagos\""))))
                .andExpect(content().string(not(containsString("href=\"/socios\""))));
    }

    @Test
    @DisplayName("SC-001 Mi Panel con 50 pagos y 50 obligaciones responde en menos de 5 segundos")
    void rendimiento() throws Exception {
        for (int indice = 0; indice < 50; indice++) {
            Multa multa = fabrica.multa(socio, "1.00");
            cobrar(socio, multa.getClave(), "1.00");
            fabrica.cuota(socio, "Cuota " + indice, "2.00", LocalDate.now().plusDays(indice));
        }
        long inicio = System.nanoTime();
        mvc.perform(get("/mi-informacion").with(fabrica.sesion(socio))).andExpect(status().isOk());
        mvc.perform(get("/mi-informacion/estado-cuenta").with(fabrica.sesion(socio)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("$100.00")));
        long milisegundos = (System.nanoTime() - inicio) / 1_000_000;
        assertThat(milisegundos).isLessThan(5000);
    }

    @Test
    @DisplayName("T064 un socio INACTIVO no inicia sesión y su sesión abierta se cierra")
    void socioInactivoSinIngreso() throws Exception {
        var sesionAbierta = fabrica.sesion(socio);
        mvc.perform(get("/mi-informacion").with(sesionAbierta)).andExpect(status().isOk());
        socio.setEstado(EstadoSocio.INACTIVO);
        fabrica.guardar(socio);
        mvc.perform(formLogin("/login").user(socio.getUsername()).password(Fabrica.CLAVE))
                .andExpect(redirectedUrl("/login?desactivada"));
        mvc.perform(get("/mi-informacion").with(sesionAbierta)).andExpect(redirectedUrl("/login?desactivada"));
        socio.setEstado(EstadoSocio.ACTIVO);
        fabrica.guardar(socio);
        mvc.perform(formLogin("/login").user(socio.getUsername()).password(Fabrica.CLAVE))
                .andExpect(redirectedUrl("/inicio"));
    }
}
