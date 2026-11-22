package ec.uta.multijunta.rest;

import ec.uta.multijunta.service.PanelService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PanelController {

    private final PanelService panel;

    public PanelController(PanelService panel) {
        this.panel = panel;
    }

    @GetMapping("/panel")
    public String panel(Model modelo) {
        modelo.addAttribute("tarjetas", panel.tarjetas());
        return "inicio/directiva";
    }
}
