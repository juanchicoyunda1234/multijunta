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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

public class EstadoCuentaFilter extends OncePerRequestFilter {

    private final UsuarioRepository usuarios;
    private final SecurityContextRepository repositorio = new HttpSessionSecurityContextRepository();

    public EstadoCuentaFilter(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioAutenticado sesion) {
            Optional<Usuario> usuario = usuarios.findById(sesion.getId());
            String motivo = motivoDeCierre(usuario);
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
            if (usuario.get().getRol() != sesion.getRol()) {
                actualizarRol(usuario.get(), request, response);
            }
        }
        cadena.doFilter(request, response);
    }

    private void actualizarRol(Usuario usuario, HttpServletRequest request, HttpServletResponse response) {
        UsuarioAutenticado nueva = UsuarioAutenticado.desde(usuario);
        SecurityContext contexto = SecurityContextHolder.getContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(nueva, null, nueva.getAuthorities()));
        if (request.getSession(false) != null) {
            repositorio.saveContext(contexto, request, response);
        }
    }

    private String motivoDeCierre(Optional<Usuario> usuario) {
        if (usuario.isEmpty() || !usuario.get().isPuedeIngresar()) {
            return "desactivada";
        }
        if (usuario.get().getJunta() != null && !usuario.get().getJunta().isActiva()) {
            return "suspendida";
        }
        return null;
    }
}
