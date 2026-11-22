package ec.uta.multijunta.rest;

import ec.uta.multijunta.service.RecuperacionService;
import ec.uta.multijunta.service.ReglaNegocioException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RecuperacionController {

    private final RecuperacionService recuperacion;

    public RecuperacionController(RecuperacionService recuperacion) {
        this.recuperacion = recuperacion;
    }

    @GetMapping("/recuperar")
    public String formulario() {
        return "auth/recuperar";
    }

    @PostMapping("/recuperar")
    public String solicitar(@RequestParam(defaultValue = "") String identificador) {
        recuperacion.solicitar(identificador);
        return "redirect:/recuperar?enviada";
    }

    @GetMapping(RecuperacionService.ENLACE_SOLICITUDES)
    public String solicitudes(Model modelo) {
        modelo.addAttribute("solicitudes", recuperacion.listarAtendibles());
        return "recuperacion/solicitudes";
    }

    @PostMapping(RecuperacionService.ENLACE_SOLICITUDES + "/{id}/restablecer")
    public String restablecer(@PathVariable Long id, RedirectAttributes redireccion) {
        try {
            redireccion.addFlashAttribute("restablecida", recuperacion.restablecer(id));
        } catch (ReglaNegocioException excepcion) {
            redireccion.addFlashAttribute("error", excepcion.getClave());
        }
        return "redirect:" + RecuperacionService.ENLACE_SOLICITUDES;
    }
}
