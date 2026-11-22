package ec.uta.multijunta.rest;

import ec.uta.multijunta.service.UsuarioActual;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    private final UsuarioActual usuarioActual;

    public LoginController(UsuarioActual usuarioActual) {
        this.usuarioActual = usuarioActual;
    }

    @GetMapping("/login")
    public String login() {
        return usuarioActual.sesion().isPresent() ? "redirect:/inicio" : "auth/login";
    }
}
