package ec.uta.multijunta.service;

import ec.uta.multijunta.model.Convocatoria;
import ec.uta.multijunta.model.EstadoConvocatoria;
import ec.uta.multijunta.model.EstadoSocio;
import ec.uta.multijunta.model.Evento;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.PrioridadConvocatoria;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.TipoConvocatoria;
import ec.uta.multijunta.model.TipoEvento;
import ec.uta.multijunta.repository.ConvocatoriaRepository;
import ec.uta.multijunta.repository.EventoRepository;
import ec.uta.multijunta.repository.JuntaRepository;
import ec.uta.multijunta.repository.SocioRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConvocatoriaService {

    private static final Locale ESPANOL = Locale.forLanguageTag("es");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ConvocatoriaRepository convocatorias;
    private final EventoRepository eventos;
    private final JuntaRepository juntas;
    private final SocioRepository socios;
    private final NotificacionService notificaciones;
    private final MessageSource mensajes;
    private final UsuarioActual usuarioActual;

    public ConvocatoriaService(ConvocatoriaRepository convocatorias, EventoRepository eventos, JuntaRepository juntas,
            SocioRepository socios, NotificacionService notificaciones, MessageSource mensajes,
            UsuarioActual usuarioActual) {
        this.convocatorias = convocatorias;
        this.eventos = eventos;
        this.juntas = juntas;
        this.socios = socios;
        this.notificaciones = notificaciones;
        this.mensajes = mensajes;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public List<Convocatoria> listar() {
        return convocatorias.findByJuntaIdOrderByFechaPublicacionDescIdDesc(usuarioActual.juntaId());
    }

    @Transactional(readOnly = true)
    public Convocatoria obtener(Long id) {
        return convocatorias.findByIdAndJuntaId(id, usuarioActual.juntaId()).orElseThrow(RecursoNoEncontradoException::new);
    }

    @Transactional(readOnly = true)
    public Convocatoria borrador(Long id) {
        Convocatoria convocatoria = obtener(id);
        if (!convocatoria.isBorrador()) {
            throw new ReglaNegocioException("convocatoria.noEditable");
        }
        return convocatoria;
    }

    @Transactional(readOnly = true)
    public Evento evento(Long eventoId) {
        Evento evento = eventos.findByIdAndJuntaId(eventoId, usuarioActual.juntaId())
                .orElseThrow(RecursoNoEncontradoException::new);
        if (evento.getTipo() == TipoEvento.REUNION) {
            throw new RecursoNoEncontradoException();
        }
        return evento;
    }

    public static TipoConvocatoria tipoDe(Evento evento) {
        return evento.getTipo() == TipoEvento.MINGA ? TipoConvocatoria.MINGA : TipoConvocatoria.ASAMBLEA;
    }

    @Transactional
    public Convocatoria crear(DatosConvocatoria datos) {
        Junta junta = juntas.findById(usuarioActual.juntaId()).orElseThrow(RecursoNoEncontradoException::new);
        Convocatoria convocatoria = new Convocatoria();
        convocatoria.setJunta(junta);
        convocatoria.setAutor(usuarioActual.entidad());
        return guardar(convocatoria, datos);
    }

    @Transactional
    public Convocatoria editar(Long id, DatosConvocatoria datos) {
        return guardar(borrador(id), datos);
    }

    @Transactional
    public void eliminar(Long id) {
        convocatorias.delete(borrador(id));
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void publicarPendientes() {
        LocalDateTime ahora = LocalDateTime.now();
        convocatorias.findByEstadoAndFechaPublicacionLessThanEqual(EstadoConvocatoria.BORRADOR, ahora).stream()
                .filter(convocatoria -> !convocatoria.vencidaSinPublicar(ahora))
                .forEach(this::publicar);
    }

    private Convocatoria guardar(Convocatoria convocatoria, DatosConvocatoria datos) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicio = datos.fechaEvento().atTime(datos.hora());
        if (!inicio.isAfter(ahora)) {
            throw new ReglaNegocioException("convocatoria.fechaFutura", "fechaEvento");
        }
        convocatoria.setTitulo(datos.titulo().trim());
        convocatoria.setTipo(datos.tipo());
        convocatoria.setFechaEvento(datos.fechaEvento());
        convocatoria.setHora(datos.hora());
        convocatoria.setLugar(datos.lugar().trim());
        convocatoria.setContenido(datos.contenido().trim());
        convocatoria.setPrioridad(datos.prioridad());
        if (datos.prioridad() == PrioridadConvocatoria.URGENTE) {
            convocatoria.setFechaPublicacion(ahora);
            Convocatoria guardada = convocatorias.save(convocatoria);
            publicar(guardada);
            return guardada;
        }
        LocalDateTime publicacion = datos.fechaPublicacion();
        if (publicacion == null || !publicacion.isAfter(ahora) || !publicacion.isBefore(inicio)) {
            throw new ReglaNegocioException("convocatoria.fechaPublicacion", "fechaPublicacion");
        }
        convocatoria.setFechaPublicacion(publicacion);
        convocatoria.setEstado(EstadoConvocatoria.BORRADOR);
        return convocatorias.save(convocatoria);
    }

    private void publicar(Convocatoria convocatoria) {
        convocatoria.setEstado(EstadoConvocatoria.PUBLICADA);
        convocatorias.save(convocatoria);
        String titulo = mensajes.getMessage("notificacion.convocatoria.titulo", null, ESPANOL);
        String detalle = convocatoria.getTitulo() + " · " + convocatoria.getInicioEvento().format(FECHA) + " · "
                + convocatoria.getLugar();
        socios.findAllByJuntaIdAndEstadoOrderByApellidosAscNombresAsc(convocatoria.getJunta().getId(), EstadoSocio.ACTIVO)
                .stream()
                .filter(Socio::isNotificable)
                .forEach(socio -> notificaciones.crear(socio, titulo, detalle, "/anuncios"));
    }
}
