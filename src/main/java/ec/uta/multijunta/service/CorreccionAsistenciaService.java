package ec.uta.multijunta.service;

import ec.uta.multijunta.model.Asistencia;
import ec.uta.multijunta.model.CorreccionAsistencia;
import ec.uta.multijunta.model.EstadoAsistencia;
import ec.uta.multijunta.model.EstadoCorreccion;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.AsistenciaRepository;
import ec.uta.multijunta.repository.CorreccionAsistenciaRepository;
import ec.uta.multijunta.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CorreccionAsistenciaService {

    public static final String ENLACE = "/asistencia/correcciones";

    private static final Locale ESPANOL = Locale.forLanguageTag("es");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CorreccionAsistenciaRepository correcciones;
    private final AsistenciaRepository asistencias;
    private final UsuarioRepository usuarios;
    private final NotificacionService notificaciones;
    private final MessageSource mensajes;
    private final UsuarioActual usuarioActual;

    public CorreccionAsistenciaService(CorreccionAsistenciaRepository correcciones, AsistenciaRepository asistencias,
            UsuarioRepository usuarios, NotificacionService notificaciones, MessageSource mensajes,
            UsuarioActual usuarioActual) {
        this.correcciones = correcciones;
        this.asistencias = asistencias;
        this.usuarios = usuarios;
        this.notificaciones = notificaciones;
        this.mensajes = mensajes;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public List<CorreccionAsistencia> listar() {
        return correcciones.findByJuntaIdOrderByFechaSolicitudDesc(usuarioActual.juntaId());
    }

    @Transactional(readOnly = true)
    public List<CorreccionAsistencia> delEvento(Long eventoId) {
        return correcciones.findByAsistenciaEventoIdOrderByFechaSolicitudDesc(eventoId).stream()
                .filter(correccion -> correccion.getJunta().getId().equals(usuarioActual.juntaId()))
                .toList();
    }

    @Transactional
    public CorreccionAsistencia solicitar(Long asistenciaId, EstadoAsistencia estadoNuevo, String motivo) {
        exigirRol(Rol.SECRETARIO);
        Asistencia asistencia = asistencias.findByIdAndEventoJuntaId(asistenciaId, usuarioActual.juntaId())
                .orElseThrow(RecursoNoEncontradoException::new);
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException("correccion.motivo", "motivo");
        }
        if (estadoNuevo == null || estadoNuevo == asistencia.getEstado()) {
            throw new ReglaNegocioException("correccion.mismoEstado", "estadoNuevo");
        }
        if (correcciones.existsByAsistenciaIdAndEstado(asistenciaId, EstadoCorreccion.PENDIENTE)) {
            throw new ReglaNegocioException("correccion.pendiente");
        }
        CorreccionAsistencia correccion = new CorreccionAsistencia();
        correccion.setJunta(asistencia.getEvento().getJunta());
        correccion.setAsistencia(asistencia);
        correccion.setEstadoAnterior(asistencia.getEstado());
        correccion.setEstadoNuevo(estadoNuevo);
        correccion.setMotivo(motivo.trim().length() > 300 ? motivo.trim().substring(0, 300) : motivo.trim());
        correccion.setSolicitante(usuarioActual.entidad());
        CorreccionAsistencia guardada = correcciones.save(correccion);
        String titulo = mensajes.getMessage("notificacion.correccion.titulo", null, ESPANOL);
        usuarios.findByJuntaIdAndRolAndActivoTrue(usuarioActual.juntaId(), Rol.ADMIN_JUNTA)
                .forEach(admin -> notificaciones.crear(admin, titulo, descripcion(guardada), ENLACE));
        return guardada;
    }

    @Transactional
    public CorreccionAsistencia aprobar(Long id) {
        return resolver(id, true);
    }

    @Transactional
    public CorreccionAsistencia rechazar(Long id) {
        return resolver(id, false);
    }

    private CorreccionAsistencia resolver(Long id, boolean aprobar) {
        exigirRol(Rol.ADMIN_JUNTA);
        CorreccionAsistencia correccion = correcciones.findByIdAndJuntaId(id, usuarioActual.juntaId())
                .orElseThrow(RecursoNoEncontradoException::new);
        if (correccion.getEstado() != EstadoCorreccion.PENDIENTE) {
            throw new ReglaNegocioException("correccion.yaResuelta");
        }
        if (aprobar) {
            Asistencia asistencia = correccion.getAsistencia();
            asistencia.setEstado(correccion.getEstadoNuevo());
            asistencias.save(asistencia);
        }
        correccion.setEstado(aprobar ? EstadoCorreccion.APROBADA : EstadoCorreccion.RECHAZADA);
        correccion.setFechaResolucion(LocalDateTime.now());
        correccion.setResolutor(usuarioActual.entidad());
        CorreccionAsistencia guardada = correcciones.save(correccion);
        String clave = aprobar ? "notificacion.correccionAprobada.titulo" : "notificacion.correccionRechazada.titulo";
        Usuario solicitante = guardada.getSolicitante();
        notificaciones.crear(solicitante, mensajes.getMessage(clave, null, ESPANOL), descripcion(guardada), ENLACE);
        return guardada;
    }

    private String descripcion(CorreccionAsistencia correccion) {
        Asistencia asistencia = correccion.getAsistencia();
        return asistencia.getSocio().getNombreCompleto() + " · " + asistencia.getEvento().getTipo().getEtiqueta()
                + " del " + asistencia.getEvento().getFecha().format(FECHA) + ": "
                + correccion.getEstadoAnterior().getEtiqueta() + " → " + correccion.getEstadoNuevo().getEtiqueta();
    }

    private void exigirRol(Rol rol) {
        if (usuarioActual.rol() != rol) {
            throw new AccessDeniedException("Operación no permitida para el rol");
        }
    }
}
