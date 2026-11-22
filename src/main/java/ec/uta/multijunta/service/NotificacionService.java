package ec.uta.multijunta.service;

import ec.uta.multijunta.model.Notificacion;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.NotificacionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificacionService {

    private final NotificacionRepository notificaciones;
    private final UsuarioActual usuarioActual;

    public NotificacionService(NotificacionRepository notificaciones, UsuarioActual usuarioActual) {
        this.notificaciones = notificaciones;
        this.usuarioActual = usuarioActual;
    }

    @Transactional
    public Notificacion crear(Usuario destinatario, String titulo, String mensaje, String enlace) {
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(destinatario);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        notificacion.setEnlace(enlace);
        return notificaciones.save(notificacion);
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas() {
        return usuarioActual.sesion()
                .map(sesion -> notificaciones.countByUsuarioIdAndLeidaFalse(sesion.getId()))
                .orElse(0L);
    }

    @Transactional(readOnly = true)
    public List<Notificacion> listar() {
        return notificaciones.findByUsuarioIdOrderByLeidaAscFechaDesc(usuarioActual.id());
    }

    @Transactional
    public String abrir(Long id) {
        Notificacion notificacion = notificaciones.findByIdAndUsuarioId(id, usuarioActual.id())
                .orElseThrow(RecursoNoEncontradoException::new);
        notificacion.setLeida(true);
        notificaciones.save(notificacion);
        return notificacion.getEnlace();
    }

    @Transactional
    public void marcarTodas() {
        List<Notificacion> pendientes = notificaciones.findByUsuarioIdAndLeidaFalse(usuarioActual.id());
        pendientes.forEach(notificacion -> notificacion.setLeida(true));
        notificaciones.saveAll(pendientes);
    }
}
