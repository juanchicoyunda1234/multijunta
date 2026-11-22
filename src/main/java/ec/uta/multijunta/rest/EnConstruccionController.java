package ec.uta.multijunta.rest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class EnConstruccionController {

    @GetMapping("/en-construccion")
    public String enConstruccion() {
        return "en-construccion";
    }

    @GetMapping({"/pagos", "/multas",
            "/egresos", "/reportes", "/inventario", "/activos", "/cuotas", "/convocatorias", "/actas",
            "/comunicaciones", "/anuncios", "/mensajes"})
    public String moduloPendiente() {
        return "redirect:/en-construccion";
    }
}
