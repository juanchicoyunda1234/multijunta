package ec.uta.multijunta.service;

import ec.uta.multijunta.model.Asistencia;
import ec.uta.multijunta.model.EstadoAsistencia;
import ec.uta.multijunta.model.EstadoSocio;
import ec.uta.multijunta.model.Evento;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.TipoEvento;
import ec.uta.multijunta.repository.AsistenciaRepository;
import ec.uta.multijunta.repository.EventoRepository;
import ec.uta.multijunta.repository.SocioRepository;
import ec.uta.multijunta.model.EstadoEvento;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AsistenciaService implements IAsistencia {

    public static final Set<TipoEvento> TIPOS_CON_ASISTENCIA = EnumSet.of(TipoEvento.ASAMBLEA, TipoEvento.MINGA);

    public record Resumen(long presentes, long ausentes, long justificados, long total, long porcentaje) {
    }

    public record FilaEvento(Evento evento, Resumen resumen, boolean admiteToma) {
    }

    private final EventoRepository eventos;
    private final AsistenciaRepository asistencias;
    private final SocioRepository socios;
    private final UsuarioActual usuarioActual;

    public AsistenciaService(EventoRepository eventos, AsistenciaRepository asistencias, SocioRepository socios,
            UsuarioActual usuarioActual) {
        this.eventos = eventos;
        this.asistencias = asistencias;
        this.socios = socios;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public List<FilaEvento> eventosConAsistencia() {
        LocalDate hoy = LocalDate.now();
        return eventos.findByJuntaIdAndTipoInOrderByFechaDescHoraDesc(usuarioActual.juntaId(), TIPOS_CON_ASISTENCIA)
                .stream()
                .map(evento -> new FilaEvento(evento,
                        evento.isAsistenciaCerrada() ? resumen(asistencias(evento.getId())) : null,
                        evento.admiteToma(hoy)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Evento evento(Long eventoId) {
        return eventos.findByIdAndJuntaId(eventoId, usuarioActual.juntaId())
                .orElseThrow(RecursoNoEncontradoException::new);
    }

    @Transactional(readOnly = true)
    public List<Socio> planilla(Long eventoId) {
        validarToma(evento(eventoId));
        return sociosActivos();
    }

    @Transactional
    public Resumen cerrar(Long eventoId, Map<Long, EstadoAsistencia> estados) {
        Evento evento = evento(eventoId);
        validarToma(evento);
        List<Socio> activos = sociosActivos();
        if (activos.isEmpty()) {
            throw new ReglaNegocioException("asistencia.sinSocios");
        }
        List<Asistencia> registro = new ArrayList<>();
        for (Socio socio : activos) {
            Asistencia asistencia = new Asistencia();
            asistencia.setEvento(evento);
            asistencia.setSocio(socio);
            asistencia.setFecha(evento.getFecha());
            asistencia.setEstado(estados.getOrDefault(socio.getId(), EstadoAsistencia.AUSENTE));
            registro.add(asistencia);
        }
        asistencias.saveAll(registro);
        evento.setAsistenciaCerrada(true);
        eventos.save(evento);
        return resumen(registro);
    }

    @Transactional(readOnly = true)
    public List<Asistencia> asistencias(Long eventoId) {
        evento(eventoId);
        return asistencias.findByEventoIdOrderBySocioApellidosAscSocioNombresAsc(eventoId);
    }

    @Transactional(readOnly = true)
    public Resumen resumen(Long eventoId) {
        return resumen(asistencias(eventoId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inasistencia> inasistenciasDeEventosCerrados(Long juntaId) {
        return asistencias
                .findByEventoJuntaIdAndEventoAsistenciaCerradaTrueAndEstadoOrderByEventoFechaAscSocioApellidosAsc(
                        juntaId, EstadoAsistencia.AUSENTE)
                .stream().map(AsistenciaService::inasistencia).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inasistencia> inasistenciasDelEvento(Long juntaId, Long eventoId) {
        return asistencias
                .findByEventoIdAndEventoJuntaIdAndEventoAsistenciaCerradaTrueAndEstadoOrderBySocioApellidosAsc(
                        eventoId, juntaId, EstadoAsistencia.AUSENTE)
                .stream().map(AsistenciaService::inasistencia).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoSinCerrar> eventosSinCerrar(Long juntaId) {
        return eventos.findByJuntaIdAndTipoInAndEstadoAndAsistenciaCerradaFalseAndFechaLessThanEqualOrderByFechaDesc(
                        juntaId, TIPOS_CON_ASISTENCIA, EstadoEvento.PROGRAMADO, LocalDate.now())
                .stream()
                .map(evento -> new EventoSinCerrar(evento.getId(), evento.getTipo(), evento.getFecha(), evento.getLugar()))
                .toList();
    }

    private void validarToma(Evento evento) {
        if (evento.isAsistenciaCerrada()) {
            throw new ReglaNegocioException("asistencia.yaCerrada");
        }
        if (!evento.admiteToma(LocalDate.now())) {
            throw new ReglaNegocioException("asistencia.noAdmite");
        }
    }

    private List<Socio> sociosActivos() {
        return socios.findAllByJuntaIdAndEstadoOrderByApellidosAscNombresAsc(usuarioActual.juntaId(),
                EstadoSocio.ACTIVO);
    }

    private static Resumen resumen(List<Asistencia> registro) {
        long presentes = registro.stream().filter(a -> a.getEstado() == EstadoAsistencia.PRESENTE).count();
        long ausentes = registro.stream().filter(a -> a.getEstado() == EstadoAsistencia.AUSENTE).count();
        long justificados = registro.stream().filter(a -> a.getEstado() == EstadoAsistencia.JUSTIFICADO).count();
        long total = registro.size();
        long porcentaje = total == 0 ? 0 : Math.round(presentes * 100.0 / total);
        return new Resumen(presentes, ausentes, justificados, total, porcentaje);
    }

    private static Inasistencia inasistencia(Asistencia asistencia) {
        Evento evento = asistencia.getEvento();
        Socio socio = asistencia.getSocio();
        return new Inasistencia(asistencia.getId(), evento.getId(), evento.getTipo(), evento.getFecha(),
                socio.getId(), socio.getCedula(), socio.getNombreCompleto());
    }
}
