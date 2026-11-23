package ec.uta.multijunta.rest;

import ec.uta.multijunta.service.MensajeService;
import ec.uta.multijunta.service.NotificacionService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContadorController {

    private final NotificacionService notificaciones;
    private final MensajeService mensajes;

    public ContadorController(NotificacionService notificaciones, MensajeService mensajes) {
        this.notificaciones = notificaciones;
        this.mensajes = mensajes;
    }

    @GetMapping("/contadores")
    public Map<String, Long> contadores() {
        return Map.of("notificaciones", notificaciones.contarNoLeidas(), "mensajes", mensajes.noLeidos());
    }
}
