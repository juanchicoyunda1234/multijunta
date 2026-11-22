package ec.uta.multijunta.service;

import ec.uta.multijunta.model.EstadoSolicitud;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.SolicitudRecuperacion;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.SolicitudRecuperacionRepository;
import ec.uta.multijunta.repository.UsuarioRepository;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecuperacionService {

    public static final String ENLACE_SOLICITUDES = "/recuperacion/solicitudes";

    private static final Locale ESPANOL = Locale.forLanguageTag("es");

    private final UsuarioRepository usuarios;
    private final SolicitudRecuperacionRepository solicitudes;
    private final PasswordService passwords;
    private final NotificacionService notificaciones;
    private final UsuarioActual usuarioActual;
    private final MessageSource mensajes;

    public RecuperacionService(UsuarioRepository usuarios, SolicitudRecuperacionRepository solicitudes,
            PasswordService passwords, NotificacionService notificaciones, UsuarioActual usuarioActual,
            MessageSource mensajes) {
        this.usuarios = usuarios;
        this.solicitudes = solicitudes;
        this.passwords = passwords;
        this.notificaciones = notificaciones;
        this.usuarioActual = usuarioActual;
        this.mensajes = mensajes;
    }

    @Transactional
    public void solicitar(String identificador) {
        buscar(identificador)
                .filter(Usuario::isActivo)
                .filter(usuario -> usuario.getRol() != Rol.SUPER_ADMIN && usuario.getJunta() != null)
                .filter(usuario -> !solicitudes.existsByUsuarioIdAndEstado(usuario.getId(), EstadoSolicitud.PENDIENTE))
                .ifPresent(this::registrar);
    }

    @Transactional(readOnly = true)
    public List<SolicitudRecuperacion> listarAtendibles() {
        if (usuarioActual.rol() == Rol.SUPER_ADMIN) {
            return solicitudes.findByEstadoAndUsuarioRolOrderByFechaSolicitudAsc(
                    EstadoSolicitud.PENDIENTE, Rol.ADMIN_JUNTA);
        }
        return solicitudes.findByJuntaIdAndEstadoAndUsuarioRolNotOrderByFechaSolicitudAsc(
                usuarioActual.juntaId(), EstadoSolicitud.PENDIENTE, Rol.ADMIN_JUNTA);
    }

    @Transactional
    public Resultado restablecer(Long solicitudId) {
        SolicitudRecuperacion solicitud = buscarAtendible(solicitudId);
        if (solicitud.getEstado() == EstadoSolicitud.ATENDIDA) {
            throw new ReglaNegocioException("recuperar.atendida");
        }
        String temporal = passwords.generarTemporal();
        Usuario usuario = solicitud.getUsuario();
        usuario.setPassword(passwords.codificar(temporal));
        usuario.setDebeCambiarPassword(true);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarios.save(usuario);
        solicitud.setEstado(EstadoSolicitud.ATENDIDA);
        solicitud.setFechaAtencion(LocalDateTime.now());
        solicitud.setAtendidaPor(usuarioActual.entidad());
        solicitudes.save(solicitud);
        return new Resultado(usuario.getNombreCompleto(), usuario.getUsername(), temporal);
    }

    public record Resultado(String nombre, String username, String temporal) implements Serializable {
    }

    private Optional<Usuario> buscar(String identificador) {
        if (identificador == null || identificador.isBlank()) {
            return Optional.empty();
        }
        Optional<Usuario> porUsuario = usuarios.findByUsername(Usuario.normalizarUsername(identificador));
        return porUsuario.isPresent() ? porUsuario : usuarios.findByCedula(identificador.trim());
    }

    private void registrar(Usuario usuario) {
        SolicitudRecuperacion solicitud = new SolicitudRecuperacion();
        solicitud.setUsuario(usuario);
        solicitud.setJunta(usuario.getJunta());
        solicitudes.save(solicitud);
        List<Usuario> destinatarios = usuario.getRol() == Rol.ADMIN_JUNTA
                ? usuarios.findByRolAndActivoTrue(Rol.SUPER_ADMIN)
                : usuarios.findByJuntaIdAndRolAndActivoTrue(usuario.getJunta().getId(), Rol.ADMIN_JUNTA);
        String mensaje = usuario.getNombreCompleto() + " solicitó restablecer su contraseña.";
        String titulo = mensajes.getMessage("notificacion.recuperacion.titulo", null, ESPANOL);
        destinatarios.forEach(destinatario -> notificaciones.crear(destinatario, titulo, mensaje, ENLACE_SOLICITUDES));
    }

    private SolicitudRecuperacion buscarAtendible(Long solicitudId) {
        if (usuarioActual.rol() == Rol.SUPER_ADMIN) {
            return solicitudes.findById(solicitudId)
                    .filter(solicitud -> solicitud.getUsuario().getRol() == Rol.ADMIN_JUNTA)
                    .orElseThrow(RecursoNoEncontradoException::new);
        }
        return solicitudes.findByIdAndJuntaId(solicitudId, usuarioActual.juntaId())
                .filter(solicitud -> solicitud.getUsuario().getRol() != Rol.ADMIN_JUNTA)
                .orElseThrow(RecursoNoEncontradoException::new);
    }
}
