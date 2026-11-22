package ec.uta.multijunta.config;

import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class EstadoCuentaFilter extends OncePerRequestFilter {

    private final UsuarioRepository usuarios;

    public EstadoCuentaFilter(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioAutenticado sesion) {
            String motivo = motivoDeCierre(usuarios.findById(sesion.getId()));
            if (motivo != null) {
                HttpSession actual = request.getSession(false);
                if (actual != null) {
                    actual.invalidate();
                }
                SecurityContextHolder.clearContext();
                request.getSession(true);
                response.sendRedirect(request.getContextPath() + "/login?" + motivo);
                return;
            }
        }
        cadena.doFilter(request, response);
    }

    private String motivoDeCierre(Optional<Usuario> usuario) {
        if (usuario.isEmpty() || !usuario.get().isActivo()) {
            return "desactivada";
        }
        if (usuario.get().getJunta() != null && !usuario.get().getJunta().isActiva()) {
            return "suspendida";
        }
        return null;
    }
}
