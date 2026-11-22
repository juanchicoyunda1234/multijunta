package ec.uta.multijunta.rest;

import ec.uta.multijunta.service.NotificacionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class NotificacionController {

    private final NotificacionService notificaciones;

    public NotificacionController(NotificacionService notificaciones) {
        this.notificaciones = notificaciones;
    }

    @GetMapping("/notificaciones")
    public String lista(Model modelo) {
        modelo.addAttribute("notificaciones", notificaciones.listar());
        return "notificaciones/lista";
    }

    @GetMapping("/notificaciones/{id}")
    public String abrir(@PathVariable Long id) {
        String enlace = notificaciones.abrir(id);
        boolean interno = enlace != null && enlace.startsWith("/") && !enlace.startsWith("//");
        return "redirect:" + (interno ? enlace : "/notificaciones");
    }

    @PostMapping("/notificaciones/marcar-todas")
    public String marcarTodas() {
        notificaciones.marcarTodas();
        return "redirect:/notificaciones";
    }
}
