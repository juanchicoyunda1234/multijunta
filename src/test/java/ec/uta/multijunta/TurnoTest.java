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

import ec.uta.multijunta.model.EstadoSocio;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Recurso;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.Turno;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.NotificacionRepository;
import ec.uta.multijunta.repository.RecursoRepository;
import ec.uta.multijunta.repository.TurnoRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
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
@DisplayName("CU08 Gestionar calendario y turnos")
class TurnoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private TurnoRepository turnos;

    @Autowired
    private RecursoRepository recursos;

    @Autowired
    private NotificacionRepository notificaciones;

    private Junta junta;
    private Sector sector;
    private Sector otroSector;
    private Recurso compuerta;
    private Usuario secretario;
    private LocalDate lunes;

    @BeforeEach
    void preparar() {
        junta = fabrica.junta("Junta Turnos");
        sector = fabrica.sector(junta);
        otroSector = fabrica.sector(junta);
        compuerta = fabrica.recurso(junta, sector, "Compuerta #3");
        secretario = fabrica.secretario(junta);
        lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    private ResultActions agendar(LocalDate fecha, String inicio, String fin, Sector destino, Recurso recurso)
            throws Exception {
        return mvc.perform(post("/turnos").with(fabrica.sesion(secretario)).with(csrf())
                .param("fecha", fecha.toString()).param("horaInicio", inicio).param("horaFin", fin)
                .param("sectorId", destino.getId().toString()).param("recursoId", recurso.getId().toString())
                .param("responsable", "Juan Pérez"));
    }

    @Test
    @DisplayName("HU5-1 y HU5-2 semana lunes–viernes, navegación y agendar")
    void semanaYAgendar() throws Exception {
        agendar(lunes.plusDays(2), "08:00", "10:00", sector, compuerta)
                .andExpect(flash().attribute("exito", "turno.agendado"));
        mvc.perform(get("/turnos").param("semana", lunes.plusDays(3).toString()).with(fabrica.sesion(secretario)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("lunes", lunes))
                .andExpect(content().string(containsString("08:00–10:00")))
                .andExpect(content().string(containsString("Compuerta #3")))
                .andExpect(content().string(containsString("+ Agendar")))
                .andExpect(content().string(containsString("Semana anterior")));
    }

    @Test
    @DisplayName("HU5-3 conflicto de recurso al cruzarse; sin conflicto si solo se tocan")
    void conflicto() throws Exception {
        agendar(lunes, "08:00", "10:00", sector, compuerta).andExpect(flash().attributeExists("exito"));
        agendar(lunes, "09:00", "11:00", otroSector, compuerta)
                .andExpect(model().attribute("abrir", "agendar"))
                .andExpect(content().string(containsString("Conflicto de recursos detectado")));
        agendar(lunes, "07:00", "08:30", sector, compuerta)
                .andExpect(content().string(containsString("Conflicto de recursos detectado")));
        agendar(lunes, "10:00", "12:00", otroSector, compuerta).andExpect(flash().attribute("exito", "turno.agendado"));
        agendar(lunes, "06:00", "08:00", otroSector, compuerta).andExpect(flash().attribute("exito", "turno.agendado"));
        Recurso otroRecurso = fabrica.recurso(junta, sector, "Canal 2");
        agendar(lunes, "09:00", "11:00", otroSector, otroRecurso).andExpect(flash().attributeExists("exito"));
        assertThat(turnos.findByJuntaIdAndFechaBetweenOrderByFechaAscHoraInicioAsc(junta.getId(), lunes, lunes)).hasSize(4);
    }

    @Test
    @DisplayName("HU5-4 reprogramar con la misma validación")
    void reprogramar() throws Exception {
        Turno turno = fabrica.turno(junta, sector, compuerta, lunes, LocalTime.of(8, 0), LocalTime.of(9, 0));
        fabrica.turno(junta, otroSector, compuerta, lunes.plusDays(1), LocalTime.of(8, 0), LocalTime.of(9, 0));
        mvc.perform(post("/turnos/" + turno.getId()).with(fabrica.sesion(secretario)).with(csrf())
                        .param("fecha", lunes.plusDays(1).toString()).param("horaInicio", "08:30").param("horaFin", "09:30"))
                .andExpect(content().string(containsString("Conflicto de recursos detectado")));
        mvc.perform(post("/turnos/" + turno.getId()).with(fabrica.sesion(secretario)).with(csrf())
                        .param("fecha", lunes.plusDays(1).toString()).param("horaInicio", "10:00").param("horaFin", "11:00"))
                .andExpect(flash().attribute("exito", "turno.reprogramado"));
        Turno movido = turnos.findById(turno.getId()).orElseThrow();
        assertThat(movido.getFecha()).isEqualTo(lunes.plusDays(1));
        assertThat(movido.getHoraInicio()).isEqualTo(LocalTime.of(10, 0));
    }

    @Test
    @DisplayName("HU5-5 notifica a los socios con cuenta activa del sector")
    void notificaciones() throws Exception {
        Socio conCuenta = fabrica.socio(junta, sector);
        Socio sinCuenta = fabrica.socioSinCuenta(junta, sector, EstadoSocio.ACTIVO);
        Socio deOtroSector = fabrica.socio(junta, otroSector);
        agendar(lunes, "08:00", "10:00", sector, compuerta);
        assertThat(notificaciones.findByUsuarioIdOrderByLeidaAscFechaDesc(conCuenta.getId()))
                .extracting(n -> n.getTitulo()).containsExactly("Turno de riego programado");
        assertThat(notificaciones.countByUsuarioIdAndLeidaFalse(sinCuenta.getId())).isZero();
        assertThat(notificaciones.countByUsuarioIdAndLeidaFalse(deOtroSector.getId())).isZero();
        Turno turno = turnos.findByJuntaIdAndFechaBetweenOrderByFechaAscHoraInicioAsc(junta.getId(), lunes, lunes).get(0);
        mvc.perform(post("/turnos/" + turno.getId()).with(fabrica.sesion(secretario)).with(csrf())
                .param("fecha", lunes.plusDays(1).toString()).param("horaInicio", "08:00").param("horaFin", "10:00"));
        assertThat(notificaciones.findByUsuarioIdOrderByLeidaAscFechaDesc(conCuenta.getId()))
                .extracting(n -> n.getTitulo()).contains("Turno de riego reprogramado");
    }

    @Test
    @DisplayName("Reglas: fin posterior al inicio, sin fines de semana, recurso activo")
    void reglas() throws Exception {
        agendar(lunes, "10:00", "09:00", sector, compuerta)
                .andExpect(content().string(containsString("La hora de fin debe ser posterior a la de inicio")));
        agendar(lunes.plusDays(5), "08:00", "09:00", sector, compuerta)
                .andExpect(content().string(containsString("Los turnos se agendan de lunes a viernes")));
        compuerta.setActivo(false);
        recursos.save(compuerta);
        agendar(lunes, "08:00", "09:00", sector, compuerta)
                .andExpect(content().string(containsString("El recurso no está activo")));
        assertThat(turnos.findByJuntaIdAndFechaBetweenOrderByFechaAscHoraInicioAsc(junta.getId(), lunes,
                lunes.plusDays(6))).isEmpty();
    }

    @Test
    @DisplayName("Permisos y aislamiento")
    void permisosYAislamiento() throws Exception {
        Turno turno = fabrica.turno(junta, sector, compuerta, lunes, LocalTime.of(8, 0), LocalTime.of(9, 0));
        Usuario admin = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        mvc.perform(get("/turnos").param("semana", lunes.toString()).with(fabrica.sesion(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("+ Agendar"))));
        mvc.perform(post("/turnos/" + turno.getId()).with(fabrica.sesion(admin)).with(csrf())
                .param("fecha", lunes.toString()).param("horaInicio", "10:00").param("horaFin", "11:00"))
                .andExpect(status().isForbidden());
        Junta otra = fabrica.junta("Junta Ajena Turnos");
        Sector sectorAjeno = fabrica.sector(otra);
        Recurso recursoAjeno = fabrica.recurso(otra, sectorAjeno, "Ajeno");
        Turno ajeno = fabrica.turno(otra, sectorAjeno, recursoAjeno, lunes, LocalTime.of(8, 0), LocalTime.of(9, 0));
        mvc.perform(post("/turnos/" + ajeno.getId()).with(fabrica.sesion(secretario)).with(csrf())
                .param("fecha", lunes.toString()).param("horaInicio", "10:00").param("horaFin", "11:00"))
                .andExpect(status().isNotFound());
        agendar(lunes, "12:00", "13:00", sector, recursoAjeno).andExpect(status().isNotFound());
        mvc.perform(get("/turnos").param("semana", lunes.toString()).with(fabrica.sesion(secretario)))
                .andExpect(content().string(not(containsString("Ajeno"))));
    }
}
