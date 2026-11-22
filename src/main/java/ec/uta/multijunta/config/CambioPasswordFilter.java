package ec.uta.multijunta.config;

import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class CambioPasswordFilter extends OncePerRequestFilter {

    public static final String RUTA_CAMBIO = "/cuenta/cambiar-password";

    private static final List<String> PERMITIDAS = List.of(RUTA_CAMBIO, "/logout", "/login", "/error");

    private final UsuarioRepository usuarios;

    public CambioPasswordFilter(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioAutenticado sesion
                && !permitida(ruta)
                && usuarios.findById(sesion.getId()).map(Usuario::isDebeCambiarPassword).orElse(false)) {
            response.sendRedirect(request.getContextPath() + RUTA_CAMBIO);
            return;
        }
        cadena.doFilter(request, response);
    }

    private boolean permitida(String ruta) {
        return PERMITIDAS.contains(ruta) || ruta.startsWith("/css/");
    }
}
