package ec.uta.multijunta.rest;

import ec.uta.multijunta.rest.formulario.ConfiguracionFormulario;
import ec.uta.multijunta.service.ConfiguracionJuntaService;
import ec.uta.multijunta.service.ReglaNegocioException;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/junta/configuracion")
public class ConfiguracionJuntaController {

    private static final String VISTA = "junta/configuracion";
    private static final Set<String> CAMPOS = Set.of("telefono", "direccion", "montoMultaInasistencia", "logo");
    private static final Map<String, String> PESTANAS = Map.of(
            "direccion", "ubicacion",
            "telefono", "general",
            "montoMultaInasistencia", "general",
            "logo", "general");

    private final ConfiguracionJuntaService configuracion;

    public ConfiguracionJuntaController(ConfiguracionJuntaService configuracion) {
        this.configuracion = configuracion;
    }

    @GetMapping
    public String formulario(Model modelo) {
        modelo.addAttribute("configuracionFormulario", ConfiguracionFormulario.desde(configuracion.junta()));
        return mostrar(modelo, null, null);
    }

    @PostMapping
    public String actualizar(@Valid @ModelAttribute("configuracionFormulario") ConfiguracionFormulario formulario,
            BindingResult resultado, Model modelo, RedirectAttributes redireccion) {
        if (resultado.hasErrors()) {
            FieldError primero = resultado.getFieldError();
            String campo = primero == null ? null : primero.getField();
            return mostrar(modelo, PESTANAS.getOrDefault(campo, "general"), campo);
        }
        try {
            configuracion.actualizar(formulario.datos());
        } catch (ReglaNegocioException excepcion) {
            String campo = excepcion.getCampo();
            if (CAMPOS.contains(campo)) {
                resultado.rejectValue(campo, excepcion.getClave());
                return mostrar(modelo, PESTANAS.get(campo), campo);
            }
            resultado.reject(excepcion.getClave());
            return mostrar(modelo, "sectores", "nuevoSector");
        }
        redireccion.addFlashAttribute("exito", "configuracion.actualizada");
        return "redirect:/junta/configuracion";
    }

    private String mostrar(Model modelo, String pestana, String foco) {
        modelo.addAttribute("junta", configuracion.junta());
        modelo.addAttribute("sectores", configuracion.sectores());
        modelo.addAttribute("pestana", pestana);
        modelo.addAttribute("foco", foco);
        return VISTA;
    }
}
