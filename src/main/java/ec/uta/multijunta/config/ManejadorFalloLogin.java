package ec.uta.multijunta.config;

import ec.uta.multijunta.service.AutenticacionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class ManejadorFalloLogin implements AuthenticationFailureHandler {

    private final AutenticacionService autenticacion;

    public ManejadorFalloLogin(AutenticacionService autenticacion) {
        this.autenticacion = autenticacion;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException excepcion) throws IOException {
        response.sendRedirect(request.getContextPath() + "/login?" + motivo(request, excepcion));
    }

    private String motivo(HttpServletRequest request, AuthenticationException excepcion) {
        if (excepcion instanceof LockedException) {
            return "bloqueada";
        }
        if (excepcion instanceof DisabledException) {
            return "desactivada";
        }
        if (excepcion instanceof JuntaSuspendidaException) {
            return "suspendida";
        }
        if (autenticacion.estaBloqueado(request.getParameter("username"))) {
            return "bloqueada";
        }
        return "error";
    }
}
