package ec.uta.multijunta.service;

import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Recurso;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.model.Turno;
import ec.uta.multijunta.repository.JuntaRepository;
import ec.uta.multijunta.repository.RecursoRepository;
import ec.uta.multijunta.repository.SectorRepository;
import ec.uta.multijunta.repository.SocioRepository;
import ec.uta.multijunta.repository.TurnoRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TurnoService {

    public record Dia(LocalDate fecha, String etiqueta, List<Turno> turnos) {
    }

    private static final Locale ESPANOL = Locale.forLanguageTag("es");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy", ESPANOL);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("EEE dd/MM", ESPANOL);

    private final TurnoRepository turnos;
    private final RecursoRepository recursos;
    private final SectorRepository sectores;
    private final SocioRepository socios;
    private final JuntaRepository juntas;
    private final NotificacionService notificaciones;
    private final MessageSource mensajes;
    private final UsuarioActual usuarioActual;

    public TurnoService(TurnoRepository turnos, RecursoRepository recursos, SectorRepository sectores,
            SocioRepository socios, JuntaRepository juntas, NotificacionService notificaciones, MessageSource mensajes,
            UsuarioActual usuarioActual) {
        this.turnos = turnos;
        this.recursos = recursos;
        this.sectores = sectores;
        this.socios = socios;
        this.juntas = juntas;
        this.notificaciones = notificaciones;
        this.mensajes = mensajes;
        this.usuarioActual = usuarioActual;
    }

    public static LocalDate lunesDe(LocalDate fecha) {
        return fecha.with(DayOfWeek.MONDAY);
    }

    @Transactional(readOnly = true)
    public List<Dia> semana(LocalDate fecha) {
        LocalDate lunes = lunesDe(fecha);
        List<Turno> enSemana = turnos.findByJuntaIdAndFechaBetweenOrderByFechaAscHoraInicioAsc(usuarioActual.juntaId(),
                lunes, lunes.plusDays(4));
        List<Dia> dias = new ArrayList<>();
        for (int indice = 0; indice < 5; indice++) {
            LocalDate dia = lunes.plusDays(indice);
            dias.add(new Dia(dia, dia.format(DIA), enSemana.stream().filter(turno -> turno.getFecha().equals(dia)).toList()));
        }
        return dias;
    }

    @Transactional(readOnly = true)
    public Turno obtener(Long id) {
        return turnos.findByIdAndJuntaId(id, usuarioActual.juntaId()).orElseThrow(RecursoNoEncontradoException::new);
    }

    @Transactional
    public Turno agendar(DatosTurno datos) {
        Long juntaId = usuarioActual.juntaId();
        Sector sector = sectores.findByIdAndJuntaId(datos.sectorId(), juntaId)
                .orElseThrow(RecursoNoEncontradoException::new);
        Recurso recurso = recursos.findByIdAndJuntaId(datos.recursoId(), juntaId)
                .orElseThrow(RecursoNoEncontradoException::new);
        validar(recurso, datos.fecha(), datos.horaInicio(), datos.horaFin(), null);
        Junta junta = juntas.findById(juntaId).orElseThrow(RecursoNoEncontradoException::new);
        Turno turno = new Turno();
        turno.setJunta(junta);
        turno.setSector(sector);
        turno.setRecurso(recurso);
        turno.setFecha(datos.fecha());
        turno.setHoraInicio(datos.horaInicio());
        turno.setHoraFin(datos.horaFin());
        turno.setResponsable(datos.responsable().trim());
        Turno guardado = turnos.save(turno);
        notificar(guardado, "notificacion.turno.titulo");
        return guardado;
    }

    @Transactional
    public Turno reprogramar(Long id, LocalDate fecha, LocalTime inicio, LocalTime fin) {
        Turno turno = obtener(id);
        validar(turno.getRecurso(), fecha, inicio, fin, id);
        turno.setFecha(fecha);
        turno.setHoraInicio(inicio);
        turno.setHoraFin(fin);
        Turno guardado = turnos.save(turno);
        notificar(guardado, "notificacion.turnoReprogramado.titulo");
        return guardado;
    }

    private void validar(Recurso recurso, LocalDate fecha, LocalTime inicio, LocalTime fin, Long excluirId) {
        if (fecha == null || inicio == null || fin == null) {
            throw new ReglaNegocioException("campo.obligatorio", "fecha");
        }
        if (!fin.isAfter(inicio)) {
            throw new ReglaNegocioException("turno.horas", "horaFin");
        }
        if (fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new ReglaNegocioException("turno.finDeSemana", "fecha");
        }
        if (!recurso.isActivo()) {
            throw new ReglaNegocioException("turno.recursoInactivo", "recursoId");
        }
        if (turnos.existeConflicto(recurso.getId(), fecha, inicio, fin, excluirId)) {
            throw new ReglaNegocioException("turno.conflicto", "recursoId");
        }
    }

    private void notificar(Turno turno, String claveTitulo) {
        String titulo = mensajes.getMessage(claveTitulo, null, ESPANOL);
        String mensaje = turno.getFecha().format(FECHA) + " de " + turno.getHoraInicio().format(HORA) + " a "
                + turno.getHoraFin().format(HORA) + " · " + turno.getRecurso().getNombre() + " · "
                + turno.getSector().getNombre();
        socios.findBySectorIdAndTieneCuentaTrueAndActivoTrue(turno.getSector().getId())
                .forEach(socio -> notificaciones.crear(socio, titulo, mensaje, "/notificaciones"));
    }
}
