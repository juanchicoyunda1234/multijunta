package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.uta.multijunta.model.Conversacion;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Mensaje;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.ConversacionRepository;
import ec.uta.multijunta.repository.MensajeRepository;
import ec.uta.multijunta.repository.NotificacionRepository;
import ec.uta.multijunta.service.PanelService;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CU18 Enviar y recibir mensajes")
class MensajeTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private ConversacionRepository conversaciones;

    @Autowired
    private MensajeRepository mensajes;

    @Autowired
    private NotificacionRepository notificaciones;

    private Junta junta;
    private Socio socio;
    private Socio otroSocio;
    private Usuario admin;
    private Usuario secretario;
    private Usuario tesorero;

    @BeforeEach
    void preparar() {
        junta = fabrica.junta("Junta Mensajes");
        Sector sector = fabrica.sector(junta);
        socio = fabrica.socio(junta, sector);
        otroSocio = fabrica.socio(junta, sector);
        admin = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        secretario = fabrica.secretario(junta);
        tesorero = fabrica.tesorero(junta);
    }

    private Conversacion conversacionDe(Socio dueno) {
        return conversaciones.findBySocioId(dueno.getId()).orElseThrow();
    }

    private ResultActions enviar(Usuario usuario, Conversacion conversacion, String texto) throws Exception {
        return mvc.perform(multipart("/mensajes/" + conversacion.getId()).param("contenido", texto)
                .with(fabrica.sesion(usuario)).with(csrf()));
    }

    private long contador(Usuario usuario) throws Exception {
        String json = mvc.perform(get("/contadores").with(fabrica.sesion(usuario)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return Long.parseLong(json.replaceAll(".*\"mensajes\":(\\d+).*", "$1"));
    }

    private Conversacion abrirDirectiva() throws Exception {
        mvc.perform(get("/mensajes").with(fabrica.sesion(socio))).andExpect(status().isOk());
        return conversacionDe(socio);
    }

    @Test
    @DisplayName("HU4-1 y HU4-2 el socio solo ve 'Directiva' y su mensaje llega a los tres roles con notificación")
    void socioEscribeALaDirectiva() throws Exception {
        String vista = mvc.perform(get("/mensajes").with(fabrica.sesion(socio)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Directiva")))
                .andExpect(content().string(not(containsString(otroSocio.getNombreCompleto()))))
                .andReturn().getResponse().getContentAsString();
        assertThat(vista).doesNotContain(admin.getNombreCompleto());
        Conversacion conversacion = conversacionDe(socio);
        enviar(socio, conversacion, "Buenos días, consulta sobre mi turno").andExpect(status().is3xxRedirection());
        for (Usuario miembro : List.of(admin, secretario, tesorero)) {
            assertThat(contador(miembro)).isEqualTo(1);
            assertThat(notificaciones.findByUsuarioIdOrderByLeidaAscFechaDesc(miembro.getId()))
                    .extracting(n -> n.getTitulo() + " " + n.getEnlace())
                    .containsExactly("Nuevo mensaje /mensajes/" + conversacion.getId());
        }
        assertThat(contador(socio)).isZero();
    }

    @Test
    @DisplayName("HU4-3 y FR-018 la respuesta de un miembro llega como 'Directiva' y la lectura es compartida")
    void respuestaDeLaDirectiva() throws Exception {
        Conversacion conversacion = abrirDirectiva();
        enviar(socio, conversacion, "Pregunta del socio");
        mvc.perform(get("/mensajes").with(fabrica.sesion(admin)))
                .andExpect(content().string(containsString(socio.getNombreCompleto())));
        mvc.perform(get("/mensajes/" + conversacion.getId()).with(fabrica.sesion(admin)))
                .andExpect(content().string(containsString("Pregunta del socio")));
        assertThat(contador(admin)).isZero();
        assertThat(contador(secretario)).isZero();
        enviar(secretario, conversacion, "Respuesta de la directiva").andExpect(status().is3xxRedirection());
        assertThat(contador(socio)).isEqualTo(1);
        assertThat(notificaciones.findByUsuarioIdOrderByLeidaAscFechaDesc(socio.getId()))
                .extracting(n -> n.getTitulo()).containsExactly("Nuevo mensaje");
        mvc.perform(get("/mensajes/" + conversacion.getId()).with(fabrica.sesion(socio)))
                .andExpect(content().string(containsString("Respuesta de la directiva")))
                .andExpect(content().string(containsString("Directiva · " + secretario.getNombreCompleto())));
        assertThat(contador(socio)).isZero();
        assertThat(contador(admin)).isZero();
    }

    @Test
    @DisplayName("HU4-3 la directiva inicia conversaciones con socios y entre sus miembros")
    void conversacionesDeLaDirectiva() throws Exception {
        mvc.perform(get("/mensajes").with(fabrica.sesion(admin)))
                .andExpect(content().string(containsString(otroSocio.getNombreCompleto())))
                .andExpect(content().string(containsString(tesorero.getNombreCompleto())));
        mvc.perform(post("/mensajes/nueva").param("usuarioId", tesorero.getId().toString())
                .with(fabrica.sesion(admin)).with(csrf())).andExpect(status().is3xxRedirection());
        Conversacion interna = conversaciones.findAll().stream().filter(c -> c.getSocio() == null)
                .filter(c -> c.getJunta().getId().equals(junta.getId())).findFirst().orElseThrow();
        enviar(admin, interna, "Reunión de directiva el lunes");
        assertThat(contador(tesorero)).isEqualTo(1);
        assertThat(contador(secretario)).isZero();
        mvc.perform(get("/mensajes/" + interna.getId()).with(fabrica.sesion(secretario))).andExpect(status().isNotFound());
        mvc.perform(post("/mensajes/nueva").param("usuarioId", otroSocio.getId().toString())
                .with(fabrica.sesion(admin)).with(csrf())).andExpect(status().is3xxRedirection());
        enviar(admin, conversacionDe(otroSocio), "Recordatorio de pago");
        assertThat(contador(otroSocio)).isEqualTo(1);
    }

    @Test
    @DisplayName("HU4-4 y HU4-5 texto vacío rechazado, botón deshabilitado e imagen en miniatura")
    void contenidoEImagen() throws Exception {
        Conversacion conversacion = abrirDirectiva();
        mvc.perform(get("/mensajes/" + conversacion.getId()).with(fabrica.sesion(socio)))
                .andExpect(content().string(containsString("id=\"botonEnviar\"")))
                .andExpect(content().string(containsString("disabled")));
        enviar(socio, conversacion, "   ")
                .andExpect(content().string(containsString("Escriba un mensaje")));
        assertThat(mensajes.findByConversacionIdOrderByIdAsc(conversacion.getId())).isEmpty();
        mvc.perform(multipart("/mensajes/" + conversacion.getId())
                        .file(new MockMultipartFile("imagen", "foto.png", "image/png", Fabrica.PNG_MINIMO))
                        .with(fabrica.sesion(socio)).with(csrf()))
                .andExpect(status().is3xxRedirection());
        Mensaje conImagen = mensajes.findByConversacionIdOrderByIdAsc(conversacion.getId()).get(0);
        assertThat(conImagen.getImagenNombre()).isNotNull();
        mvc.perform(get("/mensajes/" + conversacion.getId()).with(fabrica.sesion(socio)))
                .andExpect(content().string(containsString("/mensajes/imagen/" + conImagen.getId())));
        mvc.perform(get("/mensajes/imagen/" + conImagen.getId()).with(fabrica.sesion(admin))).andExpect(status().isOk());
        mvc.perform(get("/mensajes/imagen/" + conImagen.getId()).with(fabrica.sesion(otroSocio)))
                .andExpect(status().isNotFound());
        mvc.perform(multipart("/mensajes/" + conversacion.getId())
                        .file(new MockMultipartFile("imagen", "nota.txt", "text/plain", "hola".getBytes()))
                        .with(fabrica.sesion(socio)).with(csrf()))
                .andExpect(content().string(containsString("La imagen debe ser JPG o PNG")));
    }

    @Test
    @DisplayName("HU4-6 un socio no escribe a otro socio ni a otra junta")
    void restricciones() throws Exception {
        abrirDirectiva();
        mvc.perform(get("/mensajes").with(fabrica.sesion(otroSocio)));
        Conversacion ajena = conversacionDe(otroSocio);
        enviar(socio, ajena, "Hola vecino").andExpect(status().isNotFound());
        mvc.perform(post("/mensajes/nueva").param("usuarioId", otroSocio.getId().toString())
                .with(fabrica.sesion(socio)).with(csrf())).andExpect(status().isForbidden());
        Junta otra = fabrica.junta("Junta Ajena Mensajes");
        Usuario adminAjeno = fabrica.usuario(Rol.ADMIN_JUNTA, otra);
        enviar(adminAjeno, ajena, "Intruso").andExpect(status().isNotFound());
        mvc.perform(post("/mensajes/nueva").param("usuarioId", socio.getId().toString())
                .with(fabrica.sesion(adminAjeno)).with(csrf())).andExpect(status().isNotFound());
        assertThat(mensajes.findByConversacionIdOrderByIdAsc(ajena.getId())).isEmpty();
    }

    @Test
    @DisplayName("Edge un miembro de la directiva desactivado deja de ser contacto y de recibir avisos")
    void miembroDesactivado() throws Exception {
        tesorero.setActivo(false);
        fabrica.guardar(tesorero);
        mvc.perform(get("/mensajes").with(fabrica.sesion(admin)))
                .andExpect(content().string(not(containsString(tesorero.getNombreCompleto()))));
        Conversacion conversacion = abrirDirectiva();
        enviar(socio, conversacion, "Consulta");
        assertThat(notificaciones.findByUsuarioIdOrderByLeidaAscFechaDesc(tesorero.getId())).isEmpty();
        assertThat(notificaciones.findByUsuarioIdOrderByLeidaAscFechaDesc(admin.getId())).hasSize(1);
    }

    @Test
    @DisplayName("HU4-7 y FR-019 contadores en JSON, sondeo cada 15 s y burbujas que se refrescan")
    void sondeo() throws Exception {
        Conversacion conversacion = abrirDirectiva();
        enviar(socio, conversacion, "Mensaje para refrescar");
        mvc.perform(get("/contadores").with(fabrica.sesion(admin)))
                .andExpect(jsonPath("$.mensajes").value(1))
                .andExpect(jsonPath("$.notificaciones").value(1));
        mvc.perform(get("/panel").with(fabrica.sesion(admin)))
                .andExpect(content().string(containsString("/contadores")))
                .andExpect(content().string(containsString("15000")));
        mvc.perform(get("/mensajes/" + conversacion.getId()).with(fabrica.sesion(admin)))
                .andExpect(content().string(containsString("/mensajes/" + conversacion.getId() + "/burbujas")));
        mvc.perform(get("/mensajes/" + conversacion.getId() + "/burbujas").with(fabrica.sesion(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mensaje para refrescar")))
                .andExpect(content().string(not(containsString("<html"))));
        mvc.perform(get("/contadores").with(fabrica.sesion(fabrica.superAdmin()))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("FR-031 la tarjeta 'Mensajes no leídos' del panel muestra el número real")
    @SuppressWarnings("unchecked")
    void tarjetaDelPanel() throws Exception {
        Conversacion conversacion = abrirDirectiva();
        enviar(socio, conversacion, "Uno");
        enviar(socio, conversacion, "Dos");
        List<PanelService.Tarjeta> tarjetas = (List<PanelService.Tarjeta>) mvc.perform(get("/panel")
                .with(fabrica.sesion(admin))).andReturn().getModelAndView().getModel().get("tarjetas");
        assertThat(tarjetas).filteredOn(t -> t.titulo().equals("Mensajes no leídos"))
                .extracting(PanelService.Tarjeta::valor).containsExactly(2L);
    }
}
