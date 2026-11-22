package ec.uta.multijunta.rest;

import ec.uta.multijunta.service.UsuarioActual;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InicioController {

    private final UsuarioActual usuarioActual;

    public InicioController(UsuarioActual usuarioActual) {
        this.usuarioActual = usuarioActual;
    }

    @GetMapping({"/", "/inicio"})
    public String inicio() {
        return "redirect:" + usuarioActual.rutaInicio();
    }

    @GetMapping("/plataforma")
    public String plataforma() {
        return "redirect:/plataforma/juntas";
    }

    @GetMapping("/mi-informacion")
    public String miInformacion() {
        return "inicio/socio";
    }
}
