package ec.uta.multijunta.rest;

import ec.uta.multijunta.config.CambioPasswordFilter;
import ec.uta.multijunta.service.PasswordService;
import ec.uta.multijunta.service.ReglaNegocioException;
import ec.uta.multijunta.service.UsuarioActual;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PasswordController {

    private final PasswordService passwords;
    private final UsuarioActual usuarioActual;

    public PasswordController(PasswordService passwords, UsuarioActual usuarioActual) {
        this.passwords = passwords;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping(CambioPasswordFilter.RUTA_CAMBIO)
    public String formulario(Model modelo) {
        modelo.addAttribute("obligado", usuarioActual.entidad().isDebeCambiarPassword());
        return "cuenta/cambiar-password";
    }

    @PostMapping(CambioPasswordFilter.RUTA_CAMBIO)
    public String cambiar(@RequestParam(defaultValue = "") String actual,
            @RequestParam(defaultValue = "") String nueva,
            @RequestParam(defaultValue = "") String confirmacion,
            Model modelo, RedirectAttributes redireccion) {
        try {
            passwords.cambiar(usuarioActual.id(), actual, nueva, confirmacion);
        } catch (ReglaNegocioException excepcion) {
            modelo.addAttribute("error", excepcion.getClave());
            modelo.addAttribute("obligado", usuarioActual.entidad().isDebeCambiarPassword());
            return "cuenta/cambiar-password";
        }
        redireccion.addFlashAttribute("exito", "password.actualizada");
        return "redirect:" + usuarioActual.rutaInicio();
    }
}
