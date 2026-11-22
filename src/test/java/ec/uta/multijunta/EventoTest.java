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

import ec.uta.multijunta.model.EstadoEvento;
import ec.uta.multijunta.model.Evento;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.TipoEvento;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.EventoRepository;
import java.time.LocalDate;
import java.time.YearMonth;
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
@DisplayName("CU26 Gestionar eventos")
class EventoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private EventoRepository eventos;

    private Junta junta;
    private Usuario secretario;

    @BeforeEach
    void preparar() {
        junta = fabrica.junta("Junta Eventos");
        secretario = fabrica.secretario(junta);
    }

    @Test
    @DisplayName("HU2-1 y HU2-2 crear cada tipo de evento; tarjeta con Tomar Asistencia solo si corresponde")
    void crearEventos() throws Exception {
        LocalDate hoy = LocalDate.now();
        for (TipoEvento tipo : TipoEvento.values()) {
            mvc.perform(post("/eventos").with(fabrica.sesion(secretario)).with(csrf())
                            .param("tipo", tipo.name()).param("fecha", hoy.toString()).param("hora", "09:30")
                            .param("lugar", "Casa comunal").param("descripcion", "Orden del día"))
                    .andExpect(flash().attribute("exito", "evento.creado"));
        }
        assertThat(eventos.findByJuntaIdAndFechaBetweenOrderByFechaAscHoraAsc(junta.getId(), hoy, hoy))
                .hasSize(3).allMatch(evento -> evento.getEstado() == EstadoEvento.PROGRAMADO);
        String html = mvc.perform(get("/eventos").param("mes", YearMonth.from(hoy).toString())
                        .with(fabrica.sesion(secretario)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Calendario de Actividades")))
                .andExpect(content().string(containsString("Nuevo Evento")))
                .andReturn().getResponse().getContentAsString();
        assertThat(html.split("Tomar Asistencia", -1).length - 1).isEqualTo(2);
        Evento futuro = fabrica.evento(junta, TipoEvento.ASAMBLEA, hoy.plusDays(3));
        mvc.perform(get("/eventos").param("mes", YearMonth.from(futuro.getFecha()).toString())
                        .with(fabrica.sesion(secretario)))
                .andExpect(model().attribute("tomables", org.hamcrest.Matchers.hasEntry(futuro.getId(), false)));
    }

    @Test
    @DisplayName("HU2-3 editar y cancelar un evento sin asistencia")
    void editarYCancelar() throws Exception {
        Evento evento = fabrica.evento(junta, TipoEvento.MINGA, LocalDate.now().plusDays(2));
        mvc.perform(post("/eventos/" + evento.getId()).with(fabrica.sesion(secretario)).with(csrf())
                        .param("tipo", "MINGA").param("fecha", evento.getFecha().toString()).param("hora", "07:00")
                        .param("lugar", "Canal principal"))
                .andExpect(flash().attribute("exito", "evento.actualizado"));
        assertThat(eventos.findById(evento.getId()).orElseThrow().getLugar()).isEqualTo("Canal principal");
        mvc.perform(post("/eventos/" + evento.getId() + "/cancelar").with(fabrica.sesion(secretario)).with(csrf()))
                .andExpect(flash().attribute("exito", "evento.cancelado"));
        assertThat(eventos.findById(evento.getId()).orElseThrow().getEstado()).isEqualTo(EstadoEvento.CANCELADO);
        mvc.perform(get("/eventos").param("mes", YearMonth.from(evento.getFecha()).toString())
                        .with(fabrica.sesion(secretario)))
                .andExpect(content().string(containsString("Cancelado")));
    }

    @Test
    @DisplayName("HU2-4 un evento con asistencia cerrada no se edita ni cancela")
    void eventoCerrado() throws Exception {
        Evento evento = fabrica.evento(junta, TipoEvento.ASAMBLEA, LocalDate.now().minusDays(1));
        evento.setAsistenciaCerrada(true);
        eventos.save(evento);
        mvc.perform(post("/eventos/" + evento.getId()).with(fabrica.sesion(secretario)).with(csrf())
                        .param("tipo", "ASAMBLEA").param("fecha", evento.getFecha().toString()).param("hora", "10:00")
                        .param("lugar", "Otro"))
                .andExpect(flash().attribute("error", "evento.cerrado"));
        mvc.perform(post("/eventos/" + evento.getId() + "/cancelar").with(fabrica.sesion(secretario)).with(csrf()))
                .andExpect(flash().attribute("error", "evento.cerrado"));
        assertThat(eventos.findById(evento.getId()).orElseThrow().getEstado()).isEqualTo(EstadoEvento.PROGRAMADO);
        mvc.perform(get("/eventos").with(fabrica.sesion(secretario)).flashAttr("error", "evento.cerrado"))
                .andExpect(content().string(containsString("No se puede modificar un evento con asistencia cerrada")));
    }

    @Test
    @DisplayName("HU2-5 campos obligatorios vacíos")
    void obligatorios() throws Exception {
        mvc.perform(post("/eventos").with(fabrica.sesion(secretario)).with(csrf()))
                .andExpect(model().attributeHasFieldErrors("eventoFormulario", "tipo", "fecha", "hora", "lugar"))
                .andExpect(model().attribute("abrir", "nuevo"))
                .andExpect(content().string(containsString("Este campo es obligatorio")));
    }

    @Test
    @DisplayName("Permisos y aislamiento: ADMIN_JUNTA solo lee; evento de otra junta responde 404")
    void permisosYAislamiento() throws Exception {
        Evento evento = fabrica.evento(junta, TipoEvento.MINGA, LocalDate.now().plusDays(1));
        Usuario admin = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        mvc.perform(get("/eventos").param("mes", YearMonth.from(evento.getFecha()).toString())
                        .with(fabrica.sesion(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("data-bs-target=\"#modalNuevo\""))));
        mvc.perform(post("/eventos/" + evento.getId() + "/cancelar").with(fabrica.sesion(admin)).with(csrf()))
                .andExpect(status().isForbidden());
        Junta otra = fabrica.junta("Junta Otra Eventos");
        Evento ajeno = fabrica.evento(otra, TipoEvento.MINGA, LocalDate.now().plusDays(1));
        mvc.perform(post("/eventos/" + ajeno.getId() + "/cancelar").with(fabrica.sesion(secretario)).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(eventos.findById(ajeno.getId()).orElseThrow().getEstado()).isEqualTo(EstadoEvento.PROGRAMADO);
    }
}
