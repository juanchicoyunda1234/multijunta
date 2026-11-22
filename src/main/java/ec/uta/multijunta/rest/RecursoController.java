package ec.uta.multijunta.rest;

import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.TipoRecurso;
import ec.uta.multijunta.rest.formulario.RecursoFormulario;
import ec.uta.multijunta.service.RecursoService;
import ec.uta.multijunta.service.ReglaNegocioException;
import ec.uta.multijunta.service.UsuarioActual;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/recursos")
public class RecursoController {

    private static final String LISTA = "redirect:/recursos";

    private final RecursoService recursos;
    private final UsuarioActual usuarioActual;

    public RecursoController(RecursoService recursos, UsuarioActual usuarioActual) {
        this.recursos = recursos;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public String lista(Model modelo) {
        return mostrar(modelo, null, null);
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("recursoFormulario") RecursoFormulario formulario,
            BindingResult resultado, Model modelo, RedirectAttributes redireccion) {
        if (!resultado.hasErrors()) {
            try {
                recursos.crear(formulario.datos());
                redireccion.addFlashAttribute("exito", "recurso.guardado");
                return LISTA;
            } catch (ReglaNegocioException excepcion) {
                resultado.rejectValue(excepcion.getCampo(), excepcion.getClave());
            }
        }
        return mostrar(modelo, "nuevo", null);
    }

    @PostMapping("/{id}")
    public String editar(@PathVariable Long id,
            @Valid @ModelAttribute("edicionFormulario") RecursoFormulario formulario,
            BindingResult resultado, Model modelo, RedirectAttributes redireccion) {
        recursos.obtener(id);
        if (!resultado.hasErrors()) {
            try {
                recursos.editar(id, formulario.datos());
                redireccion.addFlashAttribute("exito", "recurso.guardado");
                return LISTA;
            } catch (ReglaNegocioException excepcion) {
                resultado.rejectValue(excepcion.getCampo(), excepcion.getClave());
            }
        }
        return mostrar(modelo, "edicion", id);
    }

    @PostMapping("/{id}/desactivar")
    public String desactivar(@PathVariable Long id, RedirectAttributes redireccion) {
        recursos.cambiarEstado(id, false);
        redireccion.addFlashAttribute("exito", "recurso.desactivado");
        return LISTA;
    }

    @PostMapping("/{id}/reactivar")
    public String reactivar(@PathVariable Long id, RedirectAttributes redireccion) {
        recursos.cambiarEstado(id, true);
        redireccion.addFlashAttribute("exito", "recurso.reactivado");
        return LISTA;
    }

    private String mostrar(Model modelo, String abrir, Long edicionId) {
        modelo.addAttribute("recursos", recursos.listar());
        modelo.addAttribute("sectores", recursos.sectores());
        modelo.addAttribute("tipos", TipoRecurso.values());
        modelo.addAttribute("puedeEditar", usuarioActual.rol() == Rol.SECRETARIO);
        modelo.addAttribute("abrir", abrir);
        modelo.addAttribute("edicionId", edicionId);
        if (!modelo.containsAttribute("recursoFormulario")) {
            modelo.addAttribute("recursoFormulario", new RecursoFormulario());
        }
        if (!modelo.containsAttribute("edicionFormulario")) {
            modelo.addAttribute("edicionFormulario", new RecursoFormulario());
        }
        return "recursos/lista";
    }
}
