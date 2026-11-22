package ec.uta.multijunta.service;

import ec.uta.multijunta.config.UsuarioAutenticado;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.UsuarioRepository;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioActual {

    private final UsuarioRepository usuarios;

    public UsuarioActual(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public Optional<UsuarioAutenticado> sesion() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioAutenticado sesion) {
            return Optional.of(sesion);
        }
        return Optional.empty();
    }

    public UsuarioAutenticado requerida() {
        return sesion().orElseThrow(() -> new IllegalStateException("No hay sesión iniciada"));
    }

    public Long id() {
        return requerida().getId();
    }

    public Rol rol() {
        return requerida().getRol();
    }

    public Long juntaId() {
        return requerida().getJuntaId();
    }

    public String rutaInicio() {
        return switch (rol()) {
            case SUPER_ADMIN -> "/plataforma";
            case SOCIO -> "/mi-informacion";
            default -> "/panel";
        };
    }

    public Usuario entidad() {
        return usuarios.findById(id()).orElseThrow(RecursoNoEncontradoException::new);
    }
}
